package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.common.image.StorageUnavailableException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.RegisterMemberCommand;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.TestImages.PNG_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RegisterMemberServiceTest {

	private static final long FAMILY_ID = 12L;

	@Mock
	private LoadUserPort loadUserPort;

	@Mock
	private SaveUserPort saveUserPort;

	@Mock
	private ProfileImageStoragePort profileImageStoragePort;

	@InjectMocks
	private RegisterMemberService registerMemberService;

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CO_GUARDIAN", "DELEGATE", "TEACHER"})
	@DisplayName("멤버 등록은 자녀 외 멤버가 이미 5명이면 MEMBER_LIMIT_EXCEEDED로 거절한다")
	void rejectsNonChildMemberWhenFiveNonChildMembersAlreadyExist(Role role) {
		given(loadUserPort.countNonChildMembers(FAMILY_ID)).willReturn(5L);
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, role, "이선생", "+821098765432");

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("MEMBER_LIMIT_EXCEEDED");
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("멤버 등록은 자녀 외 멤버가 4명이면 자녀 외 멤버를 등록하고 userId를 돌려준다")
	void registersNonChildMemberWhenFourNonChildMembersExist() {
		given(loadUserPort.countNonChildMembers(FAMILY_ID)).willReturn(4L);
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(140L, FAMILY_ID, Role.TEACHER, "이선생", "+821098765432"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.TEACHER, "이선생", "+821098765432");

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(140L);
		verify(saveUserPort).saveUser(any());
	}

	@Test
	@DisplayName("멤버 등록은 자녀 외 멤버가 5명이어도 CHILD는 등록한다")
	void registersChildEvenWhenFiveNonChildMembersAlreadyExist() {
		lenient().when(loadUserPort.countNonChildMembers(FAMILY_ID)).thenReturn(5L);
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(131L, FAMILY_ID, Role.CHILD, "민준", null));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null);

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(131L);
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getUserId()).isNull();
		assertThat(savedUser.getValue().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.CHILD);
		assertThat(savedUser.getValue().getName()).isEqualTo("민준");
		assertThat(savedUser.getValue().getPhone()).isNull();
	}

	@Test
	@DisplayName("CO_GUARDIAN이 CO_GUARDIAN을 등록하려 하면 FORBIDDEN으로 거절한다")
	void rejectsCoGuardianRegisteringCoGuardian() {
		lenient().when(saveUserPort.saveUser(any())).thenReturn(
				User.restore(160L, FAMILY_ID, Role.CO_GUARDIAN, "박공동", "+821055556666"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.CO_GUARDIAN, Role.CO_GUARDIAN, "박공동", "+821055556666");

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.FORBIDDEN.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
		verify(saveUserPort, never()).saveUser(any());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"DELEGATE", "TEACHER", "CHILD"})
	@DisplayName("CO_GUARDIAN은 DELEGATE · TEACHER · CHILD를 등록할 수 있다")
	void coGuardianRegistersDelegateTeacherAndChild(Role role) {
		String phone = role == Role.CHILD ? null : "+821077778888";
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(150L, FAMILY_ID, role, "최멤버", phone));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.CO_GUARDIAN, role, "최멤버", phone);

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(150L);
		verify(saveUserPort).saveUser(any());
	}

	@Test
	@DisplayName("GUARDIAN은 CO_GUARDIAN을 등록할 수 있다")
	void guardianRegistersCoGuardian() {
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(160L, FAMILY_ID, Role.CO_GUARDIAN, "박공동", "+821055556666"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CO_GUARDIAN, "박공동", "+821055556666");

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(160L);
		verify(saveUserPort).saveUser(any());
	}

	static Stream<Arguments> nonChildRolesWithoutPhone() {
		return Stream.of(Role.CO_GUARDIAN, Role.DELEGATE, Role.TEACHER)
				.flatMap(role -> Stream.of(Arguments.of(role, null), Arguments.of(role, "")));
	}

	@ParameterizedTest
	@MethodSource("nonChildRolesWithoutPhone")
	@DisplayName("CHILD가 아닌 멤버를 phone 없이 등록하려 하면 거절한다")
	void rejectsNonChildMemberWithoutPhone(Role role, String phone) {
		lenient().when(saveUserPort.saveUser(any())).thenReturn(
				User.restore(170L, FAMILY_ID, role, "정멤버", phone));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, role, "정멤버", phone);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).saveUser(any());
	}

	@ParameterizedTest
	@MethodSource("nonChildRolesWithoutPhone")
	@DisplayName("자녀 외 멤버를 phone 없이 등록하거나 GUARDIAN이 자기 role을 바꾸려 할 때의 VALIDATION_FAILED는 fields에 phone · role을 담는다")
	void putsPhoneInFieldsWhenNonChildMemberIsRegisteredWithoutPhone(Role role, String phone) {
		lenient().when(saveUserPort.saveUser(any())).thenReturn(
				User.restore(172L, FAMILY_ID, role, "정멤버", phone));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, role, "정멤버", phone);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.fields()).isNotNull();
		assertThat(exception.fields()).extracting(FieldError::field).containsExactly("phone");
		assertThat(exception.fields()).extracting(FieldError::reason).allSatisfy(
				reason -> assertThat(reason).isNotBlank());
		verify(saveUserPort, never()).saveUser(any());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CO_GUARDIAN", "DELEGATE", "TEACHER"})
	@DisplayName("CHILD가 아닌 멤버를 국제 형식이 아닌 phone으로 등록하려 하면 VALIDATION_FAILED로 거절한다")
	void rejectsNonChildMemberWithNonInternationalPhone(Role role) {
		lenient().when(saveUserPort.saveUser(any())).thenReturn(
				User.restore(171L, FAMILY_ID, role, "정멤버", "01012345678"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, role, "정멤버", "01012345678");

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("멤버 등록은 사진이 있으면 검증을 모두 통과한 뒤 저장소에 올리고 User.profileImageKey에 그 키를 저장한다")
	void uploadsProfileImageAfterValidationAndStoresKeyOnUser() {
		given(loadUserPort.countNonChildMembers(FAMILY_ID)).willReturn(4L);
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(180L, FAMILY_ID, Role.TEACHER, "이선생", "+821098765432"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.TEACHER, "이선생", "+821098765432")
				.withProfileImage(ProfileImage.of(PNG_BYTES));

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(180L);
		InOrder inOrder = inOrder(loadUserPort, profileImageStoragePort, saveUserPort);
		inOrder.verify(loadUserPort).countNonChildMembers(FAMILY_ID);
		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
		inOrder.verify(profileImageStoragePort).put(key.capture(), bytes.capture(), eq("image/png"));
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		inOrder.verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(key.getValue()).matches(
				"profiles/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
		assertThat(bytes.getValue()).isEqualTo(PNG_BYTES);
		assertThat(savedUser.getValue().getProfileImageKey()).isEqualTo(key.getValue());
	}

	static Stream<Arguments> commandsFailingValidationWithImage() {
		return Stream.of(
				Arguments.of("권한", 0L, new RegisterMemberCommand(
						FAMILY_ID, Role.CO_GUARDIAN, Role.CO_GUARDIAN, "박공동", "+821055556666"),
						ErrorCode.FORBIDDEN.name()),
				Arguments.of("멤버 한도", 5L, new RegisterMemberCommand(
						FAMILY_ID, Role.GUARDIAN, Role.TEACHER, "이선생", "+821098765432"),
						"MEMBER_LIMIT_EXCEEDED"),
				Arguments.of("phone 형식", 0L, new RegisterMemberCommand(
						FAMILY_ID, Role.GUARDIAN, Role.TEACHER, "이선생", "01012345678"),
						ErrorCode.VALIDATION_FAILED.name()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("commandsFailingValidationWithImage")
	@DisplayName("멤버 등록이 권한 · 멤버 한도 검증에서 실패하면 저장소에 아무것도 올리지 않는다")
	void uploadsNothingWhenValidationFails(String reason, long nonChildCount,
			RegisterMemberCommand commandWithoutImage, String expectedCode) {
		lenient().when(loadUserPort.countNonChildMembers(FAMILY_ID)).thenReturn(nonChildCount);
		RegisterMemberCommand command = commandWithoutImage.withProfileImage(ProfileImage.of(PNG_BYTES));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(expectedCode);
		verifyNoInteractions(profileImageStoragePort);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("멤버 등록은 저장소 업로드가 실패하면 StorageUnavailableException으로 끝나고 User를 만들지 않는다")
	void failsWithStorageUnavailableAndSavesNoUserWhenUploadFails() {
		willThrow(new StorageUnavailableException())
				.given(profileImageStoragePort).put(anyString(), any(), anyString());
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null)
				.withProfileImage(ProfileImage.of(PNG_BYTES));

		Throwable thrown = catchThrowable(() -> registerMemberService.registerMember(command));

		assertThat(thrown).isInstanceOf(StorageUnavailableException.class);
		verify(profileImageStoragePort).put(anyString(), any(), anyString());
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("멤버 등록은 사진이 없으면 저장소를 부르지 않고 profileImageKey를 비워 둔다")
	void leavesProfileImageKeyEmptyAndSkipsStorageWithoutImage() {
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(133L, FAMILY_ID, Role.CHILD, "하준", null));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "하준", null);

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(133L);
		verifyNoInteractions(profileImageStoragePort);
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getProfileImageKey()).isNull();
	}

	@Test
	@DisplayName("저장소 설정이 없을 때 사진이 있는 등록 요청은 StorageUnavailableException으로 끝난다")
	void failsWithStorageUnavailableWhenStorageIsNotConfiguredAndImageIsGiven() {
		InMemoryProfileImageStorage storage = new InMemoryProfileImageStorage();
		storage.unconfigure();
		RegisterMemberService service = new RegisterMemberService(loadUserPort, saveUserPort, storage);
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null)
				.withProfileImage(ProfileImage.of(PNG_BYTES));

		Throwable thrown = catchThrowable(() -> service.registerMember(command));

		assertThat(thrown).isInstanceOf(StorageUnavailableException.class);
		assertThat(storage.storedKeys()).isEmpty();
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("저장소 설정이 없어도 사진이 없는 멤버 등록은 지금처럼 성공한다")
	void registersMemberWithoutImageWhenStorageIsNotConfigured() {
		InMemoryProfileImageStorage storage = new InMemoryProfileImageStorage();
		storage.unconfigure();
		RegisterMemberService service = new RegisterMemberService(loadUserPort, saveUserPort, storage);
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(134L, FAMILY_ID, Role.CHILD, "민준", null));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null);

		long userId = service.registerMember(command);

		assertThat(userId).isEqualTo(134L);
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getProfileImageKey()).isNull();
		assertThat(storage.storedKeys()).isEmpty();
	}

	@Test
	@DisplayName("CHILD는 phone 없이 등록된다")
	void registersChildWithoutPhone() {
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(132L, FAMILY_ID, Role.CHILD, "서윤", null));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "서윤", null);

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(132L);
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.CHILD);
		assertThat(savedUser.getValue().getName()).isEqualTo("서윤");
		assertThat(savedUser.getValue().getPhone()).isNull();
	}
}
