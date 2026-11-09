package com.example.pickupservice.auth.adapter.out;

import com.example.pickupservice.auth.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.auth.application.port.out.DeviceReplacedEvent;
import com.example.pickupservice.auth.application.port.out.PublishAuthEventPort;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.in.ActivePickupQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RecordApplicationEvents
@RequiredArgsConstructor
class AuthModuleLinkAdaptersTest extends IntegrationTestSupport {

	private static final String OUT_ADAPTER_PACKAGE = "com.example.pickupservice.auth.adapter.out.";

	private final ApplicationContext applicationContext;

	@MockitoBean
	private ActivePickupQueryUseCase activePickupQueryUseCase;

	@Test
	@DisplayName("PublishAuthEventPort는 이전 기기 id를 담은 스프링 이벤트를 발행한다")
	void publishPortPublishesDeviceReplacedEvent(ApplicationEvents applicationEvents) {
		PublishAuthEventPort publishPort = onlyBeanOf(PublishAuthEventPort.class);

		publishPort.publishDeviceReplaced(List.of(1L, 2L));

		List<DeviceReplacedEvent> events = applicationEvents.stream(DeviceReplacedEvent.class).toList();
		assertThat(events).hasSize(1);
		assertThat(events.getFirst().previousDeviceIds()).containsExactly(1L, 2L);
	}

	@Test
	@DisplayName("auth의 CheckActivePickupPort · PublishAuthEventPort는 각각 빈이 정확히 1개이고 auth.adapter.out 아래 클래스가 구현한다")
	void eachPortHasExactlyOneBeanInAuthOutAdapterPackage() {
		for (Class<?> port : List.of(CheckActivePickupPort.class, PublishAuthEventPort.class)) {
			Object bean = onlyBeanOf(port);
			assertThat(AopUtils.getTargetClass(bean).getName())
					.as("%s 구현 클래스 위치", port.getSimpleName())
					.startsWith(OUT_ADAPTER_PACKAGE);
		}
	}

	@Test
	@DisplayName("auth의 CheckActivePickupPort.hasActivePickup은 pickup ActivePickupQueryUseCase.hasActivePickup 결과를 그대로 돌려준다")
	void activePickupPortDelegatesHasActivePickupToPickupUseCase() {
		CheckActivePickupPort activePickupPort = onlyBeanOf(CheckActivePickupPort.class);
		given(activePickupQueryUseCase.hasActivePickup(77L)).willReturn(true);
		given(activePickupQueryUseCase.hasActivePickup(78L)).willReturn(false);

		assertThat(activePickupPort.hasActivePickup(77L)).isTrue();
		assertThat(activePickupPort.hasActivePickup(78L)).isFalse();
		verify(activePickupQueryUseCase).hasActivePickup(77L);
		verify(activePickupQueryUseCase).hasActivePickup(78L);
	}

	@Test
	@DisplayName("publishDeviceReplaced는 부를 때마다 DeviceReplacedEvent를 1건씩 발행하고 각 이벤트는 넘긴 id 목록을 순서대로 담는다")
	void publishPortPublishesOneEventPerCallWithGivenIds(ApplicationEvents applicationEvents) {
		PublishAuthEventPort publishPort = onlyBeanOf(PublishAuthEventPort.class);

		publishPort.publishDeviceReplaced(List.of(10L));
		publishPort.publishDeviceReplaced(List.of(30L, 20L));

		List<DeviceReplacedEvent> events = applicationEvents.stream(DeviceReplacedEvent.class).toList();
		assertThat(events).hasSize(2);
		assertThat(events.get(0).previousDeviceIds()).containsExactly(10L);
		assertThat(events.get(1).previousDeviceIds()).containsExactly(30L, 20L);
	}

	private <T> T onlyBeanOf(Class<T> type) {
		Map<String, T> beans = applicationContext.getBeansOfType(type);
		assertThat(beans).as("%s 빈 개수", type.getName()).hasSize(1);
		return beans.values().iterator().next();
	}
}
