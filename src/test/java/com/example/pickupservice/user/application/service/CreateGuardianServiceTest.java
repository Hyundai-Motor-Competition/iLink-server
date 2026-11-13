package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.common.image.StorageUnavailableException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CreateGuardianCommand;
import com.example.pickupservice.user.application.port.in.GuardianAccount;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.JoinCallMode;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.TestImages.PNG_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

	@Mock
	private ProfileImageStoragePort profileImageStoragePort;

	@InjectMocks
	private CreateGuardianService createGuardianService;

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

	static Stream<Arguments> alreadyLinkedOrUnlinkedVehicles() {
		return Stream.of(
				Arguments.of("다른 가족에 연결된 차량", Vehicle.restore(VEHICLE_ID, 5L, VIN, MODEL, null, null)),
				Arguments.of("연결이 해제된 차량",
						Vehicle.restore(VEHICLE_ID, null, VIN, MODEL, null, Instant.parse("2026-11-01T00:00:00Z"))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("alreadyLinkedOrUnlinkedVehicles")
	@DisplayName("createGuardian은 이미 가족에 연결됐거나 연결이 해제된 차량이면 409 VEHICLE_ALREADY_LINKED로 거절하고 아무것도 저장하지 않는다")
	void rejectsAlreadyLinkedOrUnlinkedVehicle(String caseName, Vehicle vehicle) {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(vehicle));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> createGuardianService.createGuardian(command));

		assertThat(exception).as(caseName).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("VEHICLE_ALREADY_LINKED");
		verify(saveFamilyPort, never()).saveFamily(any());
		verify(saveUserPort, never()).saveUser(any());
		verify(saveVehiclePort, never()).saveVehicle(any());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("alreadyLinkedOrUnlinkedVehicles")
	@DisplayName("createGuardian의 VEHICLE_ALREADY_LINKED는 common ErrorCode.VEHICLE_ALREADY_LINKED와 같은 409 「이미 보호자가 연결된 차량입니다」다")
	void vehicleAlreadyLinkedUsesCommonErrorCode(String caseName, Vehicle vehicle) {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(vehicle));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> createGuardianService.createGuardian(command));

		assertThat(exception).as(caseName).isNotNull();
		assertThat(exception.status()).isEqualTo(ErrorCode.VEHICLE_ALREADY_LINKED.getStatus());
		assertThat(exception.code()).isEqualTo(ErrorCode.VEHICLE_ALREADY_LINKED.name());
		assertThat(exception.getMessage()).isEqualTo("이미 보호자가 연결된 차량입니다");
	}

	@Test
	@DisplayName("보호자 생성(LINK-04 NEW)은 사진이 있으면 차량 검증 뒤 저장소에 올리고 업로드한 키를 보호자 User에 저장한다")
	void uploadsProfileImageAndStoresKeyOnGuardian() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(unownedVehicle()));
		given(saveFamilyPort.saveFamily(any())).willAnswer(invocation -> {
			Family family = invocation.getArgument(0);
			return Family.restore(FAMILY_ID, family.getJoinCallMode(), family.getSettingsVersion());
		});
		given(saveUserPort.saveUser(any())).willAnswer(invocation -> {
			User user = invocation.getArgument(0);
			return User.restore(USER_ID, user.getFamilyId(), user.getRole(), user.getName(), user.getPhone(),
					user.getProfileImageKey());
		});
		lenient().when(saveVehiclePort.saveVehicle(any())).thenAnswer(invocation -> invocation.getArgument(0));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME)
				.withProfileImage(ProfileImage.of(PNG_BYTES));

		GuardianAccount account = createGuardianService.createGuardian(command);

		assertThat(account).isEqualTo(new GuardianAccount(FAMILY_ID, USER_ID));
		InOrder inOrder = inOrder(loadVehiclePort, profileImageStoragePort, saveUserPort);
		inOrder.verify(loadVehiclePort).loadVehicle(VEHICLE_ID);
		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
		inOrder.verify(profileImageStoragePort).put(key.capture(), bytes.capture(), eq("image/png"));
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		inOrder.verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(key.getValue()).matches(
				"profiles/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
		assertThat(bytes.getValue()).isEqualTo(PNG_BYTES);
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.GUARDIAN);
		assertThat(savedUser.getValue().getProfileImageKey()).isEqualTo(key.getValue());
	}

	@Test
	@DisplayName("보호자 생성은 차량이 이미 연결돼 거절되면 사진이 있어도 저장소에 아무것도 올리지 않는다")
	void uploadsNothingWhenVehicleAlreadyLinked() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(
				Optional.of(Vehicle.restore(VEHICLE_ID, 5L, VIN, MODEL, null, null)));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME)
				.withProfileImage(ProfileImage.of(PNG_BYTES));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> createGuardianService.createGuardian(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("VEHICLE_ALREADY_LINKED");
		verify(profileImageStoragePort, never()).put(anyString(), any(), anyString());
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("보호자 생성은 사진이 없으면 저장소를 부르지 않고 profileImageKey를 비워 둔다")
	void leavesProfileImageKeyEmptyWhenNoImage() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(unownedVehicle()));
		given(saveFamilyPort.saveFamily(any())).willReturn(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0));
		given(saveUserPort.saveUser(any())).willAnswer(invocation -> {
			User user = invocation.getArgument(0);
			return User.restore(USER_ID, user.getFamilyId(), user.getRole(), user.getName(), user.getPhone());
		});
		lenient().when(saveVehiclePort.saveVehicle(any())).thenAnswer(invocation -> invocation.getArgument(0));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME).withProfileImage(null);

		createGuardianService.createGuardian(command);

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getProfileImageKey()).isNull();
		verifyNoInteractions(profileImageStoragePort);
	}

	@Test
	@DisplayName("저장소 설정이 없을 때 사진이 있는 보호자 생성 요청은 StorageUnavailableException으로 끝나고 보호자를 만들지 않는다")
	void failsWithStorageUnavailableWhenStorageIsNotConfiguredAndImageIsGiven() {
		InMemoryProfileImageStorage storage = new InMemoryProfileImageStorage();
		storage.unconfigure();
		CreateGuardianService service = new CreateGuardianService(
				saveFamilyPort, saveUserPort, loadVehiclePort, saveVehiclePort, storage);
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(unownedVehicle()));
		lenient().when(saveFamilyPort.saveFamily(any())).thenReturn(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME)
				.withProfileImage(ProfileImage.of(PNG_BYTES));

		Throwable thrown = catchThrowable(() -> service.createGuardian(command));

		assertThat(thrown).isInstanceOf(StorageUnavailableException.class);
		assertThat(storage.storedKeys()).isEmpty();
		verify(saveUserPort, never()).saveUser(any());
		verify(saveVehiclePort, never()).saveVehicle(any());
	}

	@Test
	@DisplayName("저장소 설정이 없어도 사진이 없는 보호자 생성은 지금처럼 성공한다")
	void createsGuardianWithoutImageWhenStorageIsNotConfigured() {
		InMemoryProfileImageStorage storage = new InMemoryProfileImageStorage();
		storage.unconfigure();
		CreateGuardianService service = new CreateGuardianService(
				saveFamilyPort, saveUserPort, loadVehiclePort, saveVehiclePort, storage);
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(unownedVehicle()));
		given(saveFamilyPort.saveFamily(any())).willReturn(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0));
		given(saveUserPort.saveUser(any())).willAnswer(invocation -> {
			User user = invocation.getArgument(0);
			return User.restore(USER_ID, user.getFamilyId(), user.getRole(), user.getName(), user.getPhone());
		});
		lenient().when(saveVehiclePort.saveVehicle(any())).thenAnswer(invocation -> invocation.getArgument(0));
		CreateGuardianCommand command = new CreateGuardianCommand(VEHICLE_ID, PHONE, NAME);

		GuardianAccount account = service.createGuardian(command);

		assertThat(account).isEqualTo(new GuardianAccount(FAMILY_ID, USER_ID));
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getProfileImageKey()).isNull();
		assertThat(storage.storedKeys()).isEmpty();
	}
}
