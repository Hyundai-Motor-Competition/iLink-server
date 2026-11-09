package com.example.pickupservice.schedule.adapter.out.map;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.schedule.application.port.out.SearchAcademyPort;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class KakaoMapSearchAdapterContextTest extends IntegrationTestSupport {

	private final ApplicationContext applicationContext;

	@Test
	@DisplayName("카카오 키가 없어도 컨텍스트가 뜨고 KakaoMapSearchAdapter가 SearchAcademyPort의 유일한 빈이다")
	void kakaoMapSearchAdapterIsTheOnlySearchAcademyPortBean() {
		Map<String, SearchAcademyPort> beans = applicationContext.getBeansOfType(SearchAcademyPort.class);

		assertThat(beans).hasSize(1);
		assertThat(AopUtils.getTargetClass(beans.values().iterator().next()))
				.isEqualTo(KakaoMapSearchAdapter.class);
	}
}
