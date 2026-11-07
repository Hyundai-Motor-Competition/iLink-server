package com.example.pickupservice.user.adapter.out;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.RevokeMemberDevicesPort;
import com.example.pickupservice.user.domain.DeviceStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@RequiredArgsConstructor
class UserModuleLinkAdaptersTest extends IntegrationTestSupport {

	private static final String OUT_ADAPTER_PACKAGE = "com.example.pickupservice.user.adapter.out.";

	private final ApplicationContext applicationContext;

	@Test
	@DisplayName("auth · pickup 구현 전 임시 어댑터가 빈으로 등록된다: LoadMemberDeviceStatusPort는 모든 userId에 NONE, RevokeMemberDevicesPort는 아무것도 하지 않음, CheckActivePickupPort는 false")
	void temporaryModuleAdaptersAreRegisteredWithPlaceholderBehavior() {
		LoadMemberDeviceStatusPort deviceStatusPort = onlyBeanOf(LoadMemberDeviceStatusPort.class);
		RevokeMemberDevicesPort revokePort = onlyBeanOf(RevokeMemberDevicesPort.class);
		CheckActivePickupPort activePickupPort = onlyBeanOf(CheckActivePickupPort.class);

		assertThat(deviceStatusPort.loadDeviceStatuses(List.of(1L, 2L)))
				.containsExactlyInAnyOrderEntriesOf(Map.of(1L, DeviceStatus.NONE, 2L, DeviceStatus.NONE));
		assertThatCode(() -> revokePort.revokeDevicesOf(1L)).doesNotThrowAnyException();
		assertThat(activePickupPort.isInActivePickup(1L)).isFalse();
		assertThat(activePickupPort.hasActivePickup(1L)).isFalse();
	}

	@Test
	@DisplayName("세 포트는 각각 빈이 정확히 1개이고 user.adapter.out 아래 클래스가 구현한다")
	void eachPortHasExactlyOneBeanInUserOutAdapterPackage() {
		for (Class<?> port : List.of(LoadMemberDeviceStatusPort.class, RevokeMemberDevicesPort.class, CheckActivePickupPort.class)) {
			Object bean = onlyBeanOf(port);
			assertThat(AopUtils.getTargetClass(bean).getName())
					.as("%s 구현 클래스 위치", port.getSimpleName())
					.startsWith(OUT_ADAPTER_PACKAGE);
		}
	}

	@Test
	@DisplayName("LoadMemberDeviceStatusPort는 요청한 userId마다 NONE을 담고 그 밖의 키는 담지 않는다")
	void deviceStatusPortReturnsNoneForEveryRequestedUserId() {
		LoadMemberDeviceStatusPort deviceStatusPort = onlyBeanOf(LoadMemberDeviceStatusPort.class);

		Map<Long, DeviceStatus> statuses = deviceStatusPort.loadDeviceStatuses(List.of(101L, 131L, 9_999_999L));

		assertThat(statuses).containsExactlyInAnyOrderEntriesOf(Map.of(
				101L, DeviceStatus.NONE,
				131L, DeviceStatus.NONE,
				9_999_999L, DeviceStatus.NONE));
	}

	@Test
	@DisplayName("LoadMemberDeviceStatusPort에 빈 목록을 주면 빈 Map을 돌려준다")
	void deviceStatusPortReturnsEmptyMapForEmptyList() {
		LoadMemberDeviceStatusPort deviceStatusPort = onlyBeanOf(LoadMemberDeviceStatusPort.class);

		assertThat(deviceStatusPort.loadDeviceStatuses(List.of())).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("RevokeMemberDevicesPort는 어떤 userId로 불러도 예외 없이 끝난다")
	void revokePortDoesNothing() {
		RevokeMemberDevicesPort revokePort = onlyBeanOf(RevokeMemberDevicesPort.class);

		assertThatCode(() -> {
			revokePort.revokeDevicesOf(1L);
			revokePort.revokeDevicesOf(9_999_999L);
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("CheckActivePickupPort는 isInActivePickup · hasActivePickup 모두 false를 돌려준다")
	void activePickupPortAlwaysReturnsFalse() {
		CheckActivePickupPort activePickupPort = onlyBeanOf(CheckActivePickupPort.class);

		assertThat(activePickupPort.isInActivePickup(101L)).isFalse();
		assertThat(activePickupPort.isInActivePickup(9_999_999L)).isFalse();
		assertThat(activePickupPort.hasActivePickup(77L)).isFalse();
		assertThat(activePickupPort.hasActivePickup(9_999_999L)).isFalse();
	}

	private <T> T onlyBeanOf(Class<T> type) {
		Map<String, T> beans = applicationContext.getBeansOfType(type);
		assertThat(beans).as("%s 빈 개수", type.getSimpleName()).hasSize(1);
		return beans.values().iterator().next();
	}
}
