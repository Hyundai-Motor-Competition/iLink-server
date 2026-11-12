package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.domain.CurbStopCandidate;
import com.example.pickupservice.place.domain.PublicParking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotRecommendOutPortsTest {

	private static final String LAT_LNG = LatLng.class.getName();
	private static final String LAT_LNG_LIST = List.class.getName() + "<" + LAT_LNG + ">";
	private static final String INTEGER_LIST = List.class.getName() + "<" + Integer.class.getName() + ">";

	@Test
	@DisplayName("핵심 로직 「시그니처」의 SPOT-04 out 포트 ServiceAreaPort는 interface이고 isSupported(LatLng)만 갖는다")
	void serviceAreaPortHasDesignSignature() throws NoSuchMethodException {
		assertInterfaceMethods(ServiceAreaPort.class, "isSupported");
		assertSignature(ServiceAreaPort.class.getMethod("isSupported", LatLng.class),
				"boolean", LAT_LNG);
	}

	@Test
	@DisplayName("LoadSpotCandidatePort는 interface이고 loadPublicParkingWithin · loadCurbStopsWithin만 갖는다")
	void loadSpotCandidatePortHasDesignSignatures() throws NoSuchMethodException {
		assertInterfaceMethods(LoadSpotCandidatePort.class, "loadCurbStopsWithin", "loadPublicParkingWithin");
		assertSignature(LoadSpotCandidatePort.class.getMethod("loadPublicParkingWithin", LatLng.class, int.class),
				List.class.getName() + "<" + PublicParking.class.getName() + ">", LAT_LNG, "int");
		assertSignature(LoadSpotCandidatePort.class.getMethod("loadCurbStopsWithin", LatLng.class, int.class),
				List.class.getName() + "<" + CurbStopCandidate.class.getName() + ">", LAT_LNG, "int");
	}

	@Test
	@DisplayName("DriveTimePort는 interface이고 driveSecondsNow · driveSecondsAt만 갖는다")
	void driveTimePortHasDesignSignatures() throws NoSuchMethodException {
		assertInterfaceMethods(DriveTimePort.class, "driveSecondsAt", "driveSecondsNow");
		assertSignature(DriveTimePort.class.getMethod("driveSecondsNow", LatLng.class, List.class),
				INTEGER_LIST, LAT_LNG, LAT_LNG_LIST);
		assertSignature(DriveTimePort.class.getMethod("driveSecondsAt", LatLng.class, List.class, Instant.class),
				INTEGER_LIST, LAT_LNG, LAT_LNG_LIST, Instant.class.getName());
	}

	private static void assertInterfaceMethods(Class<?> port, String... sortedMethodNames) {
		assertThat(port.isInterface()).as("%s는 interface여야 한다", port.getSimpleName()).isTrue();
		List<String> abstractMethodNames = Arrays.stream(port.getDeclaredMethods())
				.filter(m -> Modifier.isAbstract(m.getModifiers()))
				.map(Method::getName)
				.sorted()
				.toList();
		assertThat(abstractMethodNames)
				.as("%s의 추상 메서드 목록", port.getSimpleName())
				.containsExactly(sortedMethodNames);
	}

	private static void assertSignature(Method method, String returnType, String... parameterTypes) {
		assertThat(method.getGenericReturnType().getTypeName())
				.as("%s 반환 타입", method.getName())
				.isEqualTo(returnType);
		assertThat(Arrays.stream(method.getGenericParameterTypes()).map(Type::getTypeName).toList())
				.as("%s 파라미터 타입", method.getName())
				.containsExactly(parameterTypes);
	}
}
