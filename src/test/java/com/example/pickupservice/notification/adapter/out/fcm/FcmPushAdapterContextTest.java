package com.example.pickupservice.notification.adapter.out.fcm;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class FcmPushAdapterContextTest extends IntegrationTestSupport {

	private final ApplicationContext applicationContext;

	@Test
	@DisplayName("Firebase 자격 증명이 없어도 컨텍스트가 뜨고 FcmPushAdapter가 SendPushPort의 유일한 빈이다")
	void fcmPushAdapterIsTheOnlySendPushPortBean() {
		Map<String, SendPushPort> beans = applicationContext.getBeansOfType(SendPushPort.class);

		assertThat(beans).hasSize(1);
		assertThat(AopUtils.getTargetClass(beans.values().iterator().next())).isEqualTo(FcmPushAdapter.class);
	}
}
