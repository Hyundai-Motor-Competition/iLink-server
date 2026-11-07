package com.example.pickupservice.auth.domain;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinkCodeTest {

	private static final Instant ISSUED_AT = Instant.parse("2026-11-05T12:05:00Z");
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-05T12:10:00Z");

	@Test
	@DisplayName("LinkCode는 발급 시각 + 5분을 expiresAt으로 갖고 status ISSUED로 시작한다")
	void issueSetsExpiresAtFiveMinutesAfterIssuedAtAndStartsAsIssued() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);

		assertThat(linkCode.getExpiresAt()).isEqualTo(ISSUED_AT.plus(Duration.ofMinutes(5)));
		assertThat(linkCode.getExpiresAt()).isEqualTo(Instant.parse("2026-11-05T12:10:00Z"));
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.ISSUED);
	}

	@Test
	@DisplayName("LinkCode.issue는 vehicleId · code를 받은 값 그대로 갖고 codeId · usedAt · mode · tokenDeliveredAt은 비어 있다")
	void issueKeepsVehicleIdAndCodeAndLeavesOtherFieldsEmpty() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);

		assertThat(linkCode.getVehicleId()).isEqualTo(77L);
		assertThat(linkCode.getCode()).isEqualTo("K7P2QX9M");
		assertThat(linkCode.getCodeId()).isNull();
		assertThat(linkCode.getUsedAt()).isNull();
		assertThat(linkCode.getMode()).isNull();
		assertThat(linkCode.getTokenDeliveredAt()).isNull();
	}

	@Test
	@DisplayName("LinkCode는 expiresAt이 지나면 만료로 판정된다")
	void isExpiredReturnsTrueAfterExpiresAt() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);

		assertThat(linkCode.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
	}

	@Test
	@DisplayName("LinkCode.isExpired는 expiresAt 전이면 false를 돌려준다")
	void isExpiredReturnsFalseBeforeExpiresAt() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);

		assertThat(linkCode.isExpired(EXPIRES_AT.minusMillis(1))).isFalse();
		assertThat(linkCode.isExpired(ISSUED_AT)).isFalse();
	}

	@Test
	@DisplayName("LinkCode.isExpired는 expiresAt과 같은 시각이면 만료로 본다")
	void isExpiredReturnsTrueAtExactlyExpiresAt() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);

		assertThat(linkCode.isExpired(EXPIRES_AT)).isTrue();
	}

	@Test
	@DisplayName("LinkCode는 mode(NEW · RELINK)와 tokenDeliveredAt을 갖고, 승인하면 mode가 채워지고 USED가 되며 토큰을 내려주면 tokenDeliveredAt이 채워진다")
	void approveFillsModeAndMarksUsedAndMarkTokenDeliveredFillsTokenDeliveredAt() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);
		Instant approvedAt = ISSUED_AT.plusSeconds(60);
		Instant deliveredAt = ISSUED_AT.plusSeconds(62);

		linkCode.approve(LinkMode.NEW, approvedAt);

		assertThat(linkCode.getMode()).isEqualTo(LinkMode.NEW);
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.USED);
		assertThat(linkCode.getUsedAt()).isEqualTo(approvedAt);
		assertThat(linkCode.getTokenDeliveredAt()).isNull();

		linkCode.markTokenDelivered(deliveredAt);

		assertThat(linkCode.getTokenDeliveredAt()).isEqualTo(deliveredAt);
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.USED);
		assertThat(linkCode.getMode()).isEqualTo(LinkMode.NEW);
	}

	@Test
	@DisplayName("LinkCode.approve는 RELINK로 승인하면 mode가 RELINK가 된다")
	void approveWithRelinkFillsModeRelink() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);
		Instant approvedAt = ISSUED_AT.plusSeconds(30);

		linkCode.approve(LinkMode.RELINK, approvedAt);

		assertThat(linkCode.getMode()).isEqualTo(LinkMode.RELINK);
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.USED);
		assertThat(linkCode.getUsedAt()).isEqualTo(approvedAt);
	}

	@Test
	@DisplayName("LinkCode.restore는 저장된 값을 그대로 가진 객체를 만든다")
	void restoreKeepsAllStoredValues() {
		Instant usedAt = ISSUED_AT.plusSeconds(60);
		Instant deliveredAt = ISSUED_AT.plusSeconds(62);

		LinkCode linkCode = LinkCode.restore(5012L, 77L, "K7P2QX9M", LinkCodeStatus.USED, EXPIRES_AT, usedAt,
				LinkMode.RELINK, deliveredAt);

		assertThat(linkCode.getCodeId()).isEqualTo(5012L);
		assertThat(linkCode.getVehicleId()).isEqualTo(77L);
		assertThat(linkCode.getCode()).isEqualTo("K7P2QX9M");
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.USED);
		assertThat(linkCode.getExpiresAt()).isEqualTo(EXPIRES_AT);
		assertThat(linkCode.getUsedAt()).isEqualTo(usedAt);
		assertThat(linkCode.getMode()).isEqualTo(LinkMode.RELINK);
		assertThat(linkCode.getTokenDeliveredAt()).isEqualTo(deliveredAt);
	}

	@Test
	@DisplayName("LinkCode.restore는 비어 있던 usedAt · mode · tokenDeliveredAt을 비운 채로 만든다")
	void restoreKeepsNullFieldsNull() {
		LinkCode linkCode = LinkCode.restore(5013L, 77L, "AB12CD34", LinkCodeStatus.ISSUED, EXPIRES_AT, null, null,
				null);

		assertThat(linkCode.getCodeId()).isEqualTo(5013L);
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.ISSUED);
		assertThat(linkCode.getUsedAt()).isNull();
		assertThat(linkCode.getMode()).isNull();
		assertThat(linkCode.getTokenDeliveredAt()).isNull();
	}

	@Test
	@DisplayName("LinkCode.expire는 status를 EXPIRED로 바꾼다")
	void expireChangesStatusToExpired() {
		LinkCode linkCode = LinkCode.issue(77L, "K7P2QX9M", ISSUED_AT);

		linkCode.expire();

		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.EXPIRED);
		assertThat(linkCode.getExpiresAt()).isEqualTo(EXPIRES_AT);
		assertThat(linkCode.getUsedAt()).isNull();
	}
}
