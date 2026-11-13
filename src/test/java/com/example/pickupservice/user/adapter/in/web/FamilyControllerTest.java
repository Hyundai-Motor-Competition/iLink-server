package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.image.ImageFormat;
import com.example.pickupservice.common.image.StorageUnavailableException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CallModeResult;
import com.example.pickupservice.user.application.port.in.ChildLocationResult;
import com.example.pickupservice.user.application.port.in.ChangeCallModeCommand;
import com.example.pickupservice.user.application.port.in.ChangeChildLocationCommand;
import com.example.pickupservice.user.application.port.in.FamilyResult;
import com.example.pickupservice.user.application.port.in.GetMyFamilyQuery;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.in.RegisterMemberCommand;
import com.example.pickupservice.user.application.port.in.RemoveMemberCommand;
import com.example.pickupservice.user.application.port.in.UpdateMemberCommand;
import com.example.pickupservice.user.application.port.in.VehicleSummary;
import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.JoinCallMode;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class FamilyControllerTest extends FamilyWebTestSupport {

	private static final String MY_FAMILY_PATH = "/v1/families/me";
	private static final String MEMBERS_PATH = "/v1/families/me/members";
	private static final String CALL_MODE_PATH = "/v1/families/me/call-mode";
	private static final String CHILD_LOCATION_PATH = "/v1/families/me/child-location";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long TEACHER_USER_ID = 141L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long CHILD_USER_ID = 131L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final long VEHICLE_ID = 77L;
	private static final String GUARDIAN_ROLE_BODY = """
			{"role":"GUARDIAN","name":"이보호","phone":"+821011112222"}
			""";

	private static final String CHILD_MEMBER_BODY = """
			{"role":"CHILD","name":"민준","phone":null}
			""";
	private static final String REQUEST_PART = "request";
	private static final String PROFILE_IMAGE_PART = "profileImage";
	private static final byte[] PNG_BYTES = {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52};

	private static final String GUARDIAN_PROFILE_URL =
			"https://img-dev.chisanw.com/ilink/profiles/3f6c.webp?X-Amz-Expires=600&X-Amz-Signature=abc";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("GET /v1/families/me는 200과 familyId · joinCallMode · vehicle · members[]를 돌려준다")
	void getMyFamilyReturns200WithFamilyVehicleAndMembers() throws Exception {
		given(getMyFamilyUseCase.getMyFamily(any())).willReturn(familyResult(linkedVehicle()));

		mockMvc.perform(get(MY_FAMILY_PATH).header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"familyId":12,
							 "childLocationEnabled":false,
						 "joinCallMode":"AUTO",
						 "vehicle":{"vehicleId":77,"model":"아이오닉 5","plateNumberMasked":"12가 ****"},
						 "members":[
						   {"userId":101,"role":"GUARDIAN","name":"김보호","phone":"+821012345678","deviceStatus":"CONNECTED","profileImageUrl":null},
						   {"userId":131,"role":"CHILD","name":"민준","phone":null,"deviceStatus":"INVITE_PENDING","profileImageUrl":null}
						 ]}
						""", JsonCompareMode.STRICT))
				.andExpect(jsonPath("$.members[1]", hasKey("phone")))
				.andExpect(jsonPath("$.members[1].phone").value(nullValue()));
	}

	@Test
	@DisplayName("GET /v1/families/me는 토큰의 familyId(12)를 GetMyFamilyQuery.familyId로 유스케이스에 넘긴다")
	void getMyFamilyPassesTokenFamilyIdToUseCase() throws Exception {
		given(getMyFamilyUseCase.getMyFamily(any())).willReturn(familyResult(linkedVehicle()));

		mockMvc.perform(get(MY_FAMILY_PATH).header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(status().isOk());

		then(getMyFamilyUseCase).should().getMyFamily(new GetMyFamilyQuery(FAMILY_ID));
	}

	@Test
	@DisplayName("GET /v1/families/me는 연결 중인 차량이 없으면 본문에 \"vehicle\":null을 담아 돌려준다")
	void getMyFamilyWritesVehicleNullWhenNoLinkedVehicle() throws Exception {
		given(getMyFamilyUseCase.getMyFamily(any())).willReturn(familyResult(null));

		mockMvc.perform(get(MY_FAMILY_PATH).header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasKey("vehicle")))
				.andExpect(jsonPath("$.vehicle").value(nullValue()))
				.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
				.andExpect(jsonPath("$.members.length()").value(2));
	}

	@Test
	@DisplayName("GET /v1/families/me는 members[]마다 profileImageUrl을 돌려준다")
	void getMyFamilyReturnsProfileImageUrlForEachMember() throws Exception {
		given(getMyFamilyUseCase.getMyFamily(any())).willReturn(new FamilyResult(FAMILY_ID, JoinCallMode.AUTO, null, List.of(
				new MemberResult(GUARDIAN_USER_ID, Role.GUARDIAN, "김보호", "+821012345678", DeviceStatus.CONNECTED, GUARDIAN_PROFILE_URL),
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.INVITE_PENDING, null))));

		mockMvc.perform(get(MY_FAMILY_PATH).header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.members[0].profileImageUrl").value(GUARDIAN_PROFILE_URL))
				.andExpect(jsonPath("$.members[1]", hasKey("profileImageUrl")))
				.andExpect(jsonPath("$.members[1].profileImageUrl").value(nullValue()));
	}

	@Test
	@DisplayName("GET /v1/families/me를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void getMyFamilyWithTeacherTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(get(MY_FAMILY_PATH)
						.header(HttpHeaders.AUTHORIZATION, userBearer(TEACHER_USER_ID, Role.TEACHER)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getMyFamilyUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/families/me는 유스케이스의 childLocationEnabled가 true이면 응답에 childLocationEnabled true를 담는다")
	void getMyFamilyWritesChildLocationEnabledTrueWhenUseCaseReturnsTrue() throws Exception {
		given(getMyFamilyUseCase.getMyFamily(any())).willReturn(familyResultWithChildLocation(true));

		mockMvc.perform(get(MY_FAMILY_PATH).header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasKey("childLocationEnabled")))
				.andExpect(jsonPath("$.childLocationEnabled").value(true));
	}

	@Test
	@DisplayName("GET /v1/families/me는 유스케이스의 childLocationEnabled가 false이면 응답에 childLocationEnabled false를 담는다")
	void getMyFamilyWritesChildLocationEnabledFalseWhenUseCaseReturnsFalse() throws Exception {
		given(getMyFamilyUseCase.getMyFamily(any())).willReturn(familyResultWithChildLocation(false));

		mockMvc.perform(get(MY_FAMILY_PATH).header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasKey("childLocationEnabled")))
				.andExpect(jsonPath("$.childLocationEnabled").value(false));
	}

	@Test
	@DisplayName("POST /v1/families/me/members는 201과 userId를 돌려준다")
	void registerMemberReturns201WithUserId() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		mockMvc.perform(registerMemberRequest("""
						{"role":"CHILD","name":"민준","phone":null}
						"""))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"userId":131}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("POST /v1/families/me/members는 토큰의 familyId · role과 요청 본문을 RegisterMemberCommand로 유스케이스에 넘긴다")
	void registerMemberPassesTokenAndBodyToUseCase() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		mockMvc.perform(registerMemberRequest("""
						{"role":"CHILD","name":"민준","phone":null}
						"""))
				.andExpect(status().isCreated());

		then(registerMemberUseCase).should().registerMember(
				new RegisterMemberCommand(FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null));
	}

	@Test
	@DisplayName("POST /v1/families/me/members에 21자 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다")
	void registerMemberWith21CharNameReturns422WithNameField() throws Exception {
		String name21 = "가".repeat(21);

		mockMvc.perform(registerMemberRequest("""
						{"role":"CHILD","name":"%s","phone":null}
						""".formatted(name21)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("name")));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members에 role GUARDIAN을 보내면 422 VALIDATION_FAILED와 fields의 role을 돌려준다")
	void registerMemberWithGuardianRoleReturns422WithRoleField() throws Exception {
		mockMvc.perform(registerMemberRequest(guardianBearer(), GUARDIAN_ROLE_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("role")));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members에 CO_GUARDIAN 토큰으로 role GUARDIAN을 보내면 422 VALIDATION_FAILED와 fields의 role을 돌려주고 유스케이스를 부르지 않는다")
	void registerMemberWithGuardianRoleByCoGuardianReturns422WithRoleField() throws Exception {
		mockMvc.perform(registerMemberRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN), GUARDIAN_ROLE_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("role")));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CHILD", "CO_GUARDIAN", "DELEGATE", "TEACHER"})
	@DisplayName("POST /v1/families/me/members는 role CHILD · CO_GUARDIAN · DELEGATE · TEACHER를 받아 201을 돌려준다")
	void registerMemberAcceptsAllowedRoles(Role role) throws Exception {
		given(registerMemberUseCase.registerMember(any())).willReturn(TEACHER_USER_ID);

		mockMvc.perform(registerMemberRequest(guardianBearer(), """
						{"role":"%s","name":"이멤버","phone":"+821011112222"}
						""".formatted(role.name())))
				.andExpect(status().isCreated());

		then(registerMemberUseCase).should().registerMember(
				new RegisterMemberCommand(FAMILY_ID, Role.GUARDIAN, role, "이멤버", "+821011112222"));
	}

	@Test
	@DisplayName("POST /v1/families/me/members가 한도를 넘으면 422 MEMBER_LIMIT_EXCEEDED를 돌려준다")
	void registerMemberOverLimitReturns422MemberLimitExceeded() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willThrow(BusinessException.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "MEMBER_LIMIT_EXCEEDED", "자녀 외 멤버는 5명까지 등록할 수 있습니다"));

		mockMvc.perform(registerMemberRequest("""
						{"role":"TEACHER","name":"이선생","phone":"+821098765432"}
						"""))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "MEMBER_LIMIT_EXCEEDED"));
	}

	@Test
	@DisplayName("POST /v1/families/me/members는 multipart의 request 파트와 profileImage 파트를 받아 201과 userId를 돌려준다")
	void registerMemberMultipartWithProfileImageReturns201AndPassesImageToUseCase() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		mockMvc.perform(registerMemberMultipartRequest(CHILD_MEMBER_BODY,
						new MockMultipartFile(PROFILE_IMAGE_PART, "child.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES)))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"userId":131}
						""", JsonCompareMode.STRICT));

		ArgumentCaptor<RegisterMemberCommand> captor = ArgumentCaptor.forClass(RegisterMemberCommand.class);
		then(registerMemberUseCase).should().registerMember(captor.capture());
		RegisterMemberCommand command = captor.getValue();
		assertThat(command.familyId()).isEqualTo(FAMILY_ID);
		assertThat(command.requesterRole()).isEqualTo(Role.GUARDIAN);
		assertThat(command.role()).isEqualTo(Role.CHILD);
		assertThat(command.name()).isEqualTo("민준");
		assertThat(command.phone()).isNull();
		assertThat(command.profileImage()).isNotNull();
		assertThat(command.profileImage().format()).isEqualTo(ImageFormat.PNG);
		assertThat(command.profileImage().bytes()).isEqualTo(PNG_BYTES);
	}

	@Test
	@DisplayName("POST /v1/families/me/members는 profileImage 파트 없이 multipart로 보내도 201을 돌려준다")
	void registerMemberMultipartWithoutProfileImageReturns201() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		mockMvc.perform(registerMemberMultipartRequest(CHILD_MEMBER_BODY))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"userId":131}
						""", JsonCompareMode.STRICT));

		then(registerMemberUseCase).should().registerMember(
				new RegisterMemberCommand(FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null));
	}

	@Test
	@DisplayName("POST /v1/families/me/members는 0바이트 profileImage 파트를 사진이 없는 것으로 보고 201을 돌려준다")
	void registerMemberMultipartWithEmptyProfileImageTreatsAsNoImage() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		mockMvc.perform(registerMemberMultipartRequest(CHILD_MEMBER_BODY,
						new MockMultipartFile(PROFILE_IMAGE_PART, "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0])))
				.andExpect(status().isCreated());

		then(registerMemberUseCase).should().registerMember(
				new RegisterMemberCommand(FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null));
	}

	@Test
	@DisplayName("POST /v1/families/me/members는 지금처럼 application/json 본문도 받아 201을 돌려준다")
	void registerMemberStillAcceptsJsonBodyReturns201() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		mockMvc.perform(registerMemberRequest(CHILD_MEMBER_BODY))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"userId":131}
						""", JsonCompareMode.STRICT));

		then(registerMemberUseCase).should().registerMember(
				new RegisterMemberCommand(FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null));
	}

	@Test
	@DisplayName("POST /v1/families/me/members의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다")
	void registerMemberMultipartWithNonImageReturns422ProfileImageInvalid() throws Exception {
		mockMvc.perform(registerMemberMultipartRequest(CHILD_MEMBER_BODY,
						new MockMultipartFile(PROFILE_IMAGE_PART, "fake.png", MediaType.IMAGE_PNG_VALUE,
								"not an image".getBytes(StandardCharsets.UTF_8))))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "PROFILE_IMAGE_INVALID"));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members의 profileImage가 5MB를 넘으면 413 PROFILE_IMAGE_TOO_LARGE를 돌려준다")
	void registerMemberMultipartWithImageOver5MbReturns413ProfileImageTooLarge() throws Exception {
		byte[] oversized = Arrays.copyOf(PNG_BYTES, 5_242_881);

		mockMvc.perform(registerMemberMultipartRequest(CHILD_MEMBER_BODY,
						new MockMultipartFile(PROFILE_IMAGE_PART, "big.png", MediaType.IMAGE_PNG_VALUE, oversized)))
				.andExpect(commonError(HttpStatus.CONTENT_TOO_LARGE, "PROFILE_IMAGE_TOO_LARGE"));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members는 저장소 업로드가 실패하면 503 STORAGE_UNAVAILABLE을 돌려준다")
	void registerMemberWhenStorageUploadFailsReturns503StorageUnavailable() throws Exception {
		given(registerMemberUseCase.registerMember(any())).willThrow(new StorageUnavailableException());

		mockMvc.perform(registerMemberMultipartRequest(CHILD_MEMBER_BODY,
						new MockMultipartFile(PROFILE_IMAGE_PART, "child.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES)))
				.andExpect(commonError(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE"));
	}

	@Test
	@DisplayName("POST /v1/families/me/members multipart에 request 파트가 없으면 400 INVALID_REQUEST를 돌려주고 유스케이스를 부르지 않는다")
	void registerMemberMultipartWithoutRequestPartReturns400InvalidRequest() throws Exception {
		mockMvc.perform(multipart(MEMBERS_PATH)
						.file(new MockMultipartFile(PROFILE_IMAGE_PART, "child.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES))
						.header(HttpHeaders.AUTHORIZATION, guardianBearer())
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(commonError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST"));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members multipart의 request 파트 JSON 문법이 틀리면 400 INVALID_REQUEST를 돌려주고 유스케이스를 부르지 않는다")
	void registerMemberMultipartWithMalformedRequestJsonReturns400InvalidRequest() throws Exception {
		mockMvc.perform(registerMemberMultipartRequest("{\"role\":\"CHILD\",\"name\":",
						new MockMultipartFile(PROFILE_IMAGE_PART, "child.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES)))
				.andExpect(commonError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST"));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members multipart의 request 파트 name이 비어 있으면 422 VALIDATION_FAILED와 fields의 name을 돌려주고 유스케이스를 부르지 않는다")
	void registerMemberMultipartWithBlankNameReturns422WithNameField() throws Exception {
		mockMvc.perform(registerMemberMultipartRequest("""
						{"role":"CHILD","name":"","phone":null}
						""",
						new MockMultipartFile(PROFILE_IMAGE_PART, "child.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("name")));

		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PATCH /v1/families/me/members/{userId}는 200과 userId · role · name · phone · deviceStatus를 돌려준다")
	void updateMemberReturns200WithMemberFields() throws Exception {
		given(updateMemberUseCase.updateMember(any())).willReturn(
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.CONNECTED, null));

		mockMvc.perform(updateMemberRequest(CHILD_USER_ID, """
						{"name":"민준"}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"userId":131,"role":"CHILD","name":"민준","phone":null,"deviceStatus":"CONNECTED"}
						""", JsonCompareMode.STRICT))
				.andExpect(jsonPath("$", hasKey("phone")))
				.andExpect(jsonPath("$.phone").value(nullValue()));
	}

	@Test
	@DisplayName("PATCH /v1/families/me/members/{userId}는 토큰의 familyId · userId · role과 경로의 userId, 보낸 필드만 UpdateMemberCommand로 넘기고 보내지 않은 필드는 null로 둔다")
	void updateMemberPassesTokenPathAndOnlySentFieldsToUseCase() throws Exception {
		given(updateMemberUseCase.updateMember(any())).willReturn(
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.CONNECTED, null));

		mockMvc.perform(updateMemberRequest(CHILD_USER_ID, """
						{"name":"민준"}
						"""))
				.andExpect(status().isOk());

		then(updateMemberUseCase).should().updateMember(new UpdateMemberCommand(
				FAMILY_ID, GUARDIAN_USER_ID, Role.GUARDIAN, CHILD_USER_ID, "민준", null, null));
	}

	@Test
	@DisplayName("PATCH /v1/families/me/members/{userId}에 형식이 틀린 phone을 보내면 422를 돌려준다")
	void updateMemberWithInvalidPhoneReturns422WithPhoneField() throws Exception {
		mockMvc.perform(updateMemberRequest(TEACHER_USER_ID, """
						{"phone":"01012345678"}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("phone")));

		then(updateMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PATCH /v1/families/me/members/{userId}에 role GUARDIAN을 보내면 422 VALIDATION_FAILED와 fields의 role을 돌려준다")
	void updateMemberWithGuardianRoleReturns422WithRoleField() throws Exception {
		given(updateMemberUseCase.updateMember(any())).willReturn(
				new MemberResult(CO_GUARDIAN_USER_ID, Role.GUARDIAN, "이보호", "+821011112222", DeviceStatus.CONNECTED, null));

		mockMvc.perform(updateMemberRequest(CO_GUARDIAN_USER_ID, """
						{"role":"GUARDIAN"}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("role")));

		then(updateMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PATCH /v1/families/me/members/{userId}에 multipart/form-data로 보내면 415 UNSUPPORTED_MEDIA_TYPE을 돌려주고 유스케이스를 부르지 않는다")
	void updateMemberWithMultipartReturns415UnsupportedMediaType() throws Exception {
		mockMvc.perform(multipart(HttpMethod.PATCH, MEMBERS_PATH + "/{userId}", CHILD_USER_ID)
						.file(new MockMultipartFile(REQUEST_PART, "", MediaType.APPLICATION_JSON_VALUE,
								"""
								{"name":"민준"}
								""".getBytes(StandardCharsets.UTF_8)))
						.file(new MockMultipartFile(PROFILE_IMAGE_PART, "a.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES))
						.header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(commonError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE"));

		then(updateMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/families/me/call-mode에 multipart/form-data로 보내면 415 UNSUPPORTED_MEDIA_TYPE을 돌려주고 유스케이스를 부르지 않는다")
	void changeCallModeWithMultipartReturns415UnsupportedMediaType() throws Exception {
		mockMvc.perform(multipart(HttpMethod.PUT, CALL_MODE_PATH)
						.file(new MockMultipartFile(REQUEST_PART, "", MediaType.APPLICATION_JSON_VALUE,
								"""
								{"joinCallMode":"AUTO"}
								""".getBytes(StandardCharsets.UTF_8)))
						.header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(commonError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE"));

		then(changeCallModeUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("DELETE /v1/families/me/members/{userId}는 204를 돌려준다")
	void removeMemberReturns204WithEmptyBody() throws Exception {
		mockMvc.perform(removeMemberRequest(TEACHER_USER_ID, guardianBearer()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	@DisplayName("DELETE /v1/families/me/members/{userId}는 토큰의 familyId · userId와 경로의 userId를 RemoveMemberCommand로 유스케이스에 넘긴다")
	void removeMemberPassesTokenAndPathToUseCase() throws Exception {
		mockMvc.perform(removeMemberRequest(TEACHER_USER_ID, guardianBearer()))
				.andExpect(status().isNoContent());

		then(removeMemberUseCase).should().removeMember(
				new RemoveMemberCommand(FAMILY_ID, GUARDIAN_USER_ID, TEACHER_USER_ID));
	}

	@Test
	@DisplayName("DELETE /v1/families/me/members/{userId}를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void removeMemberWithCoGuardianTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(removeMemberRequest(TEACHER_USER_ID, userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(removeMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("DELETE /v1/families/me/members/{userId}가 진행 중 픽업 멤버면 409 MEMBER_IN_ACTIVE_PICKUP을 돌려준다")
	void removeMemberInActivePickupReturns409MemberInActivePickup() throws Exception {
		willThrow(BusinessException.of(
				HttpStatus.CONFLICT, "MEMBER_IN_ACTIVE_PICKUP", "진행 중인 픽업에 묶인 멤버는 해제할 수 없습니다"))
				.given(removeMemberUseCase).removeMember(any());

		mockMvc.perform(removeMemberRequest(TEACHER_USER_ID, guardianBearer()))
				.andExpect(commonError(HttpStatus.CONFLICT, "MEMBER_IN_ACTIVE_PICKUP"));
	}

	@Test
	@DisplayName("PUT /v1/families/me/call-mode는 200과 joinCallMode · settingsVersion을 돌려준다")
	void changeCallModeReturns200WithJoinCallModeAndSettingsVersion() throws Exception {
		given(changeCallModeUseCase.changeCallMode(any())).willReturn(
				new CallModeResult(JoinCallMode.GUARDIAN_CONFIRM, 8));

		mockMvc.perform(changeCallModeRequest(guardianBearer(), """
						{"joinCallMode":"GUARDIAN_CONFIRM"}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"joinCallMode":"GUARDIAN_CONFIRM","settingsVersion":8}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("PUT /v1/families/me/call-mode는 토큰의 familyId(12)와 본문의 joinCallMode를 ChangeCallModeCommand로 유스케이스에 넘긴다")
	void changeCallModePassesTokenFamilyIdAndBodyToUseCase() throws Exception {
		given(changeCallModeUseCase.changeCallMode(any())).willReturn(
				new CallModeResult(JoinCallMode.GUARDIAN_CONFIRM, 8));

		mockMvc.perform(changeCallModeRequest(guardianBearer(), """
						{"joinCallMode":"GUARDIAN_CONFIRM"}
						"""))
				.andExpect(status().isOk());

		then(changeCallModeUseCase).should().changeCallMode(
				new ChangeCallModeCommand(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM));
	}

	@Test
	@DisplayName("PUT /v1/families/me/call-mode를 CO_GUARDIAN 토큰으로 부르면 200을 돌려준다")
	void changeCallModeWithCoGuardianTokenReturns200() throws Exception {
		given(changeCallModeUseCase.changeCallMode(any())).willReturn(
				new CallModeResult(JoinCallMode.AUTO, 9));

		mockMvc.perform(changeCallModeRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN), """
						{"joinCallMode":"AUTO"}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"joinCallMode":"AUTO","settingsVersion":9}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("PUT /v1/families/me/call-mode를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void changeCallModeWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(changeCallModeRequest(userBearer(DELEGATE_USER_ID, Role.DELEGATE), """
						{"joinCallMode":"GUARDIAN_CONFIRM"}
						"""))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(changeCallModeUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/families/me/call-mode에 joinCallMode를 빼고 보내면 422 VALIDATION_FAILED와 fields의 joinCallMode를 돌려준다")
	void changeCallModeWithoutJoinCallModeReturns422WithJoinCallModeField() throws Exception {
		mockMvc.perform(changeCallModeRequest(guardianBearer(), """
						{}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("joinCallMode")));

		then(changeCallModeUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/families/me/child-location는 200과 childLocationEnabled · settingsVersion을 돌려준다")
	void changeChildLocationReturns200WithChildLocationEnabledAndSettingsVersion() throws Exception {
		given(changeChildLocationUseCase.changeChildLocation(any())).willReturn(new ChildLocationResult(true, 8));

		mockMvc.perform(changeChildLocationRequest(guardianBearer(), """
						{"enabled":true}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"childLocationEnabled":true,"settingsVersion":8}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("PUT /v1/families/me/child-location는 토큰의 familyId(12)와 본문의 enabled를 ChangeChildLocationCommand로 유스케이스에 넘긴다")
	void changeChildLocationPassesTokenFamilyIdAndEnabledToUseCase() throws Exception {
		given(changeChildLocationUseCase.changeChildLocation(any())).willReturn(new ChildLocationResult(true, 8));

		mockMvc.perform(changeChildLocationRequest(guardianBearer(), """
						{"enabled":true}
						"""))
				.andExpect(status().isOk());

		then(changeChildLocationUseCase).should().changeChildLocation(new ChangeChildLocationCommand(FAMILY_ID, true));
	}

	@Test
	@DisplayName("PUT /v1/families/me/child-location를 CO_GUARDIAN 토큰으로 부르면 200과 childLocationEnabled false를 돌려준다")
	void changeChildLocationWithCoGuardianTokenReturns200() throws Exception {
		given(changeChildLocationUseCase.changeChildLocation(any())).willReturn(new ChildLocationResult(false, 9));

		mockMvc.perform(changeChildLocationRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN), """
						{"enabled":false}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"childLocationEnabled":false,"settingsVersion":9}
						""", JsonCompareMode.STRICT));
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CHILD", "DELEGATE", "TEACHER"})
	@DisplayName("PUT /v1/families/me/child-location를 CHILD · DELEGATE · TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void changeChildLocationWithNonGuardianTokenReturns403Forbidden(Role role) throws Exception {
		mockMvc.perform(changeChildLocationRequest(userBearer(CHILD_USER_ID, role), """
						{"enabled":true}
						"""))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(changeChildLocationUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/families/me/child-location에 enabled를 빼고 보내면 422 VALIDATION_FAILED와 fields의 enabled를 돌려준다")
	void changeChildLocationWithoutEnabledReturns422WithEnabledField() throws Exception {
		mockMvc.perform(changeChildLocationRequest(guardianBearer(), """
						{}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("enabled")));

		then(changeChildLocationUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/families/me/child-location에 enabled를 null로 보내면 422 VALIDATION_FAILED와 fields의 enabled를 돌려준다")
	void changeChildLocationWithNullEnabledReturns422WithEnabledField() throws Exception {
		mockMvc.perform(changeChildLocationRequest(guardianBearer(), """
						{"enabled":null}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("enabled")));

		then(changeChildLocationUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder changeChildLocationRequest(String bearer, String body) {
		return put(CHILD_LOCATION_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder changeCallModeRequest(String bearer, String body) {
		return put(CALL_MODE_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder updateMemberRequest(long userId, String body) {
		return patch(MEMBERS_PATH + "/{userId}", userId)
				.header(HttpHeaders.AUTHORIZATION, guardianBearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder removeMemberRequest(long userId, String bearer) {
		return delete(MEMBERS_PATH + "/{userId}", userId)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
	}

	private MockHttpServletRequestBuilder registerMemberRequest(String body) {
		return registerMemberRequest(guardianBearer(), body);
	}

	private MockHttpServletRequestBuilder registerMemberRequest(String bearer, String body) {
		return post(MEMBERS_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockMultipartHttpServletRequestBuilder registerMemberMultipartRequest(String requestJson, MockMultipartFile... extraParts) {
		MockMultipartHttpServletRequestBuilder builder = multipart(MEMBERS_PATH)
				.file(new MockMultipartFile(REQUEST_PART, "", MediaType.APPLICATION_JSON_VALUE,
						requestJson.getBytes(StandardCharsets.UTF_8)));
		for (MockMultipartFile part : extraParts) {
			builder.file(part);
		}
		return builder
				.header(HttpHeaders.AUTHORIZATION, guardianBearer())
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
	}

	private String guardianBearer() {
		return userBearer(GUARDIAN_USER_ID, Role.GUARDIAN);
	}

	private String userBearer(long userId, Role role) {
		return bearer(accessTokenFixture.user(userId, FAMILY_ID, role));
	}

	private static VehicleSummary linkedVehicle() {
		return new VehicleSummary(VEHICLE_ID, "아이오닉 5", "12가 ****");
	}

	private static FamilyResult familyResultWithChildLocation(boolean childLocationEnabled) {
		return new FamilyResult(FAMILY_ID, JoinCallMode.AUTO, linkedVehicle(), List.of(
				new MemberResult(GUARDIAN_USER_ID, Role.GUARDIAN, "김보호", "+821012345678", DeviceStatus.CONNECTED, null),
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.INVITE_PENDING, null)),
				childLocationEnabled);
	}

	private static FamilyResult familyResult(VehicleSummary vehicle) {
		return new FamilyResult(FAMILY_ID, JoinCallMode.AUTO, vehicle, List.of(
				new MemberResult(GUARDIAN_USER_ID, Role.GUARDIAN, "김보호", "+821012345678", DeviceStatus.CONNECTED, null),
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.INVITE_PENDING, null)));
	}

}
