package com.example.pickupservice.auth.domain;

import com.example.pickupservice.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentsTest {

	@Test
	@DisplayName("CHILD의 필수 동의는 TERMS · PRIVACY · LOCATION · PUSH다")
	void childRequiresTermsPrivacyLocationAndPush() {
		assertThat(Consents.requiredForInvite(Role.CHILD))
				.containsExactlyInAnyOrder(ConsentType.TERMS, ConsentType.PRIVACY, ConsentType.LOCATION, ConsentType.PUSH);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@EnumSource(value = Role.class, names = "CHILD", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("자녀 외 role의 초대 필수 동의는 TERMS · PRIVACY다")
	void nonChildRolesRequireTermsAndPrivacyOnly(Role role) {
		assertThat(Consents.requiredForInvite(role))
				.containsExactlyInAnyOrder(ConsentType.TERMS, ConsentType.PRIVACY);
	}

	@Test
	@DisplayName("보호자 연결의 필수 동의는 TERMS · PRIVACY이고 MARKETING은 선택이다")
	void guardianLinkRequiresTermsAndPrivacyAndMarketingIsOptional() {
		assertThat(Consents.requiredForGuardianLink())
				.containsExactlyInAnyOrder(ConsentType.TERMS, ConsentType.PRIVACY)
				.doesNotContain(ConsentType.MARKETING);
	}

	@Test
	@DisplayName("동의 종류는 TERMS · PRIVACY · MARKETING · LOCATION · PUSH · SERVICE_IMPROVEMENT 여섯 가지다")
	void consentTypesAreTheSixDefinedValues() {
		assertThat(ConsentType.values())
				.containsExactlyInAnyOrder(ConsentType.TERMS, ConsentType.PRIVACY, ConsentType.MARKETING,
						ConsentType.LOCATION, ConsentType.PUSH, ConsentType.SERVICE_IMPROVEMENT);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@EnumSource(Role.class)
	@DisplayName("SERVICE_IMPROVEMENT는 선택 동의라 초대 필수 동의 어디에도 들어가지 않는다")
	void serviceImprovementIsNotRequiredForInvite(Role role) {
		assertThat(Consents.requiredForInvite(role)).doesNotContain(ConsentType.SERVICE_IMPROVEMENT);
	}

	@Test
	@DisplayName("SERVICE_IMPROVEMENT는 보호자 연결 필수 동의에 들어가지 않는다")
	void serviceImprovementIsNotRequiredForGuardianLink() {
		assertThat(Consents.requiredForGuardianLink()).doesNotContain(ConsentType.SERVICE_IMPROVEMENT);
	}
}
