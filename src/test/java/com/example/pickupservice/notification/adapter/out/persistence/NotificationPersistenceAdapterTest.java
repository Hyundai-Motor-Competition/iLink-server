package com.example.pickupservice.notification.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.domain.Notification;
import com.example.pickupservice.notification.fixture.NotificationSeed;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class, NotificationSeed.class})
class NotificationPersistenceAdapterTest extends IntegrationTestSupport {

	private static final Instant T0 = Instant.parse("2026-11-09T07:00:00Z");
	private static final String HELP_TYPE = "HELP";

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final NotificationSeed notificationSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private NotificationSeed.Result seeded;
	/** 시드가 만든 세션과 다른 세션 번호. 다른 세션의 행이 섞이지 않는지 볼 때 쓴다. */
	private long otherSessionId;
	/** 이 테스트가 직접 넣은 행. 끝나고 지운다. */
	private final List<Long> persistedIds = new ArrayList<>();

	@BeforeEach
	void seed() {
		seeded = notificationSeed.seed();
		otherSessionId = seeded.sessionId() + 1_000_000L;
	}

	@AfterEach
	void clear() {
		if (!persistedIds.isEmpty()) {
			transactionTemplate.executeWithoutResult(status -> entityManager
					.createQuery("delete from NotificationEntity n where n.notificationId in :ids")
					.setParameter("ids", persistedIds)
					.executeUpdate());
		}
		persistedIds.clear();
		notificationSeed.clear(seeded);
	}

