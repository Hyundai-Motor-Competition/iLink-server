package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.adapter.out.persistence.VehicleLinkCodeEntity;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.fixture.AuthSeed;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.user.adapter.out.persistence.VehicleEntity;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.fixture.UserSeed;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RequiredArgsConstructor
@Import({UserSeed.class, AuthSeed.class})
class VehicleLinkCodeControllerConcurrencyTest extends IntegrationTestSupport {

	private static final String APPROVE_PATH = "/v1/vehicle-links";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final long BARRIER_TIMEOUT_SECONDS = 5;
	private static final long REQUEST_TIMEOUT_SECONDS = 60;
	private static final String CONSENTS_JSON =
			"[{\"type\": \"TERMS\", \"agreed\": true}, {\"type\": \"PRIVACY\", \"agreed\": true}]";

	private final MockMvc mockMvc;
	private final AuthSeed authSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	@MockitoSpyBean
	private LoadVehiclePort loadVehiclePort;

	private AuthSeed.Result seeded;
	private long pendingVehicleId;
	private String codeA;
	private String codeB;
	private String nameA;
	private String nameB;

	@BeforeEach
	void setUp() {
		seeded = authSeed.seed();
		pendingVehicleId = seeded.unlinkedVehicleId();
		codeA = randomCode();
		codeB = randomCode();
		String suffix = String.format("%04d", RANDOM.nextInt(10_000));
		nameA = "동시보호" + suffix + "A";
		nameB = "동시보호" + suffix + "B";
		Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.persist(new VehicleLinkCodeEntity(null, pendingVehicleId, seeded.unlinkedDeviceId(), true,
					codeA, LinkCodeStatus.ISSUED, expiresAt, null, null, null));
			entityManager.persist(new VehicleLinkCodeEntity(null, pendingVehicleId, null, true,
					codeB, LinkCodeStatus.ISSUED, expiresAt, null, null, null));
		});
	}

	@AfterEach
	void tearDown() {
		transactionTemplate.executeWithoutResult(status -> {
			List<Object[]> created = entityManager.createQuery(
							"select u.userId, u.familyId from UserEntity u where u.name in :names", Object[].class)
					.setParameter("names", List.of(nameA, nameB))
					.getResultList();
			List<Long> userIds = created.stream().map(row -> (Long) row[0]).toList();
			List<Long> familyIds = created.stream().map(row -> (Long) row[1]).distinct().toList();
			entityManager.createQuery("delete from VehicleLinkCodeEntity c where c.vehicleId = :vehicleId")
					.setParameter("vehicleId", pendingVehicleId)
					.executeUpdate();
			entityManager.createQuery("update VehicleEntity v set v.familyId = null where v.vehicleId = :vehicleId")
					.setParameter("vehicleId", pendingVehicleId)
					.executeUpdate();
			if (!userIds.isEmpty()) {
				entityManager.createQuery(
								"delete from DeviceEntity d where d.ownerType = :ownerType and d.ownerId in :ownerIds")
						.setParameter("ownerType", OwnerType.USER)
						.setParameter("ownerIds", userIds)
						.executeUpdate();
				entityManager.createQuery("delete from UserEntity u where u.userId in :userIds")
						.setParameter("userIds", userIds)
						.executeUpdate();
			}
			if (!familyIds.isEmpty()) {
				entityManager.createQuery("delete from FamilyEntity f where f.familyId in :familyIds")
						.setParameter("familyIds", familyIds)
						.executeUpdate();
			}
		});
		authSeed.clear(seeded);
	}

	@Test
	@DisplayName("같은 연결 대기 차량에 서로 다른 코드로 NEW 승인이 동시에 들어오면 하나만 201이고 나머지는 409 VEHICLE_ALREADY_LINKED이며 가족은 1개만 남는다")
	void concurrentNewApprovalsOnSamePendingVehicleLinkOnlyOneFamily() throws Exception {
		long familiesBefore = countFamilies();
		CyclicBarrier bothRead = new CyclicBarrier(2);
		doAnswer(invocation -> {
			Object loaded = invocation.callRealMethod();
			if ((long) invocation.getArgument(0) == pendingVehicleId) {
				awaitQuietly(bothRead);
			}
			return loaded;
		}).when(loadVehiclePort).loadVehicle(anyLong());

		List<MvcResult> results = approveConcurrently(
				() -> approve(codeA, nameA),
				() -> approve(codeB, nameB));

		List<Integer> statuses = results.stream().map(result -> result.getResponse().getStatus()).toList();
		assertThat(statuses).as("응답 상태").containsExactlyInAnyOrder(201, 409);

		MvcResult success = results.stream().filter(result -> result.getResponse().getStatus() == 201)
				.findFirst().orElseThrow();
		MvcResult conflict = results.stream().filter(result -> result.getResponse().getStatus() == 409)
				.findFirst().orElseThrow();
		String conflictCode = JsonPath.read(conflict.getResponse().getContentAsString(), "$.error.code");
		assertThat(conflictCode).as("충돌 응답 code").isEqualTo("VEHICLE_ALREADY_LINKED");
		String conflictMessage = JsonPath.read(conflict.getResponse().getContentAsString(), "$.error.message");
		assertThat(conflictMessage).as("충돌 응답 message").isEqualTo("이미 보호자가 연결된 차량입니다");

		long linkedFamilyId = ((Number) JsonPath.read(success.getResponse().getContentAsString(), "$.familyId"))
				.longValue();
		assertThat(vehicleFamilyId()).as("차량에 연결된 가족은 성공한 승인의 가족").isEqualTo(linkedFamilyId);
		assertThat(countFamilies() - familiesBefore).as("새로 생긴 가족 수").isEqualTo(1L);
		assertThat(guardianFamilyIds()).as("새로 생긴 GUARDIAN의 가족").containsExactly(linkedFamilyId);
	}

	private MvcResult approve(String code, String name) throws Exception {
		String body = "{\"code\": \"" + code + "\", \"mode\": \"NEW\", \"consents\": " + CONSENTS_JSON
				+ ", \"platform\": \"ANDROID\", \"pushToken\": \"fcm-" + code + "\", \"name\": \"" + name + "\"}";
		return mockMvc.perform(post(APPROVE_PATH)
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andReturn();
	}

	@SafeVarargs
	private static List<MvcResult> approveConcurrently(Callable<MvcResult>... requests) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(requests.length);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<MvcResult>> futures = Arrays.stream(requests)
					.map(request -> executor.submit(() -> {
						start.await();
						return request.call();
					}))
					.toList();
			start.countDown();
			List<MvcResult> results = new ArrayList<>();
			for (Future<MvcResult> future : futures) {
				results.add(future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));
			}
			return results;
		} finally {
			executor.shutdownNow();
		}
	}

	private static void awaitQuietly(CyclicBarrier barrier) {
		try {
			barrier.await(BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (BrokenBarrierException | TimeoutException ignored) {
			// 한쪽이 먼저 실패하면 기다리지 않고 진행한다
		}
	}

	private long countFamilies() {
		return transactionTemplate.execute(status -> entityManager
				.createQuery("select count(f) from FamilyEntity f", Long.class)
				.getSingleResult());
	}

	private Long vehicleFamilyId() {
		return transactionTemplate.execute(status -> {
			entityManager.clear();
			return entityManager.find(VehicleEntity.class, pendingVehicleId).getFamilyId();
		});
	}

	private List<Long> guardianFamilyIds() {
		return transactionTemplate.execute(status -> entityManager.createQuery(
						"select u.familyId from UserEntity u where u.name in :names", Long.class)
				.setParameter("names", List.of(nameA, nameB))
				.getResultList());
	}

	private static String randomCode() {
		StringBuilder builder = new StringBuilder(8);
		for (int i = 0; i < 8; i++) {
			builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
		}
		return builder.toString();
	}

}
