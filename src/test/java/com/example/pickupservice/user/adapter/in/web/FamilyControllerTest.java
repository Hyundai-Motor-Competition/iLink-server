package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CallModeResult;
import com.example.pickupservice.user.application.port.in.ChangeCallModeCommand;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long TEACHER_USER_ID = 141L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long CHILD_USER_ID = 131L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final long VEHICLE_ID = 77L;

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
						 "joinCallMode":"AUTO",
						 "vehicle":{"vehicleId":77,"model":"아이오닉 5","plateNumberMasked":"12가 ****"},
						 "members":[
						   {"userId":101,"role":"GUARDIAN","name":"김보호","phone":"+821012345678","deviceStatus":"CONNECTED"},
						   {"userId":131,"role":"CHILD","name":"민준","phone":null,"deviceStatus":"INVITE_PENDING"}
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
	@DisplayName("GET /v1/families/me를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void getMyFamilyWithTeacherTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(get(MY_FAMILY_PATH)
						.header(HttpHeaders.AUTHORIZATION, userBearer(TEACHER_USER_ID, Role.TEACHER)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getMyFamilyUseCase).shouldHaveNoInteractions();
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
	@DisplayName("PATCH /v1/families/me/members/{userId}는 200과 userId · role · name · phone · deviceStatus를 돌려준다")
	void updateMemberReturns200WithMemberFields() throws Exception {
		given(updateMemberUseCase.updateMember(any())).willReturn(
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.CONNECTED));

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
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.CONNECTED));

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
		return post(MEMBERS_PATH)
				.header(HttpHeaders.AUTHORIZATION, guardianBearer())
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
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

	private static FamilyResult familyResult(VehicleSummary vehicle) {
		return new FamilyResult(FAMILY_ID, JoinCallMode.AUTO, vehicle, List.of(
				new MemberResult(GUARDIAN_USER_ID, Role.GUARDIAN, "김보호", "+821012345678", DeviceStatus.CONNECTED),
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.INVITE_PENDING)));
	}

}
