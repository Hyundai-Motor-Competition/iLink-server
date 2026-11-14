package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.InviteInfo;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInviteServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T12:05:00Z");
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long CHILD_ID = 33L;
	private static final long TEACHER_ID = 34L;
	private static final String TOKEN = "inv-token-0123456789";

	@Mock
	private LoadInvitePort loadInvitePort;

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private Clock clock;

	@InjectMocks
	private GetInviteService getInviteService;

	@BeforeEach
	void setUp() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		lenient().when(loadMemberUseCase.findMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "박공동", null),
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", null),
				new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "이선생", null)));
		lenient().when(loadMemberUseCase.findMember(CHILD_ID))
				.thenReturn(Optional.of(new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));
		lenient().when(loadMemberUseCase.findMember(TEACHER_ID))
				.thenReturn(Optional.of(new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "이선생", null)));
	}

	@Test
	@DisplayName("초대 확인은 role CHILD 멤버에 requiredConsents TERMS · PRIVACY · LOCATION · PUSH를 돌려준다")
	void getInviteReturnsChildRequiredConsentsInOrder() {
		Instant expiresAt = NOW.plus(Duration.ofHours(23));
		givenInvite(MemberInvite.restore(701L, CHILD_ID, TOKEN, expiresAt, null));

		InviteInfo info = getInviteService.getInvite(TOKEN);

		assertThat(info.requiredConsents()).containsExactly(ConsentType.TERMS, ConsentType.PRIVACY,
				ConsentType.LOCATION, ConsentType.PUSH);
		assertThat(info.role()).isEqualTo(Role.CHILD);
		assertThat(info.memberName()).isEqualTo("민준");
		assertThat(info.inviterName()).isEqualTo("김보호");
		assertThat(info.expiresAt()).isEqualTo(expiresAt);
	}

	@Test
	@DisplayName("초대 확인은 CHILD가 아닌 멤버에 requiredConsents TERMS · PRIVACY를 돌려준다")
	void getInviteReturnsBasicRequiredConsentsForNonChild() {
		givenInvite(MemberInvite.restore(702L, TEACHER_ID, TOKEN, NOW.plus(Duration.ofHours(23)), null));

		InviteInfo info = getInviteService.getInvite(TOKEN);

		assertThat(info.requiredConsents()).containsExactly(ConsentType.TERMS, ConsentType.PRIVACY);
		assertThat(info.role()).isEqualTo(Role.TEACHER);
		assertThat(info.memberName()).isEqualTo("이선생");
		assertThat(info.inviterName()).isEqualTo("김보호");
	}

	@Test
	@DisplayName("초대 확인은 사용된 초대면 INVITE_ALREADY_USED로 거절한다")
	void getInviteRejectsUsedInvite() {
		givenInvite(MemberInvite.restore(701L, CHILD_ID, TOKEN, NOW.plus(Duration.ofHours(23)),
				NOW.minusSeconds(60)));

		assertError(() -> getInviteService.getInvite(TOKEN), HttpStatus.CONFLICT, "INVITE_ALREADY_USED");
	}

	@Test
	@DisplayName("초대 확인은 만료된 초대면 INVITE_EXPIRED로 거절한다")
	void getInviteRejectsExpiredInvite() {
		givenInvite(MemberInvite.restore(701L, CHILD_ID, TOKEN, NOW.minusSeconds(1), null));

		assertError(() -> getInviteService.getInvite(TOKEN), HttpStatus.GONE, "INVITE_EXPIRED");
	}

	@Test
	@DisplayName("초대를 다시 발급해 폐기된 이전 초대 토큰으로 확인하면 INVITE_EXPIRED로 거절한다")
	void getInviteRejectsPreviousInviteRevokedByReissue() {
		MemberInvite previous = MemberInvite.issue(CHILD_ID, TOKEN, NOW.minus(Duration.ofHours(1)));
		previous.revoke(NOW);
		givenInvite(previous);

		assertError(() -> getInviteService.getInvite(TOKEN), HttpStatus.GONE, "INVITE_EXPIRED");
	}

	@Test
	@DisplayName("초대 확인은 없는 토큰이면 NOT_FOUND로 거절한다")
	void getInviteRejectsUnknownToken() {
		when(loadInvitePort.loadInviteByToken(TOKEN)).thenReturn(Optional.empty());

		assertError(() -> getInviteService.getInvite(TOKEN), HttpStatus.NOT_FOUND, "NOT_FOUND");
	}

	@Test
	@DisplayName("초대 확인은 대상 멤버가 해제된 멤버(removed true)면 NOT_FOUND로 거절한다")
	void getInviteRejectsRemovedMember() {
		when(loadMemberUseCase.findMember(CHILD_ID))
				.thenReturn(Optional.of(new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null, null, true)));
		givenInvite(MemberInvite.restore(701L, CHILD_ID, TOKEN, NOW.plus(Duration.ofHours(23)), null));

		assertError(() -> getInviteService.getInvite(TOKEN), HttpStatus.NOT_FOUND, "NOT_FOUND");
	}

	private void givenInvite(MemberInvite invite) {
		when(loadInvitePort.loadInviteByToken(TOKEN)).thenReturn(Optional.of(invite));
	}

	private static void assertError(ThrowingCallable call, HttpStatus status, String code) {
		assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> {
			assertThat(e.status()).isEqualTo(status);
			assertThat(e.code()).isEqualTo(code);
		});
	}
}
