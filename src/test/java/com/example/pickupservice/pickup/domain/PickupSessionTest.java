package com.example.pickupservice.pickup.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class PickupSessionTest {

	private static final long VEHICLE_ID = 3L;
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
	private static final long SPOT_ID = 11L;
	private static final String SPOT_NAME = "정문 앞 공영주차장";
	private static final LatLng STOP_POINT = new LatLng(37.5665, 126.9780);
	private static final LatLng MEETING_POINT = new LatLng(37.5667, 126.9783);
	private static final SpotSnapshot SPOT = new SpotSnapshot(SPOT_ID, SPOT_NAME, STOP_POINT, MEETING_POINT);
	private static final long NEW_SPOT_ID = 12L;
	private static final String NEW_SPOT_NAME = "후문 건너편 버스정류장";
	private static final LatLng NEW_STOP_POINT = new LatLng(37.5671, 126.9790);
	private static final LatLng NEW_MEETING_POINT = new LatLng(37.5673, 126.9792);
	private static final SpotSnapshot NEW_SPOT = new SpotSnapshot(NEW_SPOT_ID, NEW_SPOT_NAME, NEW_STOP_POINT,
			NEW_MEETING_POINT);
	private static final long FIRST_CHILD_ID = 21L;
	private static final long FIRST_SCHEDULE_ID = 31L;
	private static final long SECOND_CHILD_ID = 22L;
	private static final long SECOND_SCHEDULE_ID = 32L;
	private static final List<ChildSchedule> TWO_CHILDREN = List.of(
			new ChildSchedule(FIRST_CHILD_ID, FIRST_SCHEDULE_ID),
			new ChildSchedule(SECOND_CHILD_ID, SECOND_SCHEDULE_ID));
	private static final Instant LAST_OBSERVED_AT = Instant.parse("2026-11-09T09:00:00Z");
	private static final Instant LAST_ETA_AT = LAST_OBSERVED_AT.plusSeconds(900);
	private static final int LAST_REMAINING_METERS = 5_000;
	private static final int RESTORED_STATE_VERSION = 5;
	private static final Duration ETA_VALID_FOR = Duration.ofSeconds(60);
	private static final Instant DOOR_AT = Instant.parse("2026-11-09T09:10:00Z");
	private static final Duration CANCEL_WINDOW = Duration.ofSeconds(10);
	private static final Instant COMPLETE_AT = DOOR_AT.plusSeconds(3);
	private static final LatLng CHILD_LOCATION = new LatLng(37.5668, 126.9786);
	private static final Instant CHILD_LOCATED_AT = DOOR_AT.minusSeconds(30);
	private static final Instant INCIDENT_AT = DOOR_AT.plusSeconds(20);
	private static final long UNKNOWN_CHILD_ID = 99L;
	private static final Instant ACKNOWLEDGED_AT = DOOR_AT.minusSeconds(120);
	private static final Instant PREP_REQUESTED_AT = Instant.parse("2026-11-09T09:09:00Z");
	private static final int PREP_LIMIT_MINUTES = 15;
	private static final long FAMILY_ID = 7L;
	private static final Instant RELEASE_AT = INCIDENT_AT.plusSeconds(300);
	private static final Instant EARLIER_RESOLVED_AT = INCIDENT_AT.plusSeconds(60);

	@Test
	@DisplayName("PickupSession.start는 ACTIVE · ROUTING · SELECTED · stateVersion 0인 세션을 만든다")
	void startCreatesActiveRoutingSelectedSessionAtStateVersionZero() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);

		assertThat(session.getSessionId()).isNull();
		assertThat(session.getVehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(session.getDate()).isEqualTo(TODAY);
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getVehiclePhase()).isEqualTo(VehiclePhase.ROUTING);
		assertThat(session.getSpotStatus()).isEqualTo(SpotStatus.SELECTED);
		assertThat(session.getStateVersion()).isZero();
	}

	@Test
	@DisplayName("PickupSession.start는 자녀마다 WAITING인 SessionChild를 만든다")
	void startCreatesWaitingSessionChildForEachChild() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);

		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getScheduleId, SessionChild::getChildPhase)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, FIRST_SCHEDULE_ID, ChildPhase.WAITING),
						tuple(SECOND_CHILD_ID, SECOND_SCHEDULE_ID, ChildPhase.WAITING));
	}

	@Test
	@DisplayName("PickupSession.start는 장소의 spotId · name · stopPoint · meetingPoint를 세션 스냅샷으로 복사한다")
	void startCopiesSpotValuesIntoSessionSnapshot() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);

		assertThat(session.getSpotId()).isEqualTo(SPOT_ID);
		assertThat(session.getSpotName()).isEqualTo(SPOT_NAME);
		assertThat(session.getStopPoint()).isEqualTo(STOP_POINT);
		assertThat(session.getMeetingPoint()).isEqualTo(MEETING_POINT);
	}

	@Test
	@DisplayName("PickupSession.start는 받은 familyId(차량 토큰의 가족)를 세션에 담는다")
	void startStoresFamilyId() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);

		assertThat(session.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(session.getVehicleId()).isEqualTo(VEHICLE_ID);
	}

	@Test
	@DisplayName("PickupSession.restore는 저장된 familyId를 그대로 되살린다")
	void restoreRestoresFamilyId() {
		long storedFamilyId = 8L;

		PickupSession session = PickupSession.restore(1L, VEHICLE_ID, storedFamilyId, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY, SessionStatus.ACTIVE, VehiclePhase.APPROACHING, SpotStatus.CONFIRMED,
				RESTORED_STATE_VERSION, 1, 1, null, false, null, null, null, null, null, null, null, null, null, null, null,
				List.of());

		assertThat(session.getFamilyId()).isEqualTo(storedFamilyId);
		assertThat(session.getVehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(session.getSpotId()).isEqualTo(SPOT_ID);
	}

	@Test
	@DisplayName("세션을 바꾸는 도메인 동작 hold를 하면 stateVersion이 1 오른다")
	void holdRaisesStateVersionByOne() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		int before = session.getStateVersion();

		session.hold();

		assertThat(session.getStateVersion()).isEqualTo(before + 1);
	}

	@Test
	@DisplayName("세션을 바꾸는 도메인 동작 releaseHold를 하면 stateVersion이 1 오른다")
	void releaseHoldRaisesStateVersionByOne() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.hold();
		int before = session.getStateVersion();

		session.releaseHold(RELEASE_AT);

		assertThat(session.getStateVersion()).isEqualTo(before + 1);
	}

	@Test
	@DisplayName("hold는 ACTIVE 세션을 ON_HOLD로 바꾼다")
	void holdChangesActiveSessionToOnHold() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);

		session.hold();

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ON_HOLD);
	}

	@Test
	@DisplayName("releaseHold는 ON_HOLD 세션을 ACTIVE로 되돌린다")
	void releaseHoldReturnsOnHoldSessionToActive() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.hold();

		session.releaseHold(RELEASE_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
	}

	@Test
	@DisplayName("cancel은 ACTIVE 세션을 CANCELLED로 바꾸고 stateVersion을 1 올린다")
	void cancelChangesActiveSessionToCancelled() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		int before = session.getStateVersion();

		session.cancel();

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.CANCELLED);
		assertThat(session.getStateVersion()).isEqualTo(before + 1);
	}

	@Test
	@DisplayName("cancel은 ON_HOLD 세션을 CANCELLED로 바꾸고 stateVersion을 1 올린다")
	void cancelChangesOnHoldSessionToCancelled() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.hold();
		int before = session.getStateVersion();

		session.cancel();

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.CANCELLED);
		assertThat(session.getStateVersion()).isEqualTo(before + 1);
	}

	@Test
	@DisplayName("CANCELLED 세션에 cancel을 부르면 SESSION_ALREADY_FINISHED로 거절되고 세션은 그대로다")
	void cancelOnCancelledSessionIsRejectedWithSessionAlreadyFinished() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.cancel();
		int before = session.getStateVersion();

		assertThatThrownBy(session::cancel)
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.SESSION_ALREADY_FINISHED));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.CANCELLED);
		assertThat(session.getStateVersion()).isEqualTo(before);
	}

	@Test
	@DisplayName("COMPLETED 세션에 cancel을 부르면 SESSION_ALREADY_FINISHED로 거절되고 세션은 그대로다")
	void cancelOnCompletedSessionIsRejectedWithSessionAlreadyFinished() {
		int completedStateVersion = 7;
		PickupSession session = restoreWithoutEta(SessionStatus.COMPLETED, VehiclePhase.DEPARTED,
				completedStateVersion, 0, List.of());

		assertThatThrownBy(session::cancel)
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.SESSION_ALREADY_FINISHED));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(session.getStateVersion()).isEqualTo(completedStateVersion);
	}

	@Test
	@DisplayName("confirmRoute는 spotStatus를 CONFIRMED로 바꾸고 routeVersion과 stateVersion을 1씩 올린다")
	void confirmRouteChangesSpotStatusToConfirmedAndRaisesRouteVersionByOne() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		int routeBefore = session.getRouteVersion();
		int stateBefore = session.getStateVersion();

		session.confirmRoute();

		assertThat(session.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(session.getRouteVersion()).isEqualTo(routeBefore + 1);
		assertThat(session.getStateVersion()).isEqualTo(stateBefore + 1);
	}

	@Test
	@DisplayName("changeSpot은 spotStatus를 SELECTED로 되돌리고 spotVersion과 stateVersion을 1씩 올린다")
	void changeSpotReturnsSpotStatusToSelectedAndRaisesSpotVersionByOne() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.confirmRoute();
		int spotBefore = session.getSpotVersion();
		int stateBefore = session.getStateVersion();

		session.changeSpot(NEW_SPOT);

		assertThat(session.getSpotStatus()).isEqualTo(SpotStatus.SELECTED);
		assertThat(session.getSpotVersion()).isEqualTo(spotBefore + 1);
		assertThat(session.getStateVersion()).isEqualTo(stateBefore + 1);
	}

	@Test
	@DisplayName("changeSpot은 세션 스냅샷의 spotId · spotName · stopPoint · meetingPoint를 새 장소 값으로 바꾼다")
	void changeSpotReplacesSnapshotWithNewSpotValues() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.confirmRoute();

		session.changeSpot(NEW_SPOT);

		assertThat(session.getSpotId()).isEqualTo(NEW_SPOT_ID);
		assertThat(session.getSpotName()).isEqualTo(NEW_SPOT_NAME);
		assertThat(session.getStopPoint()).isEqualTo(NEW_STOP_POINT);
		assertThat(session.getMeetingPoint()).isEqualTo(NEW_MEETING_POINT);
	}

	@Test
	@DisplayName("changeSpot은 MEETING 자녀가 있으면 CHILD_ALREADY_MOVING으로 거절되고 spotVersion · stateVersion · 스냅샷은 그대로다")
	void changeSpotWithMeetingChildIsRejectedWithChildAlreadyMoving() {
		int spotVersionBefore = 1;
		SessionChild meetingChild = child(41L, FIRST_CHILD_ID, FIRST_SCHEDULE_ID, ChildPhase.MEETING, LAST_OBSERVED_AT);
		SessionChild waitingChild = child(42L, SECOND_CHILD_ID, SECOND_SCHEDULE_ID, ChildPhase.WAITING, null);
		PickupSession session = restoreWithoutEta(SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				RESTORED_STATE_VERSION, spotVersionBefore, List.of(meetingChild, waitingChild));

		assertThatThrownBy(() -> session.changeSpot(NEW_SPOT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILD_ALREADY_MOVING));
		assertThat(session.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(session.getSpotVersion()).isEqualTo(spotVersionBefore);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
		assertThat(session.getSpotId()).isEqualTo(SPOT_ID);
		assertThat(session.getSpotName()).isEqualTo(SPOT_NAME);
		assertThat(session.getStopPoint()).isEqualTo(STOP_POINT);
		assertThat(session.getMeetingPoint()).isEqualTo(MEETING_POINT);
	}

	@Test
	@DisplayName("applyTelemetry는 etaAt을 observedAt + navEtaSeconds로, remainingMeters를 새 값으로, etaUpdatedAt을 observedAt으로 덮어쓴다")
	void applyTelemetryOverwritesEtaAtRemainingMetersAndEtaUpdatedAt() {
		PickupSession session = restoreWithLastEta();
		Instant observedAt = LAST_OBSERVED_AT.plusSeconds(15);

		session.applyTelemetry(observedAt, 600, 4_200);

		assertThat(session.getEtaAt()).isEqualTo(observedAt.plusSeconds(600));
		assertThat(session.getRemainingMeters()).isEqualTo(4_200);
		assertThat(session.getEtaUpdatedAt()).isEqualTo(observedAt);
	}

	@Test
	@DisplayName("applyTelemetry는 ETA를 덮어써도 stateVersion을 올리지 않는다")
	void applyTelemetryDoesNotRaiseStateVersion() {
		PickupSession session = restoreWithLastEta();

		session.applyTelemetry(LAST_OBSERVED_AT.plusSeconds(15), 600, 4_200);

		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	@Test
	@DisplayName("applyTelemetry는 observedAt이 마지막 값보다 오래되면 etaAt · remainingMeters · etaUpdatedAt · stateVersion을 바꾸지 않는다")
	void applyTelemetryWithOlderObservedAtChangesNothing() {
		PickupSession session = restoreWithLastEta();

		session.applyTelemetry(LAST_OBSERVED_AT.minusSeconds(1), 100, 300);

		assertThat(session.getEtaAt()).isEqualTo(LAST_ETA_AT);
		assertThat(session.getRemainingMeters()).isEqualTo(LAST_REMAINING_METERS);
		assertThat(session.getEtaUpdatedAt()).isEqualTo(LAST_OBSERVED_AT);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	@Test
	@DisplayName("ETA는 etaUpdatedAt이 60초를 넘게 지나면(61초) stale로 판정된다")
	void etaIsStaleWhenEtaUpdatedAtIsMoreThanSixtySecondsAgo() {
		PickupSession session = restoreWithLastEta();

		assertThat(session.isEtaStale(LAST_OBSERVED_AT.plusSeconds(61), ETA_VALID_FOR)).isTrue();
	}

	@Test
	@DisplayName("ETA는 etaUpdatedAt이 정확히 60초 지났으면 stale이 아니다")
	void etaIsNotStaleWhenEtaUpdatedAtIsExactlySixtySecondsAgo() {
		PickupSession session = restoreWithLastEta();

		assertThat(session.isEtaStale(LAST_OBSERVED_AT.plusSeconds(60), ETA_VALID_FOR)).isFalse();
	}

	@Test
	@DisplayName("ETA를 한 번도 받지 않아 etaUpdatedAt이 없으면 stale로 판정된다")
	void etaIsStaleWhenEtaWasNeverReceived() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);

		assertThat(session.isEtaStale(LAST_OBSERVED_AT, ETA_VALID_FOR)).isTrue();
	}

	@Test
	@DisplayName("PickupSession.start는 경로를 아직 확정하지 않아 routeSpotVersion이 비어 있는 세션을 만든다")
	void startCreatesSessionWithoutRouteSpotVersion() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);

		assertThat(session.getRouteSpotVersion()).isNull();
	}

	@Test
	@DisplayName("confirmRoute는 그때의 spotVersion을 routeSpotVersion에 넣는다")
	void confirmRouteStoresCurrentSpotVersionAsRouteSpotVersion() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.changeSpot(NEW_SPOT);
		int spotVersionAtConfirm = session.getSpotVersion();

		session.confirmRoute();

		assertThat(spotVersionAtConfirm).isEqualTo(1);
		assertThat(session.getRouteSpotVersion()).isEqualTo(spotVersionAtConfirm);
	}

	@Test
	@DisplayName("changeSpot은 routeSpotVersion을 바꾸지 않아 확정 경로의 장소 버전이 지금 spotVersion보다 하나 낮아진다")
	void changeSpotKeepsRouteSpotVersionSoItFallsBehindSpotVersion() {
		PickupSession session = PickupSession.start(VEHICLE_ID, FAMILY_ID, TODAY, SPOT, TWO_CHILDREN);
		session.confirmRoute();
		Integer routeSpotVersionBefore = session.getRouteSpotVersion();

		session.changeSpot(NEW_SPOT);

		assertThat(routeSpotVersionBefore).isZero();
		assertThat(session.getRouteSpotVersion()).isZero();
		assertThat(session.getSpotVersion()).isEqualTo(1);
	}

	@Test
	@DisplayName("restore는 routeSpotVersion을 포함한 저장 값을 그대로 되살린다")
	void restoreWithRouteSpotVersionRestoresAllStoredValues() {
		int spotVersion = 3;
		int routeSpotVersion = 2;
		SessionChild child = child(41L, FIRST_CHILD_ID, FIRST_SCHEDULE_ID, ChildPhase.WAITING, null);

		PickupSession session = PickupSession.restore(1L, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY, SessionStatus.ACTIVE, VehiclePhase.APPROACHING, SpotStatus.CONFIRMED,
				RESTORED_STATE_VERSION, spotVersion, 4, routeSpotVersion, true, LAST_ETA_AT, LAST_REMAINING_METERS,
				LAST_OBSERVED_AT, null, null, null, null, null, null, null, null, List.of(child));

		assertThat(session.getSessionId()).isEqualTo(1L);
		assertThat(session.getVehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(session.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(session.getSpotId()).isEqualTo(SPOT_ID);
		assertThat(session.getSpotName()).isEqualTo(SPOT_NAME);
		assertThat(session.getStopPoint()).isEqualTo(STOP_POINT);
		assertThat(session.getMeetingPoint()).isEqualTo(MEETING_POINT);
		assertThat(session.getDate()).isEqualTo(TODAY);
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getVehiclePhase()).isEqualTo(VehiclePhase.APPROACHING);
		assertThat(session.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
		assertThat(session.getSpotVersion()).isEqualTo(spotVersion);
		assertThat(session.getRouteVersion()).isEqualTo(4);
		assertThat(session.getRouteSpotVersion()).isEqualTo(routeSpotVersion);
		assertThat(session.isCallHeld()).isTrue();
		assertThat(session.getEtaAt()).isEqualTo(LAST_ETA_AT);
		assertThat(session.getRemainingMeters()).isEqualTo(LAST_REMAINING_METERS);
		assertThat(session.getEtaUpdatedAt()).isEqualTo(LAST_OBSERVED_AT);
		assertThat(session.getChildren()).containsExactly(child);
	}

	@ParameterizedTest(name = "door {0}")
	@EnumSource(BoardingDoor.class)
	@DisplayName("detectBoarding은 STOPPED · 운전석 아닌 문 · 호출 발송 뒤면 boardingDetectedAt · boardingCancelableUntil(+10초) · boardingDoor와 모든 자녀의 boardedAt을 채운다")
	void detectBoardingWhenStoppedWithPassengerDoorAfterCallSentFillsBoardingAndAllChildrenBoardedAt(
			BoardingDoor door) {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		boolean detected = session.detectBoarding(door, DOOR_AT, true, CANCEL_WINDOW);

		assertThat(detected).isTrue();
		assertThat(session.getBoardingDetectedAt()).isEqualTo(DOOR_AT);
		assertThat(session.getBoardingCancelableUntil()).isEqualTo(DOOR_AT.plusSeconds(10));
		assertThat(session.getBoardingDoor()).isEqualTo(door);
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isEqualTo(DOOR_AT);
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isEqualTo(DOOR_AT);
	}

	@Test
	@DisplayName("detectBoarding은 탑승 후보가 되면 stateVersion을 1 올린다")
	void detectBoardingRaisesStateVersionByOneWhenBoardingIsDetected() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);

		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@ParameterizedTest(name = "vehiclePhase {0}")
	@EnumSource(value = VehiclePhase.class, names = "STOPPED", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("detectBoarding은 STOPPED가 아니면 탑승 칸 · 자녀 boardedAt · stateVersion을 바꾸지 않고 false를 돌려준다")
	void detectBoardingWhenNotStoppedChangesNothing(VehiclePhase vehiclePhase) {
		PickupSession session = restoreSession(vehiclePhase, ChildPhase.CALLED);

		boolean detected = session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);

		assertThat(detected).isFalse();
		assertNoBoardingChange(session);
	}

	@Test
	@DisplayName("detectBoarding은 합류 안내 발송 전이면 탑승 칸 · 자녀 boardedAt · stateVersion을 바꾸지 않고 false를 돌려준다")
	void detectBoardingBeforeCallSentChangesNothing() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.WAITING);

		boolean detected = session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, false, CANCEL_WINDOW);

		assertThat(detected).isFalse();
		assertNoBoardingChange(session);
	}

	@Test
	@DisplayName("rejectBoarding은 취소 창 안(인식 5초 뒤)이면 그 자녀의 boardedAt만 비우고 다른 자녀 · 탑승 칸 · 자녀 phase는 그대로 둔다")
	void rejectBoardingWithinCancelWindowClearsOnlyThatChildsBoardedAt() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);

		session.rejectBoarding(SECOND_CHILD_ID, DOOR_AT.plusSeconds(5));

		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isNull();
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isEqualTo(DOOR_AT);
		assertThat(session.getBoardingDetectedAt()).isEqualTo(DOOR_AT);
		assertThat(session.getBoardingCancelableUntil()).isEqualTo(DOOR_AT.plusSeconds(10));
		assertThat(session.getBoardingDoor()).isEqualTo(BoardingDoor.REAR_RIGHT);
		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, ChildPhase.CALLED),
						tuple(SECOND_CHILD_ID, ChildPhase.CALLED));
	}

	@Test
	@DisplayName("rejectBoarding은 now가 boardingCancelableUntil과 같으면 아직 창 안이라 그 자녀의 boardedAt을 비운다")
	void rejectBoardingExactlyAtCancelableUntilIsStillWithinWindow() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);

		session.rejectBoarding(FIRST_CHILD_ID, DOOR_AT.plusSeconds(10));

		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isNull();
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isEqualTo(DOOR_AT);
	}

	@Test
	@DisplayName("rejectBoarding은 취소 창 안에서 자녀 boardedAt을 비우면 stateVersion을 1 올린다")
	void rejectBoardingWithinCancelWindowRaisesStateVersionByOne() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);
		int before = session.getStateVersion();

		session.rejectBoarding(SECOND_CHILD_ID, DOOR_AT.plusSeconds(5));

		assertThat(session.getStateVersion()).isEqualTo(before + 1);
	}

	@Test
	@DisplayName("restore는 탑승 칸 boardingDetectedAt · boardingCancelableUntil · boardingDoor를 포함한 저장 값을 그대로 되살린다")
	void restoreWithBoardingRestoresBoardingValues() {
		SessionChild child = child(41L, FIRST_CHILD_ID, FIRST_SCHEDULE_ID, ChildPhase.CALLED, null);
		Instant cancelableUntil = DOOR_AT.plusSeconds(10);

		PickupSession session = PickupSession.restore(1L, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY, SessionStatus.ACTIVE, VehiclePhase.STOPPED, SpotStatus.CONFIRMED,
				RESTORED_STATE_VERSION, 3, 4, 2, true, LAST_ETA_AT, LAST_REMAINING_METERS, LAST_OBSERVED_AT, DOOR_AT,
				cancelableUntil, BoardingDoor.REAR_LEFT, null, null, null, null, null, List.of(child));

		assertThat(session.getSessionId()).isEqualTo(1L);
		assertThat(session.getVehiclePhase()).isEqualTo(VehiclePhase.STOPPED);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
		assertThat(session.getSpotVersion()).isEqualTo(3);
		assertThat(session.getRouteVersion()).isEqualTo(4);
		assertThat(session.getRouteSpotVersion()).isEqualTo(2);
		assertThat(session.isCallHeld()).isTrue();
		assertThat(session.getEtaAt()).isEqualTo(LAST_ETA_AT);
		assertThat(session.getRemainingMeters()).isEqualTo(LAST_REMAINING_METERS);
		assertThat(session.getEtaUpdatedAt()).isEqualTo(LAST_OBSERVED_AT);
		assertThat(session.getBoardingDetectedAt()).isEqualTo(DOOR_AT);
		assertThat(session.getBoardingCancelableUntil()).isEqualTo(cancelableUntil);
		assertThat(session.getBoardingDoor()).isEqualTo(BoardingDoor.REAR_LEFT);
		assertThat(session.getChildren()).containsExactly(child);
	}

	@Test
	@DisplayName("rejectBoarding은 boardingCancelableUntil이 지났으면(1초 뒤) REJECTION_WINDOW_CLOSED로 거절되고 boardedAt · stateVersion은 그대로다")
	void rejectBoardingAfterCancelableUntilIsRejectedWithRejectionWindowClosed() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);
		int before = session.getStateVersion();

		assertThatThrownBy(() -> session.rejectBoarding(SECOND_CHILD_ID, DOOR_AT.plusSeconds(11)))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.REJECTION_WINDOW_CLOSED));
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isEqualTo(DOOR_AT);
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isEqualTo(DOOR_AT);
		assertThat(session.getStateVersion()).isEqualTo(before);
	}

	@ParameterizedTest(name = "vehiclePhase {0}")
	@EnumSource(value = VehiclePhase.class, names = "STOPPED", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("complete는 MANUAL인데 vehiclePhase가 STOPPED가 아니면 VEHICLE_NOT_STOPPED로 거절되고 세션 상태 · 자녀 boardedAt · childPhase · stateVersion은 그대로다")
	void completeManualWhenNotStoppedIsRejectedWithVehicleNotStopped(VehiclePhase vehiclePhase) {
		PickupSession session = restoreSession(vehiclePhase, ChildPhase.CALLED);

		assertThatThrownBy(() -> session.complete(BoardingMethod.MANUAL, List.of(SECOND_CHILD_ID), COMPLETE_AT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.VEHICLE_NOT_STOPPED));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isNull();
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isNull();
		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.CALLED);
	}

	@Test
	@DisplayName("complete는 MANUAL · 정차 중이면 notBoardedChildUserIds의 자녀를 NOT_BOARDED로 바꾸고 세션을 COMPLETED로 바꾼다")
	void completeManualMarksNotBoardedChildAndCompletesSession() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		session.complete(BoardingMethod.MANUAL, List.of(SECOND_CHILD_ID), COMPLETE_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.NOT_BOARDED);
	}

	@Test
	@DisplayName("complete는 MANUAL이면 미탑승 지정 외 자녀의 boardedAt을 now로 채우고 미탑승 지정 자녀의 boardedAt은 비워 둔다")
	void completeManualBoardsChildrenOtherThanNotBoarded() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		session.complete(BoardingMethod.MANUAL, List.of(SECOND_CHILD_ID), COMPLETE_AT);

		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isEqualTo(COMPLETE_AT);
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isNull();
	}

	@Test
	@DisplayName("complete는 IMMEDIATE면 탑승 취소된 자녀를 notBoardedChildUserIds로 받아 NOT_BOARDED로 바꾸고 세션을 COMPLETED로 바꾼다")
	void completeImmediateMarksRejectedChildNotBoardedAndCompletesSession() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);
		session.rejectBoarding(SECOND_CHILD_ID, DOOR_AT.plusSeconds(2));

		session.complete(BoardingMethod.IMMEDIATE, List.of(SECOND_CHILD_ID), COMPLETE_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.NOT_BOARDED);
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isEqualTo(DOOR_AT);
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isNull();
	}

	@Test
	@DisplayName("complete는 IMMEDIATE면 모든 자녀가 탄 세션을 COMPLETED로 바꾸고 NOT_BOARDED 자녀를 만들지 않는다")
	void completeImmediateWithAllChildrenBoardedCompletesSessionWithoutNotBoardedChild() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);

		session.complete(BoardingMethod.IMMEDIATE, List.of(), COMPLETE_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.CALLED);
	}

	@Test
	@DisplayName("complete는 세션을 COMPLETED로 바꾸면 stateVersion을 1 올린다")
	void completeRaisesStateVersionByOne() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);
		int before = session.getStateVersion();

		session.complete(BoardingMethod.IMMEDIATE, List.of(), COMPLETE_AT);

		assertThat(session.getStateVersion()).isEqualTo(before + 1);
	}

	@Test
	@DisplayName("complete는 IMMEDIATE를 탑승 인식 전에 부르면 탄 자녀가 없어 CHILDREN_NOT_ALL_BOARDED로 거절되고 세션 상태 · stateVersion은 그대로다")
	void completeImmediateBeforeBoardingDetectedIsRejectedWithChildrenNotAllBoarded() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		assertThatThrownBy(() -> session.complete(BoardingMethod.IMMEDIATE, List.of(), COMPLETE_AT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILDREN_NOT_ALL_BOARDED));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	@Test
	@DisplayName("complete는 탑승 취소된 자녀를 미탑승 지정하지 않으면 CHILDREN_NOT_ALL_BOARDED로 거절되고 세션 상태 · stateVersion · childPhase는 그대로다")
	void completeImmediateWithRejectedChildNotDesignatedIsRejectedWithChildrenNotAllBoarded() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);
		session.rejectBoarding(SECOND_CHILD_ID, DOOR_AT.plusSeconds(2));
		int before = session.getStateVersion();

		assertThatThrownBy(() -> session.complete(BoardingMethod.IMMEDIATE, List.of(), COMPLETE_AT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILDREN_NOT_ALL_BOARDED));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getStateVersion()).isEqualTo(before);
		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.CALLED);
	}

	@Test
	@DisplayName("complete는 한 자녀만 미탑승 지정하고 다른 자녀가 타지 않았으면 CHILDREN_NOT_ALL_BOARDED로 거절되고 지정 자녀도 NOT_BOARDED로 바뀌지 않는다")
	void completeImmediateWithOtherChildNeitherBoardedNorDesignatedChangesNoChildPhase() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		assertThatThrownBy(() -> session.complete(BoardingMethod.IMMEDIATE, List.of(SECOND_CHILD_ID), COMPLETE_AT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILDREN_NOT_ALL_BOARDED));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.CALLED);
	}

	@ParameterizedTest(name = "boardingMethod {0}")
	@EnumSource(BoardingMethod.class)
	@DisplayName("complete는 탑승한 자녀와 NOT_BOARDED 자녀 모두의 lastLocation · lastLocatedAt을 비운다")
	void completeClearsLastLocationOfAllChildren(BoardingMethod method) {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.getChildren().forEach(child -> child.updateLocation(CHILD_LOCATION, CHILD_LOCATED_AT));
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);
		session.rejectBoarding(SECOND_CHILD_ID, DOOR_AT.plusSeconds(2));

		session.complete(method, List.of(SECOND_CHILD_ID), COMPLETE_AT);

		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getLastLocation,
						SessionChild::getLastLocatedAt)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, null, null),
						tuple(SECOND_CHILD_ID, null, null));
	}

	@Test
	@DisplayName("openIncident는 ACTIVE 세션을 ON_HOLD로 바꾸고 incidentType · incidentOpenedAt을 채우며 incidentResolvedAt은 비워 둔다")
	void openIncidentChangesSessionToOnHoldAndFillsIncidentTypeAndOpenedAt() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID, INCIDENT_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ON_HOLD);
		assertThat(session.getIncidentType()).isEqualTo(IncidentType.CHILD_NOT_ON_BOARD);
		assertThat(session.getIncidentOpenedAt()).isEqualTo(INCIDENT_AT);
		assertThat(session.getIncidentResolvedAt()).isNull();
	}

	@Test
	@DisplayName("openIncident는 대상 자녀를 받아 incidentChildUserId에 저장하고 ON_HOLD · incidentType · incidentOpenedAt도 채운다")
	void openIncidentWithChildUserIdStoresIncidentChildUserId() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID, INCIDENT_AT);

		assertThat(session.getIncidentChildUserId()).isEqualTo(FIRST_CHILD_ID);
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ON_HOLD);
		assertThat(session.getIncidentType()).isEqualTo(IncidentType.CHILD_NOT_ON_BOARD);
		assertThat(session.getIncidentOpenedAt()).isEqualTo(INCIDENT_AT);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("열린 예외가 있는 세션에 대상 자녀를 받는 openIncident를 부르면 INCIDENT_ALREADY_OPEN으로 거절되고 incidentChildUserId는 그대로다")
	void openIncidentWithChildUserIdWhenIncidentAlreadyOpenKeepsFirstChild() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID, INCIDENT_AT);

		assertThatThrownBy(() -> session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, SECOND_CHILD_ID,
				INCIDENT_AT.plusSeconds(30)))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.INCIDENT_ALREADY_OPEN));
		assertThat(session.getIncidentChildUserId()).isEqualTo(FIRST_CHILD_ID);
	}

	@Test
	@DisplayName("대상 자녀를 받는 openIncident는 세션에 없는 childUserId면 CHILD_NOT_IN_SESSION으로 거절되고 sessionStatus · incident 칸 · stateVersion은 그대로다")
	void openIncidentWithUnknownChildIsRejectedWithChildNotInSession() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		assertThatThrownBy(() -> session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, UNKNOWN_CHILD_ID, INCIDENT_AT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILD_NOT_IN_SESSION));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getIncidentType()).isNull();
		assertThat(session.getIncidentOpenedAt()).isNull();
		assertThat(session.getIncidentChildUserId()).isNull();
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	@Test
	@DisplayName("restore는 incidentChildUserId를 그대로 복원한다")
	void restoreKeepsIncidentChildUserId() {
		PickupSession session = PickupSession.restore(1L, 10L, 20L, 30L, "정문 앞", new LatLng(37.5014, 127.03995),
				new LatLng(37.501301, 127.039901), LocalDate.of(2026, 11, 9), SessionStatus.ON_HOLD,
				VehiclePhase.STOPPED, SpotStatus.CONFIRMED, 5, 0, 1, 0, false, null, null, null, null, null, null,
				IncidentType.CHILD_NOT_ON_BOARD, null, FIRST_CHILD_ID, INCIDENT_AT, null, List.of());

		assertThat(session.getIncidentChildUserId()).isEqualTo(FIRST_CHILD_ID);
		assertThat(session.getIncidentType()).isEqualTo(IncidentType.CHILD_NOT_ON_BOARD);
		assertThat(session.getIncidentOpenedAt()).isEqualTo(INCIDENT_AT);
	}

	@Test
	@DisplayName("openIncident는 예외를 열면 stateVersion을 1 올린다")
	void openIncidentRaisesStateVersionByOne() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID, INCIDENT_AT);

		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("열린 예외가 있는 세션에 openIncident를 부르면 INCIDENT_ALREADY_OPEN으로 거절되고 sessionStatus · incidentType · incidentOpenedAt · stateVersion은 그대로다")
	void openIncidentWhenIncidentAlreadyOpenIsRejectedWithIncidentAlreadyOpen() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID, INCIDENT_AT);
		int before = session.getStateVersion();

		assertThatThrownBy(() -> session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID,
				INCIDENT_AT.plusSeconds(30)))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.INCIDENT_ALREADY_OPEN));
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ON_HOLD);
		assertThat(session.getIncidentType()).isEqualTo(IncidentType.CHILD_NOT_ON_BOARD);
		assertThat(session.getIncidentOpenedAt()).isEqualTo(INCIDENT_AT);
		assertThat(session.getIncidentResolvedAt()).isNull();
		assertThat(session.getStateVersion()).isEqualTo(before);
	}

	@Test
	@DisplayName("markChildrenCalled는 세션의 WAITING 자녀를 모두 CALLED로 바꾸고 stateVersion을 1 올린다")
	void markChildrenCalledChangesAllWaitingChildrenToCalledAndRaisesStateVersionByOne() {
		PickupSession session = restoreSession(VehiclePhase.APPROACHING, ChildPhase.WAITING);

		session.markChildrenCalled();

		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.CALLED);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("revertChildrenToWaiting은 MEETING · CALLED 자녀를 모두 WAITING으로 되돌리고 lastLocation · lastLocatedAt · acknowledgedAt을 비우며 stateVersion을 1 올린다")
	void revertChildrenToWaitingReturnsChildrenToWaitingAndClearsLocationAndAcknowledgedAt() {
		SessionChild meeting = SessionChild.restore(41L, FIRST_CHILD_ID, FIRST_SCHEDULE_ID, ChildPhase.MEETING, 0, null,
				CHILD_LOCATION, CHILD_LOCATED_AT, ACKNOWLEDGED_AT, null);
		SessionChild called = SessionChild.restore(42L, SECOND_CHILD_ID, SECOND_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
				CHILD_LOCATION, CHILD_LOCATED_AT, null, null);
		PickupSession session = restoreWithoutEta(SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				RESTORED_STATE_VERSION, 1, List.of(meeting, called));

		session.revertChildrenToWaiting();

		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase, SessionChild::getLastLocation,
						SessionChild::getLastLocatedAt, SessionChild::getAcknowledgedAt)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, ChildPhase.WAITING, null, null, null),
						tuple(SECOND_CHILD_ID, ChildPhase.WAITING, null, null, null));
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("acknowledgeChild는 그 자녀만 MEETING으로 바꾸고 acknowledgedAt을 채우며 다른 자녀는 CALLED 그대로 두고 stateVersion을 1 올린다")
	void acknowledgeChildChangesOnlyThatChildToMeetingAndRaisesStateVersionByOne() {
		PickupSession session = restoreSession(VehiclePhase.APPROACHING, ChildPhase.CALLED);

		session.acknowledgeChild(FIRST_CHILD_ID, ACKNOWLEDGED_AT);

		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase, SessionChild::getAcknowledgedAt)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, ChildPhase.MEETING, ACKNOWLEDGED_AT),
						tuple(SECOND_CHILD_ID, ChildPhase.CALLED, null));
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("extendChildPrep은 09:09에 5분을 더하면 그 자녀만 extraPrepMinutes 5 · callNotBefore 09:14로 바꾸고 childPhase는 WAITING 그대로이며 stateVersion을 1 올린다")
	void extendChildPrepChangesOnlyThatChildPrepAndRaisesStateVersionByOne() {
		PickupSession session = restoreSession(VehiclePhase.APPROACHING, ChildPhase.WAITING);

		session.extendChildPrep(SECOND_CHILD_ID, 5, PREP_REQUESTED_AT, PREP_LIMIT_MINUTES);

		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase,
						SessionChild::getExtraPrepMinutes, SessionChild::getCallNotBefore)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, ChildPhase.WAITING, 0, null),
						tuple(SECOND_CHILD_ID, ChildPhase.WAITING, 5, Instant.parse("2026-11-09T09:14:00Z")));
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("extendChildPrep은 그 자녀의 누적이 15분을 넘으면(10분 + 10분) PREP_EXTENSION_LIMIT으로 거절되고 extraPrepMinutes · stateVersion은 그대로다")
	void extendChildPrepOverLimitIsRejectedWithPrepExtensionLimitAndKeepsStateVersion() {
		SessionChild extended = SessionChild.restore(41L, FIRST_CHILD_ID, FIRST_SCHEDULE_ID, ChildPhase.WAITING, 10,
				Instant.parse("2026-11-09T09:15:00Z"), null, null, null, null);
		SessionChild waiting = child(42L, SECOND_CHILD_ID, SECOND_SCHEDULE_ID, ChildPhase.WAITING, null);
		PickupSession session = restoreWithoutEta(SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				RESTORED_STATE_VERSION, 1, List.of(extended, waiting));

		assertThatThrownBy(() -> session.extendChildPrep(FIRST_CHILD_ID, 10, PREP_REQUESTED_AT, PREP_LIMIT_MINUTES))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.PREP_EXTENSION_LIMIT));
		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getExtraPrepMinutes)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, 10),
						tuple(SECOND_CHILD_ID, 0));
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	@Test
	@DisplayName("updateChildLocation은 그 자녀의 lastLocation · lastLocatedAt만 채우고 다른 자녀 위치는 비워 두며 stateVersion을 올리지 않는다")
	void updateChildLocationUpdatesOnlyThatChildLocationWithoutRaisingStateVersion() {
		PickupSession session = restoreSession(VehiclePhase.APPROACHING, ChildPhase.CALLED);

		session.updateChildLocation(FIRST_CHILD_ID, CHILD_LOCATION, CHILD_LOCATED_AT);

		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getLastLocation,
						SessionChild::getLastLocatedAt)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, CHILD_LOCATION, CHILD_LOCATED_AT),
						tuple(SECOND_CHILD_ID, null, null));
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("childEntryPointsCalledWithUnknownChild")
	@DisplayName("자녀를 바꾸는 진입점은 세션에 없는 childUserId면 CHILD_NOT_IN_SESSION으로 거절되고 자녀 childPhase · stateVersion은 그대로다")
	void childEntryPointWithUnknownChildIsRejectedWithChildNotInSession(String entryPoint,
			Consumer<PickupSession> action) {
		PickupSession session = restoreSession(VehiclePhase.APPROACHING, ChildPhase.CALLED);

		assertThatThrownBy(() -> action.accept(session))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILD_NOT_IN_SESSION));
		assertChildPhases(session, ChildPhase.CALLED, ChildPhase.CALLED);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	@Test
	@DisplayName("rejectBoarding · boardedAtOf도 세션에 없는 childUserId면 CHILD_NOT_IN_SESSION으로 거절되고 자녀 boardedAt · stateVersion은 그대로다")
	void rejectBoardingAndBoardedAtOfWithUnknownChildAreRejectedWithChildNotInSession() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);
		session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW);
		int before = session.getStateVersion();

		assertThatThrownBy(() -> session.rejectBoarding(UNKNOWN_CHILD_ID, DOOR_AT.plusSeconds(5)))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILD_NOT_IN_SESSION));
		assertThatThrownBy(() -> session.boardedAtOf(UNKNOWN_CHILD_ID))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CHILD_NOT_IN_SESSION));
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isEqualTo(DOOR_AT);
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isEqualTo(DOOR_AT);
		assertThat(session.getStateVersion()).isEqualTo(before);
	}

	@Test
	@DisplayName("markStopped는 vehiclePhase를 STOPPED로 바꾸고 stateVersion을 1 올린다")
	void markStoppedChangesVehiclePhaseToStoppedAndRaisesStateVersionByOne() {
		PickupSession session = restoreWith(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, false, null, null, null);

		session.markStopped();

		assertThat(session.getVehiclePhase()).isEqualTo(VehiclePhase.STOPPED);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("markDeparted는 vehiclePhase를 DEPARTED로 바꾸고 stateVersion을 1 올린다")
	void markDepartedChangesVehiclePhaseToDepartedAndRaisesStateVersionByOne() {
		PickupSession session = restoreWith(SessionStatus.ACTIVE, VehiclePhase.STOPPED, false, null, null, null);

		session.markDeparted();

		assertThat(session.getVehiclePhase()).isEqualTo(VehiclePhase.DEPARTED);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@ParameterizedTest(name = "sessionStatus {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("markStopped · markDeparted는 끝난 세션(COMPLETED · CANCELLED)이어도 거절하지 않고 vehiclePhase를 바꾸며 stateVersion을 1씩 올리고 sessionStatus는 그대로다")
	void markStoppedAndMarkDepartedOnFinishedSessionAreNotRejected(SessionStatus finished) {
		PickupSession session = restoreWith(finished, VehiclePhase.APPROACHING, false, null, null, null);

		session.markStopped();
		VehiclePhase afterStopped = session.getVehiclePhase();
		session.markDeparted();

		assertThat(afterStopped).isEqualTo(VehiclePhase.STOPPED);
		assertThat(session.getVehiclePhase()).isEqualTo(VehiclePhase.DEPARTED);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 2);
		assertThat(session.getSessionStatus()).isEqualTo(finished);
	}

	@Test
	@DisplayName("holdCall은 callHeld를 true로 바꾸고 stateVersion을 1 올린다")
	void holdCallSetsCallHeldAndRaisesStateVersionByOne() {
		PickupSession session = restoreWith(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, false, null, null, null);

		session.holdCall();

		assertThat(session.isCallHeld()).isTrue();
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("releaseCallHold는 callHeld를 false로 바꾸고 stateVersion을 1 올린다")
	void releaseCallHoldClearsCallHeldAndRaisesStateVersionByOne() {
		PickupSession session = restoreWith(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, true, null, null, null);

		session.releaseCallHold();

		assertThat(session.isCallHeld()).isFalse();
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@ParameterizedTest(name = "{0} · {1}")
	@MethodSource("callHoldActionsOnFinishedSession")
	@DisplayName("holdCall · releaseCallHold는 끝난 세션(COMPLETED · CANCELLED)이면 SESSION_ALREADY_FINISHED로 거절되고 callHeld · stateVersion · sessionStatus는 그대로다")
	void callHoldActionOnFinishedSessionIsRejectedWithSessionAlreadyFinished(SessionStatus finished, String action,
			boolean callHeldBefore, Consumer<PickupSession> act) {
		PickupSession session = restoreWith(finished, VehiclePhase.APPROACHING, callHeldBefore, null, null, null);

		assertThatThrownBy(() -> act.accept(session))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.SESSION_ALREADY_FINISHED));
		assertThat(session.isCallHeld()).isEqualTo(callHeldBefore);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
		assertThat(session.getSessionStatus()).isEqualTo(finished);
	}

	@ParameterizedTest(name = "{0} · {1}")
	@MethodSource("sessionActionsOnFinishedSession")
	@DisplayName("끝난 세션(COMPLETED · CANCELLED)의 hold · releaseHold · complete · detectBoarding · openIncident는 SESSION_ALREADY_FINISHED로 거절되고 sessionStatus · stateVersion · 탑승 칸 · 자녀 · 예외 칸은 그대로다")
	void sessionActionOnFinishedSessionIsRejectedWithSessionAlreadyFinished(SessionStatus finished, String action,
			Consumer<PickupSession> act) {
		PickupSession session = restoreWith(finished, VehiclePhase.STOPPED, false, null, null, null);

		assertThatThrownBy(() -> act.accept(session))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.SESSION_ALREADY_FINISHED));
		assertThat(session.getSessionStatus()).isEqualTo(finished);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
		assertThat(session.getBoardingDetectedAt()).isNull();
		assertThat(session.getBoardingCancelableUntil()).isNull();
		assertThat(session.getBoardingDoor()).isNull();
		assertThat(session.getIncidentType()).isNull();
		assertThat(session.getIncidentOpenedAt()).isNull();
		assertThat(session.getIncidentResolvedAt()).isNull();
		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase, SessionChild::getBoardedAt)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, ChildPhase.CALLED, null),
						tuple(SECOND_CHILD_ID, ChildPhase.CALLED, null));
	}

	@Test
	@DisplayName("releaseHold는 열린 예외가 있으면 incidentResolvedAt을 now로 채우고 incidentType · incidentOpenedAt은 그대로 두며 세션을 ACTIVE로 되돌리고 stateVersion을 1 올린다")
	void releaseHoldWithOpenIncidentFillsIncidentResolvedAt() {
		PickupSession session = restoreWith(SessionStatus.ON_HOLD, VehiclePhase.STOPPED, false,
				IncidentType.CHILD_NOT_ON_BOARD, INCIDENT_AT, null);

		session.releaseHold(RELEASE_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getIncidentResolvedAt()).isEqualTo(RELEASE_AT);
		assertThat(session.getIncidentType()).isEqualTo(IncidentType.CHILD_NOT_ON_BOARD);
		assertThat(session.getIncidentOpenedAt()).isEqualTo(INCIDENT_AT);
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION + 1);
	}

	@Test
	@DisplayName("releaseHold는 예외 없이 보류만 된 세션이면 incidentResolvedAt · incidentOpenedAt을 비워 둔다")
	void releaseHoldWithoutIncidentLeavesIncidentFieldsEmpty() {
		PickupSession session = restoreWith(SessionStatus.ON_HOLD, VehiclePhase.APPROACHING, false, null, null, null);

		session.releaseHold(RELEASE_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getIncidentOpenedAt()).isNull();
		assertThat(session.getIncidentResolvedAt()).isNull();
	}

	@Test
	@DisplayName("releaseHold는 이미 해소된 예외의 incidentResolvedAt을 now로 덮어쓰지 않는다")
	void releaseHoldKeepsIncidentResolvedAtOfAlreadyResolvedIncident() {
		PickupSession session = restoreWith(SessionStatus.ON_HOLD, VehiclePhase.STOPPED, false,
				IncidentType.CHILD_NOT_ON_BOARD, INCIDENT_AT, EARLIER_RESOLVED_AT);

		session.releaseHold(RELEASE_AT);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getIncidentResolvedAt()).isEqualTo(EARLIER_RESOLVED_AT);
		assertThat(session.getIncidentOpenedAt()).isEqualTo(INCIDENT_AT);
	}

	@Test
	@DisplayName("releaseHold로 예외가 해소된 세션에 openIncident를 부르면 새 예외가 열려 ON_HOLD · incidentOpenedAt은 새 시각 · incidentResolvedAt은 비워진다")
	void openIncidentAfterReleaseHoldResolvedIncidentOpensNewIncident() {
		PickupSession session = restoreWith(SessionStatus.ON_HOLD, VehiclePhase.STOPPED, false,
				IncidentType.CHILD_NOT_ON_BOARD, INCIDENT_AT, null);
		session.releaseHold(RELEASE_AT);
		Instant reopenedAt = RELEASE_AT.plusSeconds(60);

		session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID, reopenedAt);

		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ON_HOLD);
		assertThat(session.getIncidentOpenedAt()).isEqualTo(reopenedAt);
		assertThat(session.getIncidentResolvedAt()).isNull();
	}

	@Test
	@DisplayName("rejectBoarding은 탑승 인식 전(boardingCancelableUntil 없음)이면 REJECTION_WINDOW_CLOSED로 거절되고 자녀 boardedAt · stateVersion은 그대로다")
	void rejectBoardingBeforeBoardingDetectedIsRejectedWithRejectionWindowClosed() {
		PickupSession session = restoreSession(VehiclePhase.STOPPED, ChildPhase.CALLED);

		assertThatThrownBy(() -> session.rejectBoarding(FIRST_CHILD_ID, DOOR_AT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.REJECTION_WINDOW_CLOSED));
		assertThat(session.getBoardingCancelableUntil()).isNull();
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isNull();
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isNull();
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	static Stream<Arguments> callHoldActionsOnFinishedSession() {
		return Stream.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED)
				.flatMap(finished -> Stream.of(
						Arguments.of(finished, "holdCall", false, (Consumer<PickupSession>) PickupSession::holdCall),
						Arguments.of(finished, "releaseCallHold", true,
								(Consumer<PickupSession>) PickupSession::releaseCallHold)));
	}

	static Stream<Arguments> sessionActionsOnFinishedSession() {
		Map<String, Consumer<PickupSession>> actions = new LinkedHashMap<>();
		actions.put("hold", PickupSession::hold);
		actions.put("releaseHold", session -> session.releaseHold(RELEASE_AT));
		actions.put("complete", session -> session.complete(BoardingMethod.MANUAL, List.of(), COMPLETE_AT));
		actions.put("detectBoarding",
				session -> session.detectBoarding(BoardingDoor.REAR_RIGHT, DOOR_AT, true, CANCEL_WINDOW));
		actions.put("openIncident",
				session -> session.openIncident(IncidentType.CHILD_NOT_ON_BOARD, FIRST_CHILD_ID, INCIDENT_AT));
		return Stream.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED)
				.flatMap(finished -> actions.entrySet().stream()
						.map(action -> Arguments.of(finished, action.getKey(), action.getValue())));
	}

	static Stream<Arguments> childEntryPointsCalledWithUnknownChild() {
		return Stream.of(
				Arguments.of("acknowledgeChild",
						(Consumer<PickupSession>) session -> session.acknowledgeChild(UNKNOWN_CHILD_ID,
								ACKNOWLEDGED_AT)),
				Arguments.of("extendChildPrep",
						(Consumer<PickupSession>) session -> session.extendChildPrep(UNKNOWN_CHILD_ID, 5,
								PREP_REQUESTED_AT, PREP_LIMIT_MINUTES)),
				Arguments.of("updateChildLocation",
						(Consumer<PickupSession>) session -> session.updateChildLocation(UNKNOWN_CHILD_ID,
								CHILD_LOCATION, CHILD_LOCATED_AT)));
	}

	private static void assertChildPhases(PickupSession session, ChildPhase firstPhase, ChildPhase secondPhase) {
		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase)
				.containsExactlyInAnyOrder(
						tuple(FIRST_CHILD_ID, firstPhase),
						tuple(SECOND_CHILD_ID, secondPhase));
	}

	private static void assertNoBoardingChange(PickupSession session) {
		assertThat(session.getBoardingDetectedAt()).isNull();
		assertThat(session.getBoardingCancelableUntil()).isNull();
		assertThat(session.getBoardingDoor()).isNull();
		assertThat(session.boardedAtOf(FIRST_CHILD_ID)).isNull();
		assertThat(session.boardedAtOf(SECOND_CHILD_ID)).isNull();
		assertThat(session.getStateVersion()).isEqualTo(RESTORED_STATE_VERSION);
	}

	private static PickupSession restoreSession(VehiclePhase vehiclePhase, ChildPhase childPhase) {
		SessionChild first = child(41L, FIRST_CHILD_ID, FIRST_SCHEDULE_ID, childPhase, null);
		SessionChild second = child(42L, SECOND_CHILD_ID, SECOND_SCHEDULE_ID, childPhase, null);
		return restoreWithoutEta(SessionStatus.ACTIVE, vehiclePhase, RESTORED_STATE_VERSION, 1,
				List.of(first, second));
	}

	private static PickupSession restoreWithoutEta(SessionStatus sessionStatus, VehiclePhase vehiclePhase,
			int stateVersion, int spotVersion, List<SessionChild> children) {
		return PickupSession.restore(1L, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT, MEETING_POINT, TODAY,
				sessionStatus, vehiclePhase, SpotStatus.CONFIRMED, stateVersion, spotVersion, 1, null, false, null,
				null, null, null, null, null, null, null, null, null, null, children);
	}

	private static PickupSession restoreWith(SessionStatus sessionStatus, VehiclePhase vehiclePhase, boolean callHeld,
			IncidentType incidentType, Instant incidentOpenedAt, Instant incidentResolvedAt) {
		SessionChild first = child(41L, FIRST_CHILD_ID, FIRST_SCHEDULE_ID, ChildPhase.CALLED, null);
		SessionChild second = child(42L, SECOND_CHILD_ID, SECOND_SCHEDULE_ID, ChildPhase.CALLED, null);
		return PickupSession.restore(1L, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT, MEETING_POINT, TODAY,
				sessionStatus, vehiclePhase, SpotStatus.CONFIRMED, RESTORED_STATE_VERSION, 1, 1, null, callHeld,
				null, null, null, null, null, null, incidentType, null, null, incidentOpenedAt, incidentResolvedAt,
				List.of(first, second));
	}

	private static PickupSession restoreWithLastEta() {
		return PickupSession.restore(1L, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT, MEETING_POINT, TODAY,
				SessionStatus.ACTIVE, VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, RESTORED_STATE_VERSION, 1, 1,
				null, false, LAST_ETA_AT, LAST_REMAINING_METERS, LAST_OBSERVED_AT, null, null, null, null, null, null,
				null, null, List.of());
	}

	private static SessionChild child(long sessionChildId, long childUserId, long scheduleId, ChildPhase childPhase,
			Instant acknowledgedAt) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, childPhase, 0, null, null, null,
				acknowledgedAt, null);
	}
}
