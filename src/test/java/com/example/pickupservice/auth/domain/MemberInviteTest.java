package com.example.pickupservice.auth.domain;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberInviteTest {

	private static final Instant ISSUED_AT = Instant.parse("2026-11-05T12:05:00Z");
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-06T12:05:00Z");
	private static final String TOKEN = "inv_3f9a1c7e2b8d4a6f9e0c1b2a3d4e5f60";

	@Test
	@DisplayName("MemberInvite는 발급 시각 + 24시간을 expiresAt으로 갖는다")
	void issueSetsExpiresAtTwentyFourHoursAfterIssuedAt() {
		MemberInvite invite = MemberInvite.issue(31L, TOKEN, ISSUED_AT);

		assertThat(invite.getExpiresAt()).isEqualTo(ISSUED_AT.plus(Duration.ofHours(24)));
		assertThat(invite.getExpiresAt()).isEqualTo(EXPIRES_AT);
	}

	@Test
	@DisplayName("MemberInvite.issue는 userId · token을 받은 값 그대로 갖고 inviteId · usedAt은 비어 있다")
	void issueKeepsUserIdAndTokenAndLeavesInviteIdAndUsedAtEmpty() {
		MemberInvite invite = MemberInvite.issue(31L, TOKEN, ISSUED_AT);

		assertThat(invite.getUserId()).isEqualTo(31L);
		assertThat(invite.getToken()).isEqualTo(TOKEN);
		assertThat(invite.getInviteId()).isNull();
		assertThat(invite.getUsedAt()).isNull();
		assertThat(invite.isUsed()).isFalse();
	}

	@Test
	@DisplayName("MemberInvite는 usedAt이 채워지면 사용됨으로 판정된다")
	void isUsedReturnsTrueWhenUsedAtIsFilled() {
		MemberInvite unused = MemberInvite.restore(901L, 31L, TOKEN, EXPIRES_AT, null);
		MemberInvite used = MemberInvite.restore(902L, 31L, TOKEN, EXPIRES_AT, ISSUED_AT.plusSeconds(600));

		assertThat(unused.isUsed()).isFalse();
		assertThat(used.isUsed()).isTrue();
	}

	@Test
	@DisplayName("MemberInvite.isExpired는 expiresAt 전이면 false, 지나면 true를 돌려준다")
	void isExpiredReturnsFalseBeforeAndTrueAfterExpiresAt() {
		MemberInvite invite = MemberInvite.issue(31L, TOKEN, ISSUED_AT);

		assertThat(invite.isExpired(ISSUED_AT)).isFalse();
		assertThat(invite.isExpired(EXPIRES_AT.minusMillis(1))).isFalse();
		assertThat(invite.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
	}

	@Test
	@DisplayName("MemberInvite.isExpired는 expiresAt과 같은 시각이면 만료로 본다")
	void isExpiredReturnsTrueAtExactlyExpiresAt() {
		MemberInvite invite = MemberInvite.issue(31L, TOKEN, ISSUED_AT);

		assertThat(invite.isExpired(EXPIRES_AT)).isTrue();
	}

	@Test
	@DisplayName("MemberInvite.markUsed는 usedAt을 받은 시각으로 채우고 expiresAt은 그대로 둔다")
	void markUsedFillsUsedAtAndKeepsExpiresAt() {
		MemberInvite invite = MemberInvite.issue(31L, TOKEN, ISSUED_AT);
		Instant usedAt = ISSUED_AT.plusSeconds(600);

		invite.markUsed(usedAt);

		assertThat(invite.getUsedAt()).isEqualTo(usedAt);
		assertThat(invite.isUsed()).isTrue();
		assertThat(invite.getExpiresAt()).isEqualTo(EXPIRES_AT);
	}

	@Test
	@DisplayName("MemberInvite.revoke는 expiresAt을 지금으로 바꿔 바로 만료되게 하고 usedAt은 비워 둔다")
	void revokeMovesExpiresAtToNowAndMakesInviteExpired() {
		MemberInvite invite = MemberInvite.issue(31L, TOKEN, ISSUED_AT);
		Instant now = ISSUED_AT.plusSeconds(3600);

		invite.revoke(now);

		assertThat(invite.getExpiresAt()).isEqualTo(now);
		assertThat(invite.isExpired(now)).isTrue();
		assertThat(invite.getUsedAt()).isNull();
		assertThat(invite.isUsed()).isFalse();
	}

	@Test
	@DisplayName("MemberInvite.restore는 저장된 값을 그대로 가진 객체를 만든다")
	void restoreKeepsAllStoredValues() {
		Instant usedAt = ISSUED_AT.plusSeconds(600);

		MemberInvite invite = MemberInvite.restore(901L, 31L, TOKEN, EXPIRES_AT, usedAt);

		assertThat(invite.getInviteId()).isEqualTo(901L);
		assertThat(invite.getUserId()).isEqualTo(31L);
		assertThat(invite.getToken()).isEqualTo(TOKEN);
		assertThat(invite.getExpiresAt()).isEqualTo(EXPIRES_AT);
		assertThat(invite.getUsedAt()).isEqualTo(usedAt);
	}

	@Test
	@DisplayName("MemberInvite.restore는 비어 있던 usedAt을 비운 채로 만든다")
	void restoreKeepsNullUsedAtNull() {
		MemberInvite invite = MemberInvite.restore(903L, 32L, "inv_other", EXPIRES_AT, null);

		assertThat(invite.getInviteId()).isEqualTo(903L);
		assertThat(invite.getUserId()).isEqualTo(32L);
		assertThat(invite.getToken()).isEqualTo("inv_other");
		assertThat(invite.getUsedAt()).isNull();
	}

	@Test
	@DisplayName("LinkCode.expire는 status를 EXPIRED로, MemberInvite.markUsed는 usedAt을, MemberInvite.revoke는 expiresAt을 지금으로 바꾼다")
	void linkCodeExpireAndMemberInviteMarkUsedAndRevokeChangeTheirFields() {
		Instant now = ISSUED_AT.plusSeconds(120);

		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);
		linkCode.expire();
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.EXPIRED);

		MemberInvite usedInvite = MemberInvite.issue(31L, TOKEN, ISSUED_AT);
		usedInvite.markUsed(now);
		assertThat(usedInvite.getUsedAt()).isEqualTo(now);

		MemberInvite revokedInvite = MemberInvite.issue(31L, "inv_previous", ISSUED_AT);
		revokedInvite.revoke(now);
		assertThat(revokedInvite.getExpiresAt()).isEqualTo(now);
		assertThat(revokedInvite.isExpired(now)).isTrue();
	}
}
