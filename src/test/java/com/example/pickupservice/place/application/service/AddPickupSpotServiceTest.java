package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.AddSpotCommand;
import com.example.pickupservice.place.application.port.in.SpotCreated;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddPickupSpotServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long GUARDIAN_ID = 1001L;
	private static final long CALLER_GUARDIAN_ID = 1002L;
	private static final long ACADEMY_ID = 55L;
	private static final long NEW_SPOT_ID = 30L;
	private static final long PREVIOUS_DEFAULT_SPOT_ID = 21L;
	private static final long OTHER_SPOT_ID = 22L;
	private static final int MAX_DISTANCE_METERS = 500;

	private static final LatLng ACADEMY_LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng NEAR_STOP = new LatLng(37.501400, 127.039950);
	private static final LatLng NEAR_MEETING = new LatLng(37.501301, 127.039901);
	private static final LatLng FAR_POINT = new LatLng(37.510000, 127.039876);

	@Mock
	private LoadAcademyPort loadAcademyPort;

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private SaveSpotPort saveSpotPort;

	@Mock
	private DistancePort distancePort;

	private AddPickupSpotService addPickupSpotService;

	@BeforeEach
	void setUp() {
		addPickupSpotService = new AddPickupSpotService(loadAcademyPort, loadSpotPort, saveSpotPort, distancePort);
	}

	@Test
	@DisplayName("장소 추가는 학원의 첫 장소면 isDefault false로 보내도 isDefault true로 저장한다")
	void addSpotSavesFirstSpotOfAcademyAsDefaultEvenWhenIsDefaultFalse() {
		givenAcademy();
		givenOnlyFarPointIsOutside();
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of());
		givenSaveAssignsNewSpotId();

		SpotCreated result = addPickupSpotService.addSpot(command(NEAR_STOP, NEAR_MEETING, false));

		assertThat(result).isEqualTo(new SpotCreated(NEW_SPOT_ID, true));
		ArgumentCaptor<PickupSpot> captor = ArgumentCaptor.forClass(PickupSpot.class);
		verify(saveSpotPort).saveSpot(captor.capture());
		PickupSpot saved = captor.getValue();
		assertThat(saved.getSpotId()).isNull();
		assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getAcademyId()).isEqualTo(ACADEMY_ID);
		assertThat(saved.getName()).isEqualTo("정문 앞");
		assertThat(saved.getStopPoint()).isEqualTo(NEAR_STOP);
		assertThat(saved.getMeetingPoint()).isEqualTo(NEAR_MEETING);
		assertThat(saved.isDefault()).isTrue();
	}

	@Test
	@DisplayName("장소 추가는 isDefault true면 같은 학원의 이전 기본 장소를 해제한다")
	void addSpotWithIsDefaultTrueDemotesPreviousDefaultSpotOfSameAcademy() {
		givenAcademy();
		givenOnlyFarPointIsOutside();
		PickupSpot previousDefault = existingSpot(PREVIOUS_DEFAULT_SPOT_ID, "후문", true);
		PickupSpot otherSpot = existingSpot(OTHER_SPOT_ID, "편의점 앞", false);
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of(previousDefault, otherSpot));
		givenSaveAssignsNewSpotId();

		SpotCreated result = addPickupSpotService.addSpot(command(NEAR_STOP, NEAR_MEETING, true));

		assertThat(result).isEqualTo(new SpotCreated(NEW_SPOT_ID, true));
		ArgumentCaptor<PickupSpot> captor = ArgumentCaptor.forClass(PickupSpot.class);
		verify(saveSpotPort, atLeastOnce()).saveSpot(captor.capture());
		List<PickupSpot> savedSpots = captor.getAllValues();
		assertThat(savedSpots).hasSize(2);
		assertThat(savedSpots)
				.filteredOn(spot -> spot.getSpotId() == null)
				.singleElement()
				.satisfies(newSpot -> assertThat(newSpot.isDefault()).isTrue());
		assertThat(savedSpots)
				.filteredOn(spot -> Long.valueOf(PREVIOUS_DEFAULT_SPOT_ID).equals(spot.getSpotId()))
				.singleElement()
				.satisfies(demoted -> {
					assertThat(demoted.isDefault()).isFalse();
					assertThat(demoted.getName()).isEqualTo("후문");
				});
		assertThat(savedSpots)
				.noneMatch(spot -> Long.valueOf(OTHER_SPOT_ID).equals(spot.getSpotId()));
	}

	@Test
	@DisplayName("장소 추가는 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다")
	void addSpotRejectsWithSpotTooFarWhenStopPointIsOver500mFromAcademy() {
		givenAcademy();
		givenOnlyFarPointIsOutside();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> addPickupSpotService.addSpot(command(FAR_POINT, NEAR_MEETING, false)));

		assertSpotTooFar(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("장소 추가는 만남 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다")
	void addSpotRejectsWithSpotTooFarWhenMeetingPointIsOver500mFromAcademy() {
		givenAcademy();
		givenOnlyFarPointIsOutside();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> addPickupSpotService.addSpot(command(NEAR_STOP, FAR_POINT, false)));

		assertSpotTooFar(exception);
		verify(saveSpotPort, never()).saveSpot(any());
	}

	@Test
	@DisplayName("장소 추가는 호출한 보호자를 createdBy로 저장한다")
	void addSpotSavesCallingGuardianAsCreatedBy() {
		givenAcademy();
		givenOnlyFarPointIsOutside();
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID))
				.willReturn(List.of(existingSpot(PREVIOUS_DEFAULT_SPOT_ID, "후문", true)));
		givenSaveAssignsNewSpotId();

		addPickupSpotService.addSpot(new AddSpotCommand(FAMILY_ID, CALLER_GUARDIAN_ID, ACADEMY_ID, "정문 앞",
				NEAR_STOP, NEAR_MEETING, "편의점 앞", false));

		ArgumentCaptor<PickupSpot> captor = ArgumentCaptor.forClass(PickupSpot.class);
		verify(saveSpotPort, atLeastOnce()).saveSpot(captor.capture());
		assertThat(captor.getAllValues())
				.filteredOn(spot -> spot.getSpotId() == null)
				.singleElement()
				.satisfies(newSpot -> assertThat(newSpot.getCreatedBy()).isEqualTo(CALLER_GUARDIAN_ID));
	}

	@Test
	@DisplayName("장소 추가는 학원이 없으면 NOT_FOUND로 거절하고 저장하지 않는다")
	void addSpotRejectsWithNotFoundWhenAcademyDoesNotExist() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> addPickupSpotService.addSpot(command(NEAR_STOP, NEAR_MEETING, false)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		verify(saveSpotPort, never()).saveSpot(any());
	}

	private static void assertSpotTooFar(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("SPOT_TOO_FAR");
	}

	private void givenAcademy() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID))
				.willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "해솔영어학원", ACADEMY_LOCATION)));
	}

	/** 학원 위치에서 500m 기준으로 FAR_POINT만 밖이고 나머지는 안이다. */
	private void givenOnlyFarPointIsOutside() {
		willAnswer(invocation -> !FAR_POINT.equals(invocation.getArgument(1)))
				.given(distancePort).isWithinMeters(eq(ACADEMY_LOCATION), any(LatLng.class), eq(MAX_DISTANCE_METERS));
	}

	private void givenSaveAssignsNewSpotId() {
		willAnswer(invocation -> {
			PickupSpot spot = invocation.getArgument(0);
			if (spot.getSpotId() != null) {
				return spot;
			}
			return PickupSpot.restore(NEW_SPOT_ID, spot.getFamilyId(), spot.getAcademyId(), spot.getName(),
					spot.getMemo(), spot.getStopPoint(), spot.getMeetingPoint(), spot.isDefault(), spot.getCreatedBy());
		}).given(saveSpotPort).saveSpot(any(PickupSpot.class));
	}

	private static PickupSpot existingSpot(long spotId, String name, boolean isDefault) {
		return PickupSpot.restore(spotId, FAMILY_ID, ACADEMY_ID, name, null, NEAR_STOP, NEAR_MEETING, isDefault,
				GUARDIAN_ID);
	}

	private static AddSpotCommand command(LatLng stopPoint, LatLng meetingPoint, boolean isDefault) {
		return new AddSpotCommand(FAMILY_ID, GUARDIAN_ID, ACADEMY_ID, "정문 앞", stopPoint, meetingPoint, "편의점 앞",
				isDefault);
	}
}