	@Test
	@DisplayName("loadBySession은 그 세션의 알림만 모든 값과 함께 발송 순서(notificationId 오름차순)대로 돌려주고, 다른 세션의 알림과 알림이 없는 세션은 섞이지 않는다")
	void loadBySessionReturnsOnlyThatSessionsNotificationsInSendOrder() {
		long secondId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(), T0.plusSeconds(10),
				T0.plusSeconds(12), null, T0);
		long thirdId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId() + 1, T0.plusSeconds(20), null,
				T0.plusSeconds(25), T0);
		long unsentId = persist(seeded.sessionId(), null, "DEPARTURE", seeded.childDeviceId(), null, null, null, T0);
		persist(otherSessionId, null, HELP_TYPE, seeded.childDeviceId(), T0.plusSeconds(5), null, null, T0);
		// 첫 행을 나중에 고쳐도 물리 저장 순서가 아니라 notificationId 순서로 돌려줘야 한다.
		jdbcTemplate.update("update notification set sent_at = ? where notification_id = ?",
				T0.plusSeconds(1).atOffset(ZoneOffset.UTC), seeded.notificationId());

		List<Notification> loaded = loadNotificationPort.loadBySession(seeded.sessionId());

		assertThat(loaded).extracting(Notification::getNotificationId)
				.as("그 세션의 알림만, notificationId 오름차순")
				.containsExactly(seeded.notificationId(), secondId, thirdId, unsentId);
		assertThat(loaded.get(1)).usingRecursiveComparison().ignoringFields("createdAt")
				.as("모든 값(callId · type · deviceId · sentAt · deliveredAt · openedAt)")
				.isEqualTo(Notification.restore(secondId, seeded.sessionId(), seeded.callId(), "CALL",
						seeded.childDeviceId(), T0.plusSeconds(10), T0.plusSeconds(12), null));
		assertThat(loaded.get(2)).usingRecursiveComparison().ignoringFields("createdAt")
				.as("callId가 없는 행의 모든 값")
				.isEqualTo(Notification.restore(thirdId, seeded.sessionId(), null, HELP_TYPE,
						seeded.childDeviceId() + 1, T0.plusSeconds(20), null, T0.plusSeconds(25)));
		assertThat(loaded.get(3).getSentAt()).as("아직 보내지 않은 행의 sentAt").isNull();
		assertThat(loadNotificationPort.loadBySession(otherSessionId + 1)).as("알림이 없는 세션").isEmpty();
	}

	@Test
	@DisplayName("loadResendTargets는 sentAt이나 deliveredAt이 빈 행 중 마지막 발송(sentAt, 없으면 createdAt) 뒤 1분이 지난 행을 세션에 상관없이 돌려주고, 둘 다 찬 행과 1분이 안 된 행은 돌려주지 않는다")
	void loadResendTargetsReturnsUnconfirmedRowsLastSentAtLeastOneMinuteAgo() {
		Instant now = T0.plusSeconds(600);
		// 시드 행은 보냄 · 수신 모두 채워 대상에서 뺀다. CALL은 수신 뒤에도 재발송 대상이라 type을 HELP로 바꿔 둔다.
		jdbcTemplate.update("update notification set type = ?, sent_at = ?, delivered_at = ? where notification_id = ?",
				HELP_TYPE, now.minusSeconds(300).atOffset(ZoneOffset.UTC), now.minusSeconds(290).atOffset(ZoneOffset.UTC),
				seeded.notificationId());
		long exactlyOneMinuteId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(),
				now.minusSeconds(60), null, null, now.minusSeconds(61));
		long fiftyNineSecondsId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId() + 1,
				now.minusSeconds(59), null, null, now.minusSeconds(60));
		long oldUndeliveredId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(),
				now.minusSeconds(300), null, null, now.minusSeconds(301));
		long neverSentOldId = persist(seeded.sessionId(), null, "DEPARTURE", seeded.childDeviceId(), null, null, null,
				now.minusSeconds(120));
		long neverSentJustCreatedId = persist(seeded.sessionId(), null, "DEPARTURE", seeded.childDeviceId() + 1, null,
				null, null, now.minusSeconds(30));
		long deliveredOnlyOldId = persist(seeded.sessionId(), null, "PREP_EXTENDED", seeded.childDeviceId(), null,
				now.minusSeconds(250), null, now.minusSeconds(300));
		long fullyDoneId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId() + 1,
				now.minusSeconds(300), now.minusSeconds(295), null, now.minusSeconds(301));
		long otherSessionTargetId = persist(otherSessionId, null, HELP_TYPE, seeded.childDeviceId(),
				now.minusSeconds(400), null, null, now.minusSeconds(401));

		List<Notification> targets = loadNotificationPort.loadResendTargets(now);

		assertThat(targets).extracting(Notification::getNotificationId)
				.as("sentAt이나 deliveredAt이 비고 마지막 발송 뒤 60초 이상 지난 행 (정확히 60초는 포함, 59초 · 방금 만든 행 · 둘 다 찬 행은 제외)")
				.containsExactlyInAnyOrder(exactlyOneMinuteId, oldUndeliveredId, neverSentOldId, deliveredOnlyOldId,
						otherSessionTargetId)
				.doesNotContain(seeded.notificationId(), fiftyNineSecondsId, neverSentJustCreatedId, fullyDoneId);
		assertThat(targets).filteredOn(n -> n.getNotificationId() == oldUndeliveredId).singleElement()
				.usingRecursiveComparison().ignoringFields("createdAt")
				.as("돌려준 행의 모든 값")
				.isEqualTo(Notification.restore(oldUndeliveredId, seeded.sessionId(), null, HELP_TYPE,
						seeded.childDeviceId(), now.minusSeconds(300), null, null));
	}

	@Test
	@DisplayName("loadResendTargets는 CALL 알림이면 sentAt · deliveredAt이 모두 차 있어도 마지막 발송(sentAt) 뒤 60초 이상 지난 행을 돌려주고, CALL이 아닌 행은 둘 다 차면 오래돼도 돌려주지 않는다")
	void loadResendTargetsReturnsFullyDeliveredCallRowsLastSentAtLeastOneMinuteAgo() {
		Instant now = T0.plusSeconds(600);
		// 시드 행(CALL)은 방금 보내고 받은 것으로 맞춰 1분이 안 된 행으로 둔다.
		jdbcTemplate.update("update notification set sent_at = ?, delivered_at = ? where notification_id = ?",
				now.minusSeconds(10).atOffset(ZoneOffset.UTC), now.minusSeconds(5).atOffset(ZoneOffset.UTC),
				seeded.notificationId());
		long callDoneOldId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(),
				now.minusSeconds(300), now.minusSeconds(295), null, now.minusSeconds(301));
		long callDoneExactlyOneMinuteId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId() + 1,
				now.minusSeconds(60), now.minusSeconds(55), null, now.minusSeconds(61));
		long callDoneFiftyNineSecondsId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId() + 2,
				now.minusSeconds(59), now.minusSeconds(50), null, now.minusSeconds(60));
		// createdAt은 오래됐어도 기준은 마지막 발송(sentAt)이다.
		long callDoneRecentlyResentId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId() + 3,
				now.minusSeconds(30), now.minusSeconds(20), null, now.minusSeconds(400));
		long helpDoneOldId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(),
				now.minusSeconds(300), now.minusSeconds(295), null, now.minusSeconds(301));
		long departureDoneOldId = persist(seeded.sessionId(), null, "DEPARTURE", seeded.childDeviceId(),
				now.minusSeconds(300), now.minusSeconds(295), null, now.minusSeconds(301));

		List<Notification> targets = loadNotificationPort.loadResendTargets(now);

		assertThat(targets).extracting(Notification::getNotificationId)
				.as("CALL은 둘 다 차 있어도 마지막 발송 뒤 60초 이상이면 포함 (정확히 60초는 포함, 59초 · 최근 재발송 · 시드 행은 제외)")
				.contains(callDoneOldId, callDoneExactlyOneMinuteId)
				.doesNotContain(callDoneFiftyNineSecondsId, callDoneRecentlyResentId, seeded.notificationId());
		assertThat(targets).extracting(Notification::getNotificationId)
				.as("CALL이 아닌 행은 둘 다 차면 오래돼도 제외")
				.doesNotContain(helpDoneOldId, departureDoneOldId);
		assertThat(targets).filteredOn(n -> n.getNotificationId() == callDoneOldId).singleElement()
				.usingRecursiveComparison().ignoringFields("createdAt")
				.as("돌려준 CALL 행의 모든 값")
				.isEqualTo(Notification.restore(callDoneOldId, seeded.sessionId(), seeded.callId(), "CALL",
						seeded.childDeviceId(), now.minusSeconds(300), now.minusSeconds(295), null));
	}

	@Test
	@DisplayName("loadResendTargets는 만든 지(createdAt) 24시간보다 오래된 행은 미수신이거나 CALL이어도 돌려주지 않고, 24시간 안의 행과 정확히 24시간 전 행은 조건에 맞으면 돌려준다")
	void loadResendTargetsExcludesRowsCreatedMoreThan24HoursAgo() {
		Instant now = T0.plusSeconds(600);
		Instant cutoff = now.minus(24, ChronoUnit.HOURS);
		// 시드 행은 보냄 · 수신을 모두 채워 대상에서 뺀다.
		jdbcTemplate.update("update notification set type = ?, sent_at = ?, delivered_at = ? where notification_id = ?",
				HELP_TYPE, now.minusSeconds(300).atOffset(ZoneOffset.UTC), now.minusSeconds(290).atOffset(ZoneOffset.UTC),
				seeded.notificationId());
		// 경계: 정확히 24시간 전은 포함
		long exactlyDayOldId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), null, null, null, cutoff);
		// 24시간 안 (1초 차이)
		long insideDayId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId() + 1, null, null, null,
				cutoff.plusSeconds(1));
		// 24시간 1초 전: 미수신이어도 제외
		long justOverDayUndeliveredId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), null, null, null,
				cutoff.minusSeconds(1));
		// 25시간 전 CALL: 둘 다 차 있어도 제외 (sentAt 기준으로는 대상이지만 만든 지 오래됨)
		long oldCallId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(),
				now.minusSeconds(300), now.minusSeconds(290), null, cutoff.minusSeconds(3600));
		// 25시간 전 행: 마지막 발송이 최근이어도 만든 지 오래됐으면 제외
		long oldPendingRecentSendId = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(),
				now.minusSeconds(300), null, null, cutoff.minusSeconds(3600));
		// 23시간 전 CALL: 둘 다 차 있어도 24시간 안이므로 포함
		long recentCallId = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId() + 1,
				now.minusSeconds(300), now.minusSeconds(290), null, now.minus(23, ChronoUnit.HOURS));

		List<Notification> targets = loadNotificationPort.loadResendTargets(now);

		assertThat(targets).extracting(Notification::getNotificationId)
				.as("만든 지 24시간 안이거나 정확히 24시간 전인 대상 행만 (24시간 1초 전 · 25시간 전 행은 제외)")
				.containsExactlyInAnyOrder(exactlyDayOldId, insideDayId, recentCallId)
				.doesNotContain(justOverDayUndeliveredId, oldCallId, oldPendingRecentSendId, seeded.notificationId());
	}

	@Test
	@DisplayName("countAttempts는 같은 세션 · callId · type · deviceId인 행 수를 세고(자기 행 포함), 하나라도 다르면 세지 않으며 callId가 없으면 callId가 빈 행끼리만 센다")
	void countAttemptsCountsRowsWithSameSessionCallTypeAndDevice() {
		persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(), T0, null, null, T0);
		persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(), T0.plusSeconds(60), T0.plusSeconds(61),
				null, T0);
		persist(seeded.sessionId(), seeded.callId() + 1, "CALL", seeded.childDeviceId(), T0, null, null, T0);
		persist(seeded.sessionId(), seeded.callId(), HELP_TYPE, seeded.childDeviceId(), T0, null, null, T0);
		persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId() + 1, T0, null, null, T0);
		persist(otherSessionId, seeded.callId(), "CALL", seeded.childDeviceId(), T0, null, null, T0);
		persist(seeded.sessionId(), null, "CALL", seeded.childDeviceId(), T0, null, null, T0);
		persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), T0, null, null, T0);
		persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), T0.plusSeconds(60), null, null, T0);
		persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), T0.plusSeconds(120), null, null, T0);

		Notification withCall = Notification.create(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId());
		Notification withoutCall = Notification.create(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId());
		Notification noMatch = Notification.create(seeded.sessionId(), seeded.callId(), "SESSION_STARTED",
				seeded.childDeviceId());

		assertThat(loadNotificationPort.countAttempts(withCall))
				.as("같은 세션 · callId · type · deviceId 행 수 (시드 행 1 + 직접 넣은 2)")
				.isEqualTo(3);
		assertThat(loadNotificationPort.countAttempts(withoutCall))
				.as("callId가 없으면 callId가 빈 행끼리만")
				.isEqualTo(3);
		assertThat(loadNotificationPort.countAttempts(noMatch)).as("맞는 행이 없으면 0").isZero();
	}

	@Test
	@DisplayName("save는 id가 없는 새 알림이면 notificationId를 붙여 돌려주고, load로 다시 읽으면 모든 값이 같다")
	void saveNewNotificationAssignsIdAndLoadReturnsSameValues() {
		Notification created = Notification.create(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId());

		Notification saved = saveNotificationPort.save(created);
		persistedIds.add(saved.getNotificationId());

		assertThat(saved.getNotificationId()).as("붙여 준 notificationId").isNotNull().isPositive();
		Notification loaded = loadNotificationPort.load(saved.getNotificationId()).orElseThrow();
		assertThat(loaded).usingRecursiveComparison().ignoringFields("createdAt")
				.as("저장한 값과 다시 읽은 값")
				.isEqualTo(Notification.restore(saved.getNotificationId(), seeded.sessionId(), seeded.callId(), "CALL",
						seeded.childDeviceId(), null, null, null));
		assertThat(saved).usingRecursiveComparison().ignoringFields("createdAt").as("돌려준 값과 다시 읽은 값").isEqualTo(loaded);
	}

	@Test
	@DisplayName("save는 callId가 없는 알림도 callId 빈 채로 저장하고 load로 같은 값을 읽는다")
	void saveNotificationWithoutCallIdKeepsCallIdNull() {
		Notification saved = saveNotificationPort
				.save(Notification.create(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId()));
		persistedIds.add(saved.getNotificationId());

		Notification loaded = loadNotificationPort.load(saved.getNotificationId()).orElseThrow();

		assertThat(loaded.getCallId()).as("callId").isNull();
		assertThat(loaded.getType()).isEqualTo(HELP_TYPE);
		assertThat(loaded.getDeviceId()).isEqualTo(seeded.childDeviceId());
		assertThat(loaded.getSessionId()).isEqualTo(seeded.sessionId());
	}

	@Test
	@DisplayName("이미 notificationId가 있는 알림을 save하면 새 행을 만들지 않고 같은 행의 sentAt · deliveredAt이 바뀐다")
	void saveExistingNotificationUpdatesSameRow() {
		Notification saved = saveNotificationPort
				.save(Notification.create(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId()));
		persistedIds.add(saved.getNotificationId());
		int rowsBefore = loadNotificationPort.loadBySession(seeded.sessionId()).size();

		Notification loaded = loadNotificationPort.load(saved.getNotificationId()).orElseThrow();
		loaded.markSent(T0.plusSeconds(5));
		Notification afterSent = saveNotificationPort.save(loaded);

		assertThat(afterSent.getNotificationId()).as("같은 id").isEqualTo(saved.getNotificationId());
		Notification reloadedAfterSent = loadNotificationPort.load(saved.getNotificationId()).orElseThrow();
		assertThat(reloadedAfterSent.getSentAt()).as("markSent 반영").isEqualTo(T0.plusSeconds(5));
		assertThat(reloadedAfterSent.getDeliveredAt()).isNull();

		reloadedAfterSent.markDelivered(T0.plusSeconds(8));
		saveNotificationPort.save(reloadedAfterSent);

		Notification reloadedAfterDelivered = loadNotificationPort.load(saved.getNotificationId()).orElseThrow();
		assertThat(reloadedAfterDelivered.getSentAt()).as("sentAt 유지").isEqualTo(T0.plusSeconds(5));
		assertThat(reloadedAfterDelivered.getDeliveredAt()).as("markDelivered 반영").isEqualTo(T0.plusSeconds(8));
		assertThat(loadNotificationPort.loadBySession(seeded.sessionId())).as("행 수는 그대로").hasSize(rowsBefore);
	}

	@Test
	@DisplayName("load는 없는 notificationId면 빈 값을 돌려주고, 있는 id면 시드 행의 값을 돌려준다")
	void loadReturnsEmptyForUnknownId() {
		assertThat(loadNotificationPort.load(seeded.notificationId() + 9_000_000L)).as("없는 id").isEmpty();
		assertThat(loadNotificationPort.load(seeded.notificationId())).as("시드 행").isPresent()
				.get().extracting(Notification::getSessionId).isEqualTo(seeded.sessionId());
	}

	@Test
	@DisplayName("loadPending은 그 세션 · type · callId이면서 sentAt이 빈 행만 돌려주고, 세션 · type · callId가 다르거나 sentAt이 찬 행은 돌려주지 않는다")
	void loadPendingReturnsOnlyUnsentRowsOfSessionTypeAndCall() {
		jdbcTemplate.update("update notification set sent_at = ? where notification_id = ?",
				T0.atOffset(ZoneOffset.UTC), seeded.notificationId());
		long pendingA = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(), null, null, null, T0);
		long pendingB = persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId() + 1, null, null, null,
				T0);
		persist(seeded.sessionId(), seeded.callId(), "CALL", seeded.childDeviceId(), T0.plusSeconds(1), null, null, T0);
		persist(seeded.sessionId(), seeded.callId() + 1, "CALL", seeded.childDeviceId(), null, null, null, T0);
		persist(seeded.sessionId(), seeded.callId(), HELP_TYPE, seeded.childDeviceId(), null, null, null, T0);
		persist(seeded.sessionId(), null, "CALL", seeded.childDeviceId(), null, null, null, T0);
		persist(otherSessionId, seeded.callId(), "CALL", seeded.childDeviceId(), null, null, null, T0);

		List<Notification> pending = loadNotificationPort.loadPending(seeded.sessionId(), "CALL", seeded.callId());

		assertThat(pending).extracting(Notification::getNotificationId)
				.as("같은 세션 · type · callId이고 sentAt이 빈 행만")
				.containsExactlyInAnyOrder(pendingA, pendingB);
		assertThat(pending).allSatisfy(n -> assertThat(n.getSentAt()).isNull());
		assertThat(loadNotificationPort.loadPending(seeded.sessionId(), "SESSION_STARTED", seeded.callId()))
				.as("맞는 행이 없으면 빈 목록").isEmpty();
	}

	@Test
	@DisplayName("loadPending은 callId가 null이면 callId가 빈 행 중 그 세션 · type이고 sentAt이 빈 행만 돌려준다")
	void loadPendingWithNullCallIdReturnsOnlyRowsWithoutCallId() {
		long pendingNoCall = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), null, null, null, T0);
		long pendingNoCall2 = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId() + 1, null, null, null,
				T0);
		persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), T0.plusSeconds(1), null, null, T0);
		persist(seeded.sessionId(), seeded.callId(), HELP_TYPE, seeded.childDeviceId(), null, null, null, T0);
		persist(seeded.sessionId(), null, "DEPARTURE", seeded.childDeviceId(), null, null, null, T0);
		persist(otherSessionId, null, HELP_TYPE, seeded.childDeviceId(), null, null, null, T0);

		List<Notification> pending = loadNotificationPort.loadPending(seeded.sessionId(), HELP_TYPE, null);

		assertThat(pending).extracting(Notification::getNotificationId)
				.as("callId가 빈 행 중 같은 세션 · type이고 sentAt이 빈 행만")
				.containsExactlyInAnyOrder(pendingNoCall, pendingNoCall2);
	}

	@Test
		@DisplayName("load로 읽은 Notification의 createdAt은 DB 행의 created_at과 초 단위로 같다")
		void loadReturnsCreatedAtEqualToDbCreatedAtInSeconds() {
			long id = persist(seeded.sessionId(), null, HELP_TYPE, seeded.childDeviceId(), null, null, null,
					T0.plusSeconds(42));

			Notification loaded = loadNotificationPort.load(id).orElseThrow();
			Instant dbCreatedAt = jdbcTemplate
					.queryForObject("select created_at from notification where notification_id = ?", OffsetDateTime.class, id)
					.toInstant();

			assertThat(loaded.getCreatedAt()).as("load로 읽은 createdAt").isNotNull();
			assertThat(loaded.getCreatedAt().truncatedTo(ChronoUnit.SECONDS))
					.as("DB created_at과 초 단위로 같음")
					.isEqualTo(dbCreatedAt.truncatedTo(ChronoUnit.SECONDS));
		}

	/** 서비스를 거치지 않고 행을 넣는다. created_at은 createdAt으로 맞춘다. */
	private long persist(long sessionId, Long callId, String type, long deviceId, Instant sentAt, Instant deliveredAt,
			Instant openedAt, Instant createdAt) {
		Long id = transactionTemplate.execute(tx -> {
			NotificationEntity entity = new NotificationEntity(null, sessionId, callId, type, deviceId, sentAt,
					deliveredAt, openedAt);
			entityManager.persist(entity);
			entityManager.flush();
			return entity.getNotificationId();
		});
		jdbcTemplate.update("update notification set created_at = ? where notification_id = ?",
				createdAt.atOffset(ZoneOffset.UTC), id);
		persistedIds.add(id);
		return id;
	}

}
