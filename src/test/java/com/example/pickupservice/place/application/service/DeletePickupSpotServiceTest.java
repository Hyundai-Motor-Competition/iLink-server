package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.DeleteSpotCommand;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeletePickupSpotServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long OTHER_FAMILY_ID = 11L;
	private static final long GUARDIAN_ID = 1001L;
	private static final long ACADEMY_ID = 55L;
	private static final long SPOT_ID = 22L;
	private static final long OTHER_SPOT_ID = 23L;

	private static final LatLng NEAR_STOP = new LatLng(37.501400, 127.039950);
	private static final LatLng NEAR_MEETING = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private SaveSpotPort saveSpotPort;

	@Mock
	private CheckActivePickupPort checkActivePickupPort;

	private DeletePickupSpotService deletePickupSpotService;

	@BeforeEach
	void setUp() {
		deletePickupSpotService = new DeletePickupSpotService(loadSpotPort, saveSpotPort, checkActivePickupPort);
	}

	@Test
	@DisplayName("장소 삭제는 진행 중 세션이 쓰지 않는 기본 아닌 장소를 지운다")
	void deleteSpotDeletesNonDefaultSpotNotUsedByActiveSession() {
		PickupSpot target = spot(SPOT_ID, FAMILY_ID, "후문", false);
		PickupSpot defaultSpot = spot(OTHER_SPOT_ID, FAMILY_ID, "정문 앞", true);
		givenSpot(target);
		given(checkActivePickupPort.isUsedByActiveSession(SPOT_ID)).willReturn(false);
		lenient().when(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).thenReturn(List.of(defaultSpot, target));

		deletePickupSpotService.deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID));

		verify(saveSpotPort).deleteSpot(SPOT_ID);
	}

	@Test
	@DisplayName("장소 삭제는 학원에 하나 남은 기본 장소를 지운다")
	void deleteSpotDeletesLastDefaultSpotOfAcademy() {
		PickupSpot target = spot(SPOT_ID, FAMILY_ID, "정문 앞", true);
		givenSpot(target);
		given(checkActivePickupPort.isUsedByActiveSession(SPOT_ID)).willReturn(false);
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of(target));

		deletePickupSpotService.deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID));

		verify(saveSpotPort).deleteSpot(SPOT_ID);
	}

	@Test
	@DisplayName("장소 삭제는 진행 중 세션이 쓰는 장소면 SPOT_IN_USE로 거절한다")
	void deleteSpotRejectsWithSpotInUseWhenSpotIsUsedByActiveSession() {
		givenSpot(spot(SPOT_ID, FAMILY_ID, "후문", false));
		given(checkActivePickupPort.isUsedByActiveSession(SPOT_ID)).willReturn(true);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> deletePickupSpotService.deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SPOT_IN_USE");
		verify(saveSpotPort, never()).deleteSpot(anyLong());
	}

	@Test
	@DisplayName("장소 삭제는 다른 장소가 남은 기본 장소면 DEFAULT_SPOT_REQUIRED로 거절한다")
	void deleteSpotRejectsWithDefaultSpotRequiredWhenDefaultSpotHasSiblings() {
		PickupSpot target = spot(SPOT_ID, FAMILY_ID, "정문 앞", true);
		PickupSpot sibling = spot(OTHER_SPOT_ID, FAMILY_ID, "후문", false);
		givenSpot(target);
		given(checkActivePickupPort.isUsedByActiveSession(SPOT_ID)).willReturn(false);
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of(target, sibling));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> deletePickupSpotService.deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("DEFAULT_SPOT_REQUIRED");
		verify(saveSpotPort, never()).deleteSpot(anyLong());
	}

	@Test
	@DisplayName("다른 가족의 spotId를 삭제하려 하면 NOT_FOUND로 거절한다")
	void deleteSpotRejectsWithNotFoundWhenSpotBelongsToOtherFamily() {
		givenSpot(spot(SPOT_ID, OTHER_FAMILY_ID, "후문", false));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> deletePickupSpotService.deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID)));

		assertNotFound(exception);
		verify(saveSpotPort, never()).deleteSpot(anyLong());
	}

	@Test
	@DisplayName("없는 spotId를 삭제하려 하면 NOT_FOUND로 거절한다")
	void deleteSpotRejectsWithNotFoundWhenSpotDoesNotExist() {
		given(loadSpotPort.loadSpot(SPOT_ID)).willReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> deletePickupSpotService.deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID)));

		assertNotFound(exception);
		verify(saveSpotPort, never()).deleteSpot(anyLong());
	}

	private static void assertNotFound(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
	}

	private void givenSpot(PickupSpot spot) {
		given(loadSpotPort.loadSpot(spot.getSpotId())).willReturn(Optional.of(spot));
	}

	private static PickupSpot spot(long spotId, long familyId, String name, boolean isDefault) {
		return PickupSpot.restore(spotId, familyId, ACADEMY_ID, name, null, NEAR_STOP, NEAR_MEETING, isDefault,
				GUARDIAN_ID);
	}
}
