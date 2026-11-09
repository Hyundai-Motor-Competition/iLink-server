package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.notification.application.port.in.NotifyDeviceReplacedUseCase;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotifyDeviceReplacedService implements NotifyDeviceReplacedUseCase {

	private final LoadPushTargetPort loadPushTargetPort;
	private final SendPushPort sendPushPort;

	@Override
	public void notifyDeviceReplaced(List<Long> previousDeviceIds) {
		for (Long deviceId : previousDeviceIds) {
			loadPushTargetPort.loadPushTarget(deviceId).ifPresent(target ->
					sendPushPort.send(new PushMessage(
							target,
							"다른 기기에서 로그인했어요",
							"새 기기에서 로그인해서 이 기기는 로그아웃됐어요",
							Map.of())));
		}
	}
}
