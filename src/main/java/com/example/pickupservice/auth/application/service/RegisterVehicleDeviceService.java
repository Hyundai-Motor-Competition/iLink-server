package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.RegisterVehicleDeviceCommand;
import com.example.pickupservice.auth.application.port.in.RegisterVehicleDeviceUseCase;
import com.example.pickupservice.auth.application.port.in.VehicleDeviceResult;
import com.example.pickupservice.auth.application.port.out.IssueTokenPort;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.RegisterVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RegisterVehicleDeviceService implements RegisterVehicleDeviceUseCase {

	private final LoadVehicleUseCase loadVehicleUseCase;
	private final RegisterVehicleUseCase registerVehicleUseCase;
	private final SaveDevicePort saveDevicePort;
	private final IssueTokenPort issueTokenPort;
	private final Clock clock;

	@Override
	@Transactional
	public VehicleDeviceResult registerVehicleDevice(RegisterVehicleDeviceCommand command) {
		long vehicleId = resolveVehicleId(command);
		Device device = saveDevicePort.saveDevice(Device.register(OwnerType.VEHICLE, vehicleId, Platform.VEHICLE,
				null, clock.instant(), PlaceholderRefreshHash.next()));
		IssuedTokens tokens = new DeviceTokenRotator(issueTokenPort, saveDevicePort).issueTo(device,
				new TokenSubject(PrincipalType.VEHICLE, vehicleId, null, null, device.getDeviceId()));
		return new VehicleDeviceResult(device.getDeviceId(), vehicleId, tokens.accessToken());
	}

	private long resolveVehicleId(RegisterVehicleDeviceCommand command) {
		VehicleInfo vehicle = loadVehicleUseCase.findLinkedVehicleByVin(command.vin()).orElse(null);
		if (vehicle == null) {
			return registerVehicleUseCase.registerVehicle(command.vin(), command.model());
		}
		if (vehicle.familyId() != null) {
			throw BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED);
		}
		return vehicle.vehicleId();
	}
}
