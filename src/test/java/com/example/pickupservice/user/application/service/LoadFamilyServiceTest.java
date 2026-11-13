package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoadFamilyServiceTest {

	private static final long FAMILY_ID = 12L;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@InjectMocks
	private LoadFamilyService loadFamilyService;

	@Test
	@DisplayName("user의 LoadMemberUseCase는 findMember · findMembers로 MemberInfo를 돌려주고 LoadFamilyUseCase는 findFamily로 FamilyInfo를 돌려준다")
	void findFamilyReturnsFamilyInfoFromLoadedFamily() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM, 7)));

		FamilyInfo result = loadFamilyService.findFamily(FAMILY_ID);

		assertThat(result).isEqualTo(new FamilyInfo(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM, 7));
	}

	@Test
	@DisplayName("findFamily는 저장된 Family의 childLocationEnabled가 true이면 FamilyInfo의 childLocationEnabled도 true를 돌려준다")
	void findFamilyReturnsChildLocationEnabledTrueWhenFamilyHasItOn() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM, 7, true)));

		FamilyInfo result = loadFamilyService.findFamily(FAMILY_ID);

		assertThat(result.childLocationEnabled()).isTrue();
	}

	@Test
	@DisplayName("findFamily는 가족이 없으면 NOT_FOUND로 거절한다")
	void findFamilyThrowsNotFoundWhenFamilyMissing() {
		given(loadFamilyPort.loadFamily(FAMILY_ID)).willReturn(Optional.empty());

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> loadFamilyService.findFamily(FAMILY_ID));

		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
	}
}
