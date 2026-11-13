package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.SpotView;
import com.example.pickupservice.place.application.port.in.UpdateSpotCommand;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.DefaultSpotConflictException;
import com.example.pickupservice.place.application.port.out.DistancePort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdatePickupSpotServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long OTHER_FAMILY_ID = 11L;
	private static final long GUARDIAN_ID = 1001L;
	private static final long ACADEMY_ID = 55L;
	private static final long SPOT_ID = 22L;
	private static final long PREVIOUS_DEFAULT_SPOT_ID = 21L;
	private static final long OTHER_SPOT_ID = 23L;
	private static final int MAX_DISTANCE_METERS = 500;

	private static final LatLng ACADEMY_LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng NEAR_STOP = new LatLng(37.501400, 127.039950);
	private static final LatLng NEAR_MEETING = new LatLng(37.501301, 127.039901);
	private static final LatLng NEW_NEAR_STOP = new LatLng(37.502100, 127.040500);
	private static final LatLng NEW_NEAR_MEETING = new LatLng(37.502150, 127.040550);
	private static final LatLng FAR_POINT = new LatLng(37.510000, 127.039876);

	@Mock
	private LoadAcademyPort loadAcademyPort;

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private SaveSpotPort saveSpotPort;

	@Mock
	private DistancePort distancePort;

	private UpdatePickupSpotService updatePickupSpotService;

	@BeforeEach
	void setUp() {
		updatePickupSpotService = new UpdatePickupSpotService(loadAcademyPort, loadSpotPort, saveSpotPort,
				distancePort);
	}

	@Test
	@DisplayName("장소 수정은 이름만 보내면 이름만 바꾸고 나머지 필드는 그대로 둔다")
	void updateSpotChangesOnlyNameWhenOnlyNameIsSent() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", "편의점 앞", NEAR_STOP, NEAR_MEETING, false));
		givenAcademyLeniently();
		givenSaveReturnsArgument();

		SpotView result = updatePickupSpotService.updateSpot(
				new UpdateSpotCommand(FAMILY_ID, SPOT_ID, "새 이름", null, null, null, null));

		assertThat(result).isEqualTo(
				new SpotView(SPOT_ID, "새 이름", NEAR_STOP, NEAR_MEETING, "편의점 앞", false));
		PickupSpot saved = singleSavedSpot();
		assertThat(saved.getSpotId()).isEqualTo(SPOT_ID);
		assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getAcademyId()).isEqualTo(ACADEMY_ID);
		assertThat(saved.getName()).isEqualTo("새 이름");
		assertThat(saved.getMemo()).isEqualTo("편의점 앞");
		assertThat(saved.getStopPoint()).isEqualTo(NEAR_STOP);
		assertThat(saved.getMeetingPoint()).isEqualTo(NEAR_MEETING);
		assertThat(saved.isDefault()).isFalse();
		assertThat(saved.getCreatedBy()).isEqualTo(GUARDIAN_ID);
	}

	@Test
	@DisplayName("장소 수정은 정차 지점 · 메모만 보내면 그 둘만 바꾸고 이름 · 만남 지점 · 기본 여부는 그대로 둔다")
	void updateSpotChangesOnlyStopPointAndMemoWhenOnlyTheyAreSent() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", "편의점 앞", NEAR_STOP, NEAR_MEETING, true));
		givenAcademyLeniently();
		givenOnlyFarPointIsOutside();
		givenSaveReturnsArgument();

		SpotView result = updatePickupSpotService.updateSpot(
				new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, NEW_NEAR_STOP, null, "후문 쪽 계단", null));

		assertThat(result).isEqualTo(
				new SpotView(SPOT_ID, "후문", NEW_NEAR_STOP, NEAR_MEETING, "후문 쪽 계단", true));
		PickupSpot saved = singleSavedSpot();
		assertThat(saved.getSpotId()).isEqualTo(SPOT_ID);
		assertThat(saved.getName()).isEqualTo("후문");
		assertThat(saved.getMemo()).isEqualTo("후문 쪽 계단");
		assertThat(saved.getStopPoint()).isEqualTo(NEW_NEAR_STOP);
		assertThat(saved.getMeetingPoint()).isEqualTo(NEAR_MEETING);
		assertThat(saved.isDefault()).isTrue();
	}

	@Test
	@DisplayName("장소 수정은 바꾼 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다")
	void updateSpotRejectsWithSpotTooFarWhenChangedStopPointIsOver500mFromAcademy() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false));
		givenAcademy();
		givenOnlyFarPointIsOutside();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, FAR_POINT, null, null, null)));

		assertSpotTooFar(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("장소 수정은 바꾼 만남 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다")
	void updateSpotRejectsWithSpotTooFarWhenChangedMeetingPointIsOver500mFromAcademy() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false));
		givenAcademy();
		givenOnlyFarPointIsOutside();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, NEW_NEAR_STOP, FAR_POINT, null, null)));

		assertSpotTooFar(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("장소 수정은 좌표를 바꿀 때 학원 정보가 없으면 NOT_FOUND로 거절하고 저장하지 않는다")
	void updateSpotRejectsWithNotFoundAndDoesNotSaveWhenAcademyIsMissingWhileChangingPoint() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false));
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, NEW_NEAR_STOP, NEW_NEAR_MEETING, null, null)));

		assertNotFound(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("장소 수정은 바꾸지 않은 좌표는 500m 검사를 하지 않는다")
	void updateSpotDoesNotCheckDistanceOfUnchangedPoint() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, FAR_POINT, false));
		givenAcademy();
		givenOnlyFarPointIsOutside();
		givenSaveReturnsArgument();

		SpotView result = updatePickupSpotService.updateSpot(
				new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, NEW_NEAR_STOP, null, null, null));

		assertThat(result.stopPoint()).isEqualTo(NEW_NEAR_STOP);
		assertThat(result.meetingPoint()).isEqualTo(FAR_POINT);
		verify(distancePort).isWithinMeters(ACADEMY_LOCATION, NEW_NEAR_STOP, MAX_DISTANCE_METERS);
		verify(distancePort, never()).isWithinMeters(any(LatLng.class), eq(FAR_POINT), anyInt());
	}

	@Test
	@DisplayName("장소 수정은 isDefault true로 바꾸면 같은 학원의 이전 기본 장소를 해제한다")
	void updateSpotWithIsDefaultTrueDemotesPreviousDefaultSpotOfSameAcademy() {
		PickupSpot target = spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false);
		PickupSpot previousDefault = spot(PREVIOUS_DEFAULT_SPOT_ID, FAMILY_ID, "정문 앞", null, NEAR_STOP,
				NEAR_MEETING, true);
		PickupSpot otherSpot = spot(OTHER_SPOT_ID, FAMILY_ID, "편의점 앞", null, NEAR_STOP, NEAR_MEETING, false);
		givenSpot(target);
		givenAcademyLeniently();
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of(previousDefault, target, otherSpot));
		givenSaveReturnsArgument();

		SpotView result = updatePickupSpotService.updateSpot(
				new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, null, null, null, true));

		assertThat(result.spotId()).isEqualTo(SPOT_ID);
		assertThat(result.isDefault()).isTrue();
		ArgumentCaptor<PickupSpot> captor = ArgumentCaptor.forClass(PickupSpot.class);
		verify(saveSpotPort, atLeastOnce()).saveSpot(captor.capture());
		List<PickupSpot> savedSpots = captor.getAllValues();
		assertThat(savedSpots).hasSize(2);
		assertThat(savedSpots)
				.filteredOn(spot -> Long.valueOf(SPOT_ID).equals(spot.getSpotId()))
				.singleElement()
				.satisfies(updated -> {
					assertThat(updated.isDefault()).isTrue();
					assertThat(updated.getName()).isEqualTo("후문");
				});
		assertThat(savedSpots)
				.filteredOn(spot -> Long.valueOf(PREVIOUS_DEFAULT_SPOT_ID).equals(spot.getSpotId()))
				.singleElement()
				.satisfies(demoted -> {
					assertThat(demoted.isDefault()).isFalse();
					assertThat(demoted.getName()).isEqualTo("정문 앞");
				});
		assertThat(savedSpots)
				.noneMatch(spot -> Long.valueOf(OTHER_SPOT_ID).equals(spot.getSpotId()));
	}

	@Test
	@DisplayName("장소 수정은 isDefault true로 바꾸면 이전 기본 장소 해제를 먼저 저장하고 고친 장소를 나중에 저장한다")
	void updateSpotWithIsDefaultTrueSavesDemotedPreviousDefaultBeforeTargetSpot() {
		PickupSpot target = spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false);
		PickupSpot previousDefault = spot(PREVIOUS_DEFAULT_SPOT_ID, FAMILY_ID, "정문 앞", null, NEAR_STOP,
				NEAR_MEETING, true);
		givenSpot(target);
		givenAcademyLeniently();
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of(previousDefault, target));
		givenSaveReturnsArgument();

		updatePickupSpotService.updateSpot(new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, null, null, null, true));

		InOrder inOrder = inOrder(saveSpotPort);
		inOrder.verify(saveSpotPort).saveSpot(argThat(spot ->
				Long.valueOf(PREVIOUS_DEFAULT_SPOT_ID).equals(spot.getSpotId()) && !spot.isDefault()));
		inOrder.verify(saveSpotPort).saveSpot(argThat(spot ->
				Long.valueOf(SPOT_ID).equals(spot.getSpotId()) && spot.isDefault()));
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	@DisplayName("장소 수정은 저장소가 기본 장소 충돌을 알리면 409 DEFAULT_SPOT_CONFLICT로 거절한다")
	void updateSpotRejectsWithDefaultSpotConflictWhenSavePortReportsDefaultSpotConflict() {
		PickupSpot target = spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false);
		PickupSpot previousDefault = spot(PREVIOUS_DEFAULT_SPOT_ID, FAMILY_ID, "정문 앞", null, NEAR_STOP,
				NEAR_MEETING, true);
		givenSpot(target);
		givenAcademyLeniently();
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of(previousDefault, target));
		given(saveSpotPort.saveSpot(any(PickupSpot.class))).willThrow(DefaultSpotConflictException.class);

		Throwable thrown = catchThrowable(() -> updatePickupSpotService.updateSpot(
				new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, null, null, null, true)));

		assertThat(thrown).isInstanceOf(BusinessException.class);
		BusinessException exception = (BusinessException) thrown;
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("DEFAULT_SPOT_CONFLICT");
	}

	@Test
	@DisplayName("장소 수정은 기본 장소를 isDefault false로 바꾸려 하면 DEFAULT_SPOT_REQUIRED로 거절한다")
	void updateSpotRejectsWithDefaultSpotRequiredWhenDefaultSpotIsSetToNotDefault() {
		PickupSpot target = spot(SPOT_ID, FAMILY_ID, "정문 앞", null, NEAR_STOP, NEAR_MEETING, true);
		PickupSpot otherSpot = spot(OTHER_SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false);
		givenSpot(target);
		givenAcademyLeniently();
		lenient().when(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).thenReturn(List.of(target, otherSpot));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, null, null, null, false)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("DEFAULT_SPOT_REQUIRED");
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("장소 수정은 기본 장소를 isDefault false로 바꾸면서 500m 밖 좌표를 보내도 DEFAULT_SPOT_REQUIRED로 먼저 거절하고 거리 검사를 하지 않는다")
	void updateSpotRejectsWithDefaultSpotRequiredBeforeDistanceCheckWhenFarPointIsAlsoSent() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "정문 앞", null, NEAR_STOP, NEAR_MEETING, true));
		givenAcademyLeniently();
		givenOnlyFarPointIsOutside();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, null, FAR_POINT, FAR_POINT, null, false)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("DEFAULT_SPOT_REQUIRED");
		verifyNoInteractions(distancePort);
		verify(loadAcademyPort, never()).loadAcademy(anyLong());
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("장소 수정은 기본이 아닌 장소에 isDefault false를 보내면 그대로 저장한다")
	void updateSpotSavesNonDefaultSpotWhenIsDefaultFalseIsSent() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false));
		givenAcademyLeniently();
		givenSaveReturnsArgument();

		SpotView result = updatePickupSpotService.updateSpot(
				new UpdateSpotCommand(FAMILY_ID, SPOT_ID, "후문 계단", null, null, null, false));

		assertThat(result).isEqualTo(new SpotView(SPOT_ID, "후문 계단", NEAR_STOP, NEAR_MEETING, null, false));
		PickupSpot saved = singleSavedSpot();
		assertThat(saved.getName()).isEqualTo("후문 계단");
		assertThat(saved.isDefault()).isFalse();
	}

	@Test
	@DisplayName("다른 가족의 spotId를 수정하려 하면 NOT_FOUND로 거절한다")
	void updateSpotRejectsWithNotFoundWhenSpotBelongsToOtherFamily() {
		givenSpot(spot(SPOT_ID, OTHER_FAMILY_ID, "후문", null, NEAR_STOP, NEAR_MEETING, false));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, "새 이름", null, null, null, null)));

		assertNotFound(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("없는 spotId를 수정하려 하면 NOT_FOUND로 거절한다")
	void updateSpotRejectsWithNotFoundWhenSpotDoesNotExist() {
		given(loadSpotPort.loadSpot(SPOT_ID)).willReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, "새 이름", null, null, null, null)));

		assertNotFound(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("임시 장소(temporary=true)를 수정하려 하면 isDefault true를 보내도 NOT_FOUND로 거절하고 이전 기본 장소를 건드리지 않고 저장하지 않는다")
	void updateSpotRejectsWithNotFoundAndDoesNotSaveWhenSpotIsTemporary() {
		givenSpot(PickupSpot.restore(SPOT_ID, FAMILY_ID, ACADEMY_ID, "임시", null, NEAR_STOP, NEAR_MEETING, false,
				GUARDIAN_ID, true));
		lenient().when(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).thenReturn(List.of(
				spot(PREVIOUS_DEFAULT_SPOT_ID, FAMILY_ID, "정문 앞", null, NEAR_STOP, NEAR_MEETING, true)));
		givenAcademyLeniently();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updatePickupSpotService.updateSpot(
						new UpdateSpotCommand(FAMILY_ID, SPOT_ID, "새 이름", null, null, null, true)));

		assertNotFound(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	private static void assertSpotTooFar(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("SPOT_TOO_FAR");
	}

	private static void assertNotFound(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
	}

	private PickupSpot singleSavedSpot() {
		ArgumentCaptor<PickupSpot> captor = ArgumentCaptor.forClass(PickupSpot.class);
		verify(saveSpotPort).saveSpot(captor.capture());
		return captor.getValue();
	}

	private void givenSpot(PickupSpot spot) {
		given(loadSpotPort.loadSpot(spot.getSpotId())).willReturn(Optional.of(spot));
	}

	private void givenAcademy() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID))
				.willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "해솔영어학원", ACADEMY_LOCATION)));
	}

	/** 좌표를 안 바꾸면 학원을 읽지 않아도 되므로 쓰지 않아도 실패하지 않게 둔다. */
	private void givenAcademyLeniently() {
		lenient().when(loadAcademyPort.loadAcademy(ACADEMY_ID))
				.thenReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "해솔영어학원", ACADEMY_LOCATION)));
	}

	/** 학원 위치에서 500m 기준으로 FAR_POINT만 밖이고 나머지는 안이다. */
	private void givenOnlyFarPointIsOutside() {
		lenient().doAnswer(invocation -> !FAR_POINT.equals(invocation.getArgument(1)))
				.when(distancePort).isWithinMeters(eq(ACADEMY_LOCATION), any(LatLng.class), eq(MAX_DISTANCE_METERS));
	}

	private void givenSaveReturnsArgument() {
		willAnswer(invocation -> invocation.getArgument(0)).given(saveSpotPort).saveSpot(any(PickupSpot.class));
	}

	private static PickupSpot spot(long spotId, long familyId, String name, String memo, LatLng stopPoint,
			LatLng meetingPoint, boolean isDefault) {
		return PickupSpot.restore(spotId, familyId, ACADEMY_ID, name, memo, stopPoint, meetingPoint, isDefault,
				GUARDIAN_ID);
	}
}
