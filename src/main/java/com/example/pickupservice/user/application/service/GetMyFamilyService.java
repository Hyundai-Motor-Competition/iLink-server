package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.text.PlateNumberMasker;
import com.example.pickupservice.user.application.port.in.FamilyResult;
import com.example.pickupservice.user.application.port.in.GetMyFamilyQuery;
import com.example.pickupservice.user.application.port.in.GetMyFamilyUseCase;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.in.VehicleSummary;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetMyFamilyService implements GetMyFamilyUseCase {

	private final LoadFamilyPort loadFamilyPort;
	private final LoadUserPort loadUserPort;
	private final LoadVehiclePort loadVehiclePort;
	private final LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;
	private final ProfileImageStoragePort profileImageStoragePort;

	@Override
	@Transactional(readOnly = true)
	public FamilyResult getMyFamily(GetMyFamilyQuery query) {
		Family family = loadFamilyPort.loadFamily(query.familyId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		List<MemberResult> members = MemberResults.withDeviceStatus(
				loadUserPort.loadUsers(query.familyId()), loadMemberDeviceStatusPort, this::profileImageUrl);
		VehicleSummary vehicle = loadLinkedVehicleSummary(query.familyId());
		return new FamilyResult(family.getFamilyId(), family.getJoinCallMode(), vehicle, members,
				family.isChildLocationEnabled());
	}

	private String profileImageUrl(User user) {
		String key = user.getProfileImageKey();
		Optional<String> url = key != null
				? profileImageStoragePort.presignedUrl(key)
				: profileImageStoragePort.defaultUrl();
		return url.orElse(null);
	}

	private VehicleSummary loadLinkedVehicleSummary(long familyId) {
		return loadVehiclePort.loadLinkedVehicleOfFamily(familyId)
				.map(linked -> new VehicleSummary(linked.getVehicleId(), linked.getModel(),
						PlateNumberMasker.mask(linked.getPlateNumber())))
				.orElse(null);
	}
}
