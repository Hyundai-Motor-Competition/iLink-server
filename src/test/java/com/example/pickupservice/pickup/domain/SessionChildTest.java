package com.example.pickupservice.pickup.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionChildTest {

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
	private static final long SESSION_CHILD_ID = 41L;
	private static final long CHILD_USER_ID = 21L;
	private static final long SCHEDULE_ID = 31L;
	private static final int PREP_LIMIT_MINUTES = 15;
	private static final LatLng LOCATION_A = new LatLng(37.497942, 127.027621);
	private static final LatLng LOCATION_B = new LatLng(37.498511, 127.028304);

	@Test
	@DisplayName("SessionChild.acknowledge를 두 번 부르면 첫 acknowledgedAt이 유지된다")
	void acknowledgeTwiceKeepsFirstAcknowledgedAt() {
		SessionChild child = calledChild();
		Instant first = at(18, 12);
		Instant second = at(18, 13);

		child.acknowledge(first);
		child.acknowledge(second);

		assertThat(child.getAcknowledgedAt()).isEqualTo(first);
	}

	@Test
	@DisplayName("SessionChild.acknowledge는 CALLED 자녀를 MEETING으로 바꾼다")
	void acknowledgeChangesCalledChildToMeeting() {
		SessionChild child = calledChild();

		child.acknowledge(at(18, 12));

		assertThat(child.getChildPhase()).isEqualTo(ChildPhase.MEETING);
	}

	@Test
	@DisplayName("extendPrep은 18:09에 5분을 더하면 extraPrepMinutes 5 · callNotBefore 18:14로 만든다")
	void extendPrepFiveMinutesAt1809SetsExtraFiveAndCallNotBefore1814() {
		SessionChild child = SessionChild.waiting(CHILD_USER_ID, SCHEDULE_ID);

		child.extendPrep(5, at(18, 9), PREP_LIMIT_MINUTES);

		assertThat(child.getExtraPrepMinutes()).isEqualTo(5);
		assertThat(child.getCallNotBefore()).isEqualTo(at(18, 14));
	}

	@Test
	@DisplayName("extendPrep은 18:05에 5분, 18:09에 5분을 더하면 extraPrepMinutes 10 · callNotBefore 18:19로 만든다")
	void extendPrepTwiceAccumulatesMinutesAndSetsCallNotBeforeFromLatestNow() {
		SessionChild child = SessionChild.waiting(CHILD_USER_ID, SCHEDULE_ID);

		child.extendPrep(5, at(18, 5), PREP_LIMIT_MINUTES);
		child.extendPrep(5, at(18, 9), PREP_LIMIT_MINUTES);

		assertThat(child.getExtraPrepMinutes()).isEqualTo(10);
		assertThat(child.getCallNotBefore()).isEqualTo(at(18, 19));
	}

	@Test
	@DisplayName("extendPrep은 누적이 15분을 넘으면(10분 + 10분) PREP_EXTENSION_LIMIT으로 거절되고 extraPrepMinutes · callNotBefore는 그대로다")
	void extendPrepRejectsWithPrepExtensionLimitWhenAccumulatedExceedsFifteenMinutes() {
		SessionChild child = SessionChild.waiting(CHILD_USER_ID, SCHEDULE_ID);
		child.extendPrep(10, at(18, 5), PREP_LIMIT_MINUTES);

		assertThatThrownBy(() -> child.extendPrep(10, at(18, 9), PREP_LIMIT_MINUTES))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.PREP_EXTENSION_LIMIT));

		assertThat(child.getExtraPrepMinutes()).isEqualTo(10);
		assertThat(child.getCallNotBefore()).isEqualTo(at(18, 15));
	}

	@Test
	@DisplayName("extendPrep은 누적이 정확히 15분(5분 + 10분)이면 받아들여 extraPrepMinutes 15 · callNotBefore 18:24로 만든다")
	void extendPrepAcceptsAccumulatedExactlyFifteenMinutesInTwoSteps() {
		SessionChild child = SessionChild.waiting(CHILD_USER_ID, SCHEDULE_ID);

		child.extendPrep(5, at(18, 5), PREP_LIMIT_MINUTES);
		child.extendPrep(10, at(18, 9), PREP_LIMIT_MINUTES);

		assertThat(child.getExtraPrepMinutes()).isEqualTo(15);
		assertThat(child.getCallNotBefore()).isEqualTo(at(18, 24));
	}

	@Test
	@DisplayName("extendPrep은 15분을 한 번에 더하면 받아들여 extraPrepMinutes 15 · callNotBefore 18:24로 만든다")
	void extendPrepAcceptsFifteenMinutesAtOnce() {
		SessionChild child = SessionChild.waiting(CHILD_USER_ID, SCHEDULE_ID);

		child.extendPrep(15, at(18, 9), PREP_LIMIT_MINUTES);

		assertThat(child.getExtraPrepMinutes()).isEqualTo(15);
		assertThat(child.getCallNotBefore()).isEqualTo(at(18, 24));
	}

	@Test
	@DisplayName("extendPrep은 childPhase를 바꾸지 않는다(WAITING 그대로)")
	void extendPrepKeepsChildPhaseWaiting() {
		SessionChild child = SessionChild.waiting(CHILD_USER_ID, SCHEDULE_ID);

		child.extendPrep(5, at(18, 9), PREP_LIMIT_MINUTES);

		assertThat(child.getChildPhase()).isEqualTo(ChildPhase.WAITING);
	}

	@Test
	@DisplayName("updateLocation은 저장된 위치가 없으면 받은 위치와 at을 lastLocation · lastLocatedAt에 넣는다")
	void updateLocationSetsFirstLocationWhenNoneStored() {
		SessionChild child = calledChild();

		child.updateLocation(LOCATION_A, at(18, 12));

		assertThat(child.getLastLocation()).isEqualTo(LOCATION_A);
		assertThat(child.getLastLocatedAt()).isEqualTo(at(18, 12));
	}

	@Test
	@DisplayName("updateLocation은 at이 저장된 값보다 새로우면 위치와 시각을 덮어쓴다")
	void updateLocationOverwritesWhenAtIsNewerThanStored() {
		SessionChild child = calledChild();
		child.updateLocation(LOCATION_A, at(18, 12));

		child.updateLocation(LOCATION_B, at(18, 13));

		assertThat(child.getLastLocation()).isEqualTo(LOCATION_B);
		assertThat(child.getLastLocatedAt()).isEqualTo(at(18, 13));
	}

	@Test
	@DisplayName("updateLocation은 at이 저장된 값보다 오래되면 위치와 시각을 바꾸지 않는다")
	void updateLocationIgnoresWhenAtIsOlderThanStored() {
		SessionChild child = calledChild();
		child.updateLocation(LOCATION_A, at(18, 13));

		child.updateLocation(LOCATION_B, at(18, 12));

		assertThat(child.getLastLocation()).isEqualTo(LOCATION_A);
		assertThat(child.getLastLocatedAt()).isEqualTo(at(18, 13));
	}

	@Test
	@DisplayName("updateLocation은 DB에서 복원한 위치보다 at이 오래되면 위치와 시각을 바꾸지 않는다")
	void updateLocationIgnoresWhenAtIsOlderThanRestoredLocation() {
		SessionChild child = SessionChild.restore(SESSION_CHILD_ID, CHILD_USER_ID, SCHEDULE_ID, ChildPhase.MEETING, 0,
				null, LOCATION_A, at(18, 13), at(18, 11), null);

		child.updateLocation(LOCATION_B, at(18, 12));

		assertThat(child.getLastLocation()).isEqualTo(LOCATION_A);
		assertThat(child.getLastLocatedAt()).isEqualTo(at(18, 13));
	}

	@Test
	@DisplayName("updateLocation은 at이 저장된 값과 같은 시각이면 위치를 덮어쓴다")
	void updateLocationOverwritesWhenAtEqualsStored() {
		SessionChild child = calledChild();
		child.updateLocation(LOCATION_A, at(18, 12));

		child.updateLocation(LOCATION_B, at(18, 12));

		assertThat(child.getLastLocation()).isEqualTo(LOCATION_B);
		assertThat(child.getLastLocatedAt()).isEqualTo(at(18, 12));
	}

	@Test
	@DisplayName("clearLocation은 저장된 lastLocation · lastLocatedAt을 모두 비운다")
	void clearLocationEmptiesLastLocationAndLastLocatedAt() {
		SessionChild child = calledChild();
		child.updateLocation(LOCATION_A, at(18, 12));

		child.clearLocation();

		assertThat(child.getLastLocation()).isNull();
		assertThat(child.getLastLocatedAt()).isNull();
	}

	@Test
	@DisplayName("revertToWaiting은 MEETING 자녀를 WAITING으로 되돌리고 acknowledgedAt · lastLocation · lastLocatedAt을 비운다")
	void revertToWaitingReturnsMeetingChildToWaitingAndClearsAcknowledgedAtAndLocation() {
		SessionChild child = SessionChild.restore(SESSION_CHILD_ID, CHILD_USER_ID, SCHEDULE_ID, ChildPhase.MEETING, 0,
				null, LOCATION_A, at(18, 13), at(18, 12), null);

		child.revertToWaiting();

		assertThat(child.getChildPhase()).isEqualTo(ChildPhase.WAITING);
		assertThat(child.getAcknowledgedAt()).isNull();
		assertThat(child.getLastLocation()).isNull();
		assertThat(child.getLastLocatedAt()).isNull();
	}

	@Test
	@DisplayName("revertToWaiting 뒤 새 호출에서 다시 acknowledge하면 새 시각이 acknowledgedAt에 들어간다")
	void acknowledgeAfterRevertToWaitingRecordsNewAcknowledgedAt() {
		SessionChild child = calledChild();
		child.acknowledge(at(18, 12));
		child.revertToWaiting();

		child.acknowledge(at(18, 20));

		assertThat(child.getAcknowledgedAt()).isEqualTo(at(18, 20));
		assertThat(child.getChildPhase()).isEqualTo(ChildPhase.MEETING);
	}

	private static SessionChild calledChild() {
		return SessionChild.restore(SESSION_CHILD_ID, CHILD_USER_ID, SCHEDULE_ID, ChildPhase.CALLED, 0, null, null,
				null, null, null);
	}

	private static Instant at(int hour, int minute) {
		return TODAY.atTime(LocalTime.of(hour, minute)).atZone(KOREA).toInstant();
	}
}
