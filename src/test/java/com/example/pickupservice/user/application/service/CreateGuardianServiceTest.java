package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CreateGuardianCommand;
import com.example.pickupservice.user.application.port.in.GuardianAccount;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.JoinCallMode;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateGuardianServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long USER_ID = 101L;
	private static final long VEHICLE_ID = 77L;
	private static final String VIN = "KMHKM81AFPU123456";
	private static final String MODEL = "아이오닉 5";
	private static final String NAME = "김보호";
	private static final String PHONE = "+821012345678";

	@Mock
	private SaveFamilyPort saveFamilyPort;

	@Mock
	private SaveUserPort saveUserPort;

	@Mock
	private LoadVehiclePort loadVehiclePort;

	@Mock
	private SaveVehiclePort saveVehiclePort;

	private CreateGuardianService createGuardianService;

	@BeforeEach
	void setUp() {
		createGuardianService = new CreateGuardianService(saveFamilyPort, saveUserPort, loadVehiclePort,
				saveVehiclePort);
	}

	private static Vehicle unownedVehicle() {
		return Vehicle.restore(VEHICLE_ID, null, VIN, MODEL, null, null);
	}

	@Test
	@DisplayName("createGuardian은 Family와 role GUARDIAN인 User를 만들고 Vehicle.familyId를 그 가족으로 채운다")
	void createsFamilyAndGuardianAndLinksVehicle() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(unownedVehicle()));
		given(saveFamilyPort.saveFamily(any())).willAnswer(invocation -> {
			Family family = invocation.getArgument(0);
			return Family.restore(FAMILY_ID, family.getJoinCallMode(), family.getSettingsVersion());
		});
		given(saveUserPort.saveUser(any())).willAnswer(invocation -> {
			User user = invocation.getArgument(0);
			return User.restore(USER_ID, user.getFamilyId(), user.getRole(), user.getName(), user.getPhone());
		});
		lenient().when(saveVehiclePort.saveVehicle(any())).thenAnswer(invocation -> invocation.getArgument(0));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME);

		GuardianAccount account = createGuardianService.createGuardian(command);

		assertThat(account).isEqualTo(new GuardianAccount(FAMILY_ID, USER_ID));

		ArgumentCaptor<Family> savedFamily = ArgumentCaptor.forClass(Family.class);
		verify(saveFamilyPort, times(1)).saveFamily(savedFamily.capture());
		assertThat(savedFamily.getValue().getFamilyId()).isNull();
		assertThat(savedFamily.getValue().getJoinCallMode()).isEqualTo(JoinCallMode.AUTO);
		assertThat(savedFamily.getValue().getSettingsVersion()).isZero();

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort, times(1)).saveUser(savedUser.capture());
		User user = savedUser.getValue();
		assertThat(user.getUserId()).isNull();
		assertThat(user.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(user.getRole()).isEqualTo(Role.GUARDIAN);
		assertThat(user.getName()).isEqualTo(NAME);
		assertThat(user.getPhone()).isEqualTo(PHONE);

		ArgumentCaptor<Vehicle> savedVehicle = ArgumentCaptor.forClass(Vehicle.class);
		verify(saveVehiclePort, times(1)).saveVehicle(savedVehicle.capture());
		Vehicle vehicle = savedVehicle.getValue();
		assertThat(vehicle.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(vehicle.getVehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(vehicle.getVin()).isEqualTo(VIN);
		assertThat(vehicle.getModel()).isEqualTo(MODEL);
		assertThat(vehicle.getUnlinkedAt()).isNull();
	}

	@Test
	@DisplayName("createGuardian은 차량이 없으면 NOT_FOUND로 거절하고 가족 · 사용자를 만들지 않는다")
	void rejectsWhenVehicleMissing() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.empty());
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> createGuardianService.createGuardian(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveFamilyPort, never()).saveFamily(any());
		verify(saveUserPort, never()).saveUser(any());
		verify(saveVehiclePort, never()).saveVehicle(any());
	}
}
