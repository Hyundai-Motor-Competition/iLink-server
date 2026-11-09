package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.RevokeCallCommand;
import com.example.pickupservice.pickup.application.port.in.RevokeCallUseCase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 두 트랜잭션의 순서를 고정한다.
 * <ol>
 *   <li>B: 한 트랜잭션 안에서 호출을 철회한다(세션 행 잠금을 잡은 채 커밋하지 않고 멈춘다).</li>
 *   <li>A: 대상 동작을 시작한다. 첫 loadCall은 아직 커밋 전이라 철회 전 값을 읽고, 세션 loadForUpdate에서 B를 기다린다.</li>
 *   <li>A가 B 때문에 기다리는 것을 pg_blocking_pids로 확인한 뒤 B를 커밋한다.</li>
 *   <li>A가 잠금을 얻어 끝날 때까지 기다리고, A가 던진 예외(없으면 null)를 돌려준다.</li>
 * </ol>
 */
final class RevokeCommitRace {

	private static final long MAX_WAIT_SECONDS = 15;
	private static final long POLL_MILLIS = 20;

	private RevokeCommitRace() {
	}

	static Throwable runWhileRevokeCommits(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate,
			RevokeCallUseCase revokeCallUseCase, RevokeCallCommand revoke, Runnable waiter) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch commitRevoke = new CountDownLatch(1);
		CompletableFuture<Integer> revokerPid = new CompletableFuture<>();
		try {
			CompletableFuture<Void> revoker = CompletableFuture.runAsync(
					() -> transactionTemplate.executeWithoutResult(tx -> {
						revokeCallUseCase.revoke(revoke);
						revokerPid.complete(jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class));
						awaitRelease(commitRevoke);
					}), executor);
			revoker.whenComplete((ignored, error) -> {
				if (error != null) {
					revokerPid.completeExceptionally(error);
				}
			});
			int pid = revokerPid.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);

			CompletableFuture<Void> waiting = CompletableFuture.runAsync(waiter, executor);
			awaitBlockedBy(jdbcTemplate, pid, waiting);

			commitRevoke.countDown();
			revoker.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
			try {
				waiting.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
				return null;
			} catch (ExecutionException e) {
				return e.getCause();
			}
		} finally {
			commitRevoke.countDown();
			executor.shutdown();
			executor.awaitTermination(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
		}
	}

	// A가 첫 loadCall을 마치고 세션 행 잠금에서 B를 기다리는 상태가 될 때까지 기다린다
	private static void awaitBlockedBy(JdbcTemplate jdbcTemplate, int pid, CompletableFuture<Void> waiting)
			throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(MAX_WAIT_SECONDS);
		while (System.nanoTime() < deadline) {
			if (waiting.isDone()) {
				throw new IllegalStateException("철회 트랜잭션이 커밋되기 전에 대상 동작이 끝났다(세션 잠금을 기다리지 않음)",
						waiting.handle((ignored, error) -> error).join());
			}
			Integer blocked = jdbcTemplate.queryForObject(
					"select count(*) from pg_stat_activity where ? = any(pg_blocking_pids(pid))", Integer.class, pid);
			if (blocked != null && blocked > 0) {
				return;
			}
			Thread.sleep(POLL_MILLIS);
		}
		throw new IllegalStateException("대상 동작이 " + MAX_WAIT_SECONDS + "초 안에 세션 잠금 대기에 들어가지 않았다");
	}

	private static void awaitRelease(CountDownLatch release) {
		try {
			if (!release.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("커밋하라는 신호가 " + MAX_WAIT_SECONDS + "초 안에 오지 않았다");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

}
