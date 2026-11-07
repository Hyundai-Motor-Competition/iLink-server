package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.GetVehicleSettingsSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.VehicleChild;
import com.example.pickupservice.schedule.application.port.in.VehicleSettingsSummary;
import com.example.pickupservice.schedule.domain.GuardianNameMasker;
import com.example.pickupservice.user.application.port.in.LoadFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class GetVehicleSettingsSummaryService implements GetVehicleSettingsSummaryUseCase {

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadFamilyUseCase loadFamilyUseCase;

	@Override
	@Transactional(readOnly = true)
	public VehicleSettingsSummary getVehicleSettingsSummary(long familyId) {
		List<MemberInfo> members = loadMemberUseCase.findMembers(familyId);
		String guardianNameMasked = members.stream()
				.filter(member -> member.role() == Role.GUARDIAN)
				.findFirst()
				.map(member -> GuardianNameMasker.mask(member.name()))
				.orElse(null);
		List<VehicleChild> children = members.stream()
				.filter(member -> member.role() == Role.CHILD)
				.map(member -> new VehicleChild(member.userId(), member.name()))
				.toList();
		return new VehicleSettingsSummary(guardianNameMasked, children,
				loadFamilyUseCase.findFamily(familyId).joinCallMode());
	}
}
