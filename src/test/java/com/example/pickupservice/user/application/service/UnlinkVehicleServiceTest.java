package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.UnlinkVehicleCommand;
import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.RevokeVehicleDevicesPort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnlinkVehicleServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long OTHER_FAMILY_ID = 99L;
	private static final long VEHICLE_ID = 77L;
	private static final String VIN = "KMHKM81AFPU123456";
	private static final String MODEL = "아이오닉 5";
	private static final String PLATE_NUMBER = "12가 3456";
	private static final Instant NOW = Instant.parse("2026-11-07T09:30:00Z");

	@Mock
	private LoadVehiclePort loadVehiclePort;

	@Mock
	private SaveVehiclePort saveVehiclePort;

	@Mock
	private CheckActivePickupPort checkActivePickupPort;

	@Mock
	private RevokeVehicleDevicesPort revokeVehicleDevicesPort;

	private UnlinkVehicleService unlinkVehicleService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		unlinkVehicleService = new UnlinkVehicleService(loadVehiclePort, saveVehiclePort, checkActivePickupPort,
				revokeVehicleDevicesPort, clock);
	}

	private static Vehicle linkedVehicleOf(long familyId) {
		return Vehicle.restore(VEHICLE_ID, familyId, VIN, MODEL, PLATE_NUMBER, null);
	}

	@Test
	@DisplayName("차량 연결 해제는 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다")
	void rejectsUnlinkWhenActivePickupExists() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(linkedVehicleOf(FAMILY_ID)));
		given(checkActivePickupPort.hasActivePickup(VEHICLE_ID)).willReturn(true);
		UnlinkVehicleCommand command = new UnlinkVehicleCommand(FAMILY_ID, VEHICLE_ID);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> unlinkVehicleService.unlinkVehicle(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("ACTIVE_PICKUP_EXISTS");
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		verify(saveVehiclePort, never()).saveVehicle(any());
		verify(revokeVehicleDevicesPort, never()).revokeDevicesOfVehicle(anyLong());
	}

	@Test
	@DisplayName("LINK-05로 차량 연결을 해제하면 그 차량의 살아 있는 기기를 모두 폐기하도록 vehicleId로 요청한다")
	void revokesLiveDevicesOfVehicleOnUnlink() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(linkedVehicleOf(FAMILY_ID)));
		given(checkActivePickupPort.hasActivePickup(VEHICLE_ID)).willReturn(false);
		lenient().when(saveVehiclePort.saveVehicle(any())).thenAnswer(invocation -> invocation.getArgument(0));
		UnlinkVehicleCommand command = new UnlinkVehicleCommand(FAMILY_ID, VEHICLE_ID);

		unlinkVehicleService.unlinkVehicle(command);

		verify(revokeVehicleDevicesPort, times(1)).revokeDevicesOfVehicle(VEHICLE_ID);
		verify(saveVehiclePort, times(1)).saveVehicle(any());
	}

	@Test
	@DisplayName("차량 연결 해제가 성공하면 unlinkedAt만 채우고 행은 남긴다")
	void fillsOnlyUnlinkedAtAndKeepsRow() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(linkedVehicleOf(FAMILY_ID)));
		given(checkActivePickupPort.hasActivePickup(VEHICLE_ID)).willReturn(false);
		lenient().when(saveVehiclePort.saveVehicle(any())).thenAnswer(invocation -> invocation.getArgument(0));
		UnlinkVehicleCommand command = new UnlinkVehicleCommand(FAMILY_ID, VEHICLE_ID);

		unlinkVehicleService.unlinkVehicle(command);

		ArgumentCaptor<Vehicle> saved = ArgumentCaptor.forClass(Vehicle.class);
		verify(saveVehiclePort, times(1)).saveVehicle(saved.capture());
		Vehicle vehicle = saved.getValue();
		assertThat(vehicle.getUnlinkedAt()).isEqualTo(NOW);
		assertThat(vehicle.isLinked()).isFalse();
		assertThat(vehicle.getVehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(vehicle.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(vehicle.getVin()).isEqualTo(VIN);
		assertThat(vehicle.getModel()).isEqualTo(MODEL);
		assertThat(vehicle.getPlateNumber()).isEqualTo(PLATE_NUMBER);
	}

	@Test
	@DisplayName("차량 연결 해제는 차량이 없으면 NOT_FOUND로 거절한다")
	void rejectsUnlinkOfMissingVehicle() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.empty());
		lenient().when(checkActivePickupPort.hasActivePickup(anyLong())).thenReturn(false);
		UnlinkVehicleCommand command = new UnlinkVehicleCommand(FAMILY_ID, VEHICLE_ID);

		assertNotFound(command);
	}

	@Test
	@DisplayName("차량 연결 해제는 다른 가족의 차량이면 NOT_FOUND로 거절한다")
	void rejectsUnlinkOfOtherFamilyVehicle() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(linkedVehicleOf(OTHER_FAMILY_ID)));
		lenient().when(checkActivePickupPort.hasActivePickup(anyLong())).thenReturn(false);
		UnlinkVehicleCommand command = new UnlinkVehicleCommand(FAMILY_ID, VEHICLE_ID);

		assertNotFound(command);
	}

	@Test
	@DisplayName("차량 연결 해제는 이미 해제된 차량이면 NOT_FOUND로 거절한다")
	void rejectsUnlinkOfAlreadyUnlinkedVehicle() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(
				Vehicle.restore(VEHICLE_ID, FAMILY_ID, VIN, MODEL, PLATE_NUMBER,
						Instant.parse("2026-11-01T00:00:00Z"))));
		lenient().when(checkActivePickupPort.hasActivePickup(anyLong())).thenReturn(false);
		UnlinkVehicleCommand command = new UnlinkVehicleCommand(FAMILY_ID, VEHICLE_ID);

		assertNotFound(command);
	}

	private void assertNotFound(UnlinkVehicleCommand command) {
		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> unlinkVehicleService.unlinkVehicle(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveVehiclePort, never()).saveVehicle(any());
		verify(revokeVehicleDevicesPort, never()).revokeDevicesOfVehicle(anyLong());
	}
}
