package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.InviteResult;
import com.example.pickupservice.auth.application.port.in.IssueInviteCommand;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueInviteServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T12:05:00Z");
	private static final String BASE_URL = "https://app.example/invite/";
	private static final long FAMILY_ID = 12L;
	private static final long OTHER_FAMILY_ID = 99L;
	private static final long CHILD_ID = 33L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long PREVIOUS_INVITE_ID = 701L;
	private static final String PREVIOUS_TOKEN = "inv-previous-token";
	private static final String NEW_TOKEN = "inv-new-token-0123456789";

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private LoadInvitePort loadInvitePort;

	@Mock
	private SaveInvitePort saveInvitePort;

	@Mock
	private InviteTokenGenerator inviteTokenGenerator;

	@Mock
	private Clock clock;

	private IssueInviteService issueInviteService;

	private final AtomicLong nextInviteId = new AtomicLong(800L);

	@BeforeEach
	void setUp() {
		issueInviteService = new IssueInviteService(loadMemberUseCase, loadDevicePort, loadInvitePort,
				saveInvitePort, inviteTokenGenerator, BASE_URL, clock);
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		lenient().when(inviteTokenGenerator.generate()).thenReturn(NEW_TOKEN);
		lenient().when(loadInvitePort.loadLatestInvite(CHILD_ID)).thenReturn(Optional.empty());
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID)).thenReturn(List.of());
		lenient().when(saveInvitePort.saveInvite(any(MemberInvite.class))).thenAnswer(invocation -> {
			MemberInvite invite = invocation.getArgument(0);
			if (invite.getInviteId() != null) {
				return invite;
			}
			return MemberInvite.restore(nextInviteId.getAndIncrement(), invite.getUserId(), invite.getToken(),
					invite.getExpiresAt(), invite.getUsedAt());
		});
	}

	@Test
	@DisplayName("초대 발급은 기기가 연결된 멤버에 replaceDevice false면 DEVICE_ALREADY_CONNECTED로 거절한다")
	void issueInviteRejectsMemberWithConnectedDeviceWhenReplaceDeviceIsFalse() {
		givenMember(CHILD_ID, FAMILY_ID);
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID)).thenReturn(List.of(liveDevice(CHILD_ID)));

		assertError(() -> issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_ID, false)),
				HttpStatus.CONFLICT, "DEVICE_ALREADY_CONNECTED");

		verify(saveInvitePort, never()).saveInvite(any());
	}

	@Test
	@DisplayName("초대 발급은 기기가 연결된 멤버라도 replaceDevice true면 초대를 만든다")
	void issueInviteIssuesForMemberWithConnectedDeviceWhenReplaceDeviceIsTrue() {
		givenMember(CHILD_ID, FAMILY_ID);
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID))
				.thenReturn(List.of(liveDevice(CHILD_ID)));

		InviteResult result = issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_ID, true));

		assertThat(result.inviteUrl()).isEqualTo(BASE_URL + NEW_TOKEN);
		assertThat(savedInvites()).anySatisfy(invite -> assertThat(invite.getToken()).isEqualTo(NEW_TOKEN));
	}

	@Test
	@DisplayName("초대 발급은 기본 주소 + token인 inviteUrl과 24시간 뒤 expiresAt을 돌려주고 초대를 저장한다")
	void issueInviteReturnsUrlAndExpiresAtAndSavesInvite() {
		givenMember(CHILD_ID, FAMILY_ID);

		InviteResult result = issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_ID, false));

		assertThat(result.inviteUrl()).isEqualTo("https://app.example/invite/" + NEW_TOKEN);
		assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
		MemberInvite saved = savedInvites().getLast();
		assertThat(saved.getUserId()).isEqualTo(CHILD_ID);
		assertThat(saved.getToken()).isEqualTo(NEW_TOKEN);
		assertThat(saved.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
		assertThat(saved.getUsedAt()).isNull();
	}

	@Test
	@DisplayName("초대를 다시 발급하면 이전 초대 토큰으로는 확인 · 수락할 수 없다")
	void reissuingInviteRevokesPreviousInvite() {
		givenMember(CHILD_ID, FAMILY_ID);
		MemberInvite previous = MemberInvite.restore(PREVIOUS_INVITE_ID, CHILD_ID, PREVIOUS_TOKEN,
				NOW.plus(Duration.ofHours(20)), null);
		when(loadInvitePort.loadLatestInvite(CHILD_ID)).thenReturn(Optional.of(previous));

		InviteResult result = issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_ID, false));

		List<MemberInvite> saved = savedInvites();
		assertThat(saved).filteredOn(invite -> PREVIOUS_TOKEN.equals(invite.getToken()))
				.as("이전 초대 폐기 저장")
				.isNotEmpty()
				.allSatisfy(invite -> {
					assertThat(invite.getInviteId()).isEqualTo(PREVIOUS_INVITE_ID);
					assertThat(invite.getExpiresAt()).isEqualTo(NOW);
					assertThat(invite.isExpired(NOW)).isTrue();
					assertThat(invite.getUsedAt()).isNull();
				});
		assertThat(saved).filteredOn(invite -> NEW_TOKEN.equals(invite.getToken()))
				.as("새 초대 저장")
				.singleElement()
				.satisfies(invite -> assertThat(invite.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24))));
		assertThat(result.inviteUrl()).isEqualTo(BASE_URL + NEW_TOKEN);
	}

	@Test
	@DisplayName("초대 발급은 다른 가족의 멤버면 NOT_FOUND로 거절한다")
	void issueInviteRejectsMemberOfOtherFamily() {
		givenMember(CHILD_ID, OTHER_FAMILY_ID);

		assertError(() -> issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_ID, false)),
				HttpStatus.NOT_FOUND, "NOT_FOUND");

		verify(saveInvitePort, never()).saveInvite(any());
	}

	@Test
	@DisplayName("초대 발급은 없는 멤버면 NOT_FOUND로 거절한다")
	void issueInviteRejectsUnknownMember() {
		when(loadMemberUseCase.findMember(CHILD_ID)).thenReturn(Optional.empty());

		assertError(() -> issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_ID, false)),
				HttpStatus.NOT_FOUND, "NOT_FOUND");

		verify(saveInvitePort, never()).saveInvite(any());
	}

	@Test
	@DisplayName("초대 발급은 대상 멤버의 role이 GUARDIAN이면 FORBIDDEN으로 거절한다")
	void issueInviteRejectsGuardianMember() {
		givenMember(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.USER, GUARDIAN_ID)).thenReturn(List.of());
		lenient().when(loadInvitePort.loadLatestInvite(GUARDIAN_ID)).thenReturn(Optional.of(MemberInvite.restore(
				PREVIOUS_INVITE_ID, GUARDIAN_ID, PREVIOUS_TOKEN, NOW.plus(Duration.ofHours(20)), null)));

		assertError(() -> issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, GUARDIAN_ID, false)),
				HttpStatus.FORBIDDEN, "FORBIDDEN");

		verify(saveInvitePort, never()).saveInvite(any());
		verify(inviteTokenGenerator, never()).generate();
	}

	@Test
	@DisplayName("초대 발급은 대상 멤버의 role이 GUARDIAN이면 replaceDevice true여도 FORBIDDEN으로 거절한다")
	void issueInviteRejectsGuardianMemberEvenWhenReplaceDeviceIsTrue() {
		givenMember(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.USER, GUARDIAN_ID))
				.thenReturn(List.of(liveDevice(GUARDIAN_ID)));
		lenient().when(loadInvitePort.loadLatestInvite(GUARDIAN_ID)).thenReturn(Optional.empty());

		assertError(() -> issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, GUARDIAN_ID, true)),
				HttpStatus.FORBIDDEN, "FORBIDDEN");

		verify(saveInvitePort, never()).saveInvite(any());
		verify(inviteTokenGenerator, never()).generate();
	}

	@Test
	@DisplayName("초대 발급은 대상 멤버의 role이 CO_GUARDIAN이면 초대를 만든다")
	void issueInviteIssuesForCoGuardianMember() {
		givenMember(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN);
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.USER, CO_GUARDIAN_ID)).thenReturn(List.of());
		lenient().when(loadInvitePort.loadLatestInvite(CO_GUARDIAN_ID)).thenReturn(Optional.empty());

		InviteResult result = issueInviteService.issueInvite(new IssueInviteCommand(FAMILY_ID, CO_GUARDIAN_ID, false));

		assertThat(result.inviteUrl()).isEqualTo(BASE_URL + NEW_TOKEN);
		assertThat(savedInvites()).singleElement().satisfies(invite -> {
			assertThat(invite.getUserId()).isEqualTo(CO_GUARDIAN_ID);
			assertThat(invite.getToken()).isEqualTo(NEW_TOKEN);
		});
	}

	private void givenMember(long userId, long familyId) {
		givenMember(userId, familyId, Role.CHILD);
	}

	private void givenMember(long userId, long familyId, Role role) {
		when(loadMemberUseCase.findMember(userId))
				.thenReturn(Optional.of(new MemberInfo(userId, familyId, role, "민준", null)));
	}

	private List<MemberInvite> savedInvites() {
		ArgumentCaptor<MemberInvite> captor = ArgumentCaptor.forClass(MemberInvite.class);
		verify(saveInvitePort, atLeastOnce()).saveInvite(captor.capture());
		return captor.getAllValues();
	}

	private static Device liveDevice(long userId) {
		return Device.restore(401L, OwnerType.USER, userId, Platform.ANDROID, "fcm-old", NOW.minusSeconds(3600),
				"0".repeat(64), null);
	}

	private static void assertError(ThrowingCallable call, HttpStatus status, String code) {
		assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> {
			assertThat(e.status()).isEqualTo(status);
			assertThat(e.code()).isEqualTo(code);
		});
	}
}
