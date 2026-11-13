package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.domain.RecommendedSpot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendPickupSpotsUseCaseTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 SPOT-04 · 05 in 포트 RecommendPickupSpotsUseCase는 interface이고 recommendSpots(RecommendSpotsQuery) · recommendNearbySpots(long)만 갖는다")
	void recommendUseCaseHasDesignSignature() throws NoSuchMethodException {
		assertThat(RecommendPickupSpotsUseCase.class.isInterface())
				.as("RecommendPickupSpotsUseCase는 interface여야 한다")
				.isTrue();
		List<String> abstractMethodNames = Arrays.stream(RecommendPickupSpotsUseCase.class.getDeclaredMethods())
				.filter(m -> Modifier.isAbstract(m.getModifiers()))
				.map(Method::getName)
				.toList();
		assertThat(abstractMethodNames).containsExactlyInAnyOrder("recommendSpots", "recommendNearbySpots");

		Method method = RecommendPickupSpotsUseCase.class.getMethod("recommendSpots", RecommendSpotsQuery.class);
		assertThat(method.getGenericReturnType().getTypeName()).isEqualTo(SpotRecommendations.class.getName());

		Method nearby = RecommendPickupSpotsUseCase.class.getMethod("recommendNearbySpots", long.class);
		assertThat(nearby.getGenericReturnType().getTypeName()).isEqualTo(NearbySpots.class.getName());
	}

	@Test
	@DisplayName("NearbySpots는 supported · items(List<NearbySpot>) 순서의 record이고 NearbySpot은 name · location · walkMeters · walkSeconds만 갖는다(driveSeconds · totalSeconds 없음)")
	void nearbySpotsHasDesignComponents() {
		assertRecord(NearbySpots.class,
				new String[] {"supported", "items"},
				new String[] {"boolean", List.class.getName() + "<" + NearbySpot.class.getName() + ">"});
		assertRecord(NearbySpot.class,
				new String[] {"name", "location", "walkMeters", "walkSeconds"},
				new String[] {String.class.getName(), LatLng.class.getName(), "int", "int"});
	}

	@Test
	@DisplayName("RecommendSpotsQuery는 academyId · origin · arriveAt(null 가능) 순서의 record다")
	void recommendSpotsQueryHasDesignComponents() {
		assertRecord(RecommendSpotsQuery.class,
				new String[] {"academyId", "origin", "arriveAt"},
				new String[] {"long", LatLng.class.getName(), Instant.class.getName()});
		RecommendSpotsQuery query = new RecommendSpotsQuery(1L, new LatLng(37.5, 127.0), null);
		assertThat(query.arriveAt()).isNull();
	}

	@Test
	@DisplayName("SpotRecommendations는 supported · items 순서의 record다")
	void spotRecommendationsHasDesignComponents() {
		assertRecord(SpotRecommendations.class,
				new String[] {"supported", "items"},
				new String[] {"boolean", List.class.getName() + "<" + RecommendedSpot.class.getName() + ">"});
	}

	private static void assertRecord(Class<?> type, String[] names, String[] types) {
		assertThat(type.isRecord()).as("%s는 record여야 한다", type.getSimpleName()).isTrue();
		RecordComponent[] actual = type.getRecordComponents();
		assertThat(Arrays.stream(actual).map(RecordComponent::getName).toList())
				.as("%s 컴포넌트 이름 · 순서", type.getSimpleName())
				.containsExactly(names);
		assertThat(Arrays.stream(actual).map(c -> c.getGenericType().getTypeName()).toList())
				.as("%s 컴포넌트 타입 · 순서", type.getSimpleName())
				.containsExactly(types);
	}
}
