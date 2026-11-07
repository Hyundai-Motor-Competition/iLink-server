package com.example.pickupservice.schedule.fixture;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

/**
 * in 포트 record의 joinCallMode 값을 컴포넌트 타입에서 이름으로 찾는다.
 * 테스트 코드가 JoinCallMode 타입을 직접 쓰지 않으므로 record 컴포넌트 타입이 바뀌어도 컴파일된다.
 */
public final class JoinCallModeFixture {

	private JoinCallModeFixture() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <E extends Enum<E>> E joinCallModeOf(Class<? extends Record> recordType, String name) {
		Class type = Arrays.stream(recordType.getRecordComponents())
				.filter(component -> component.getName().equals("joinCallMode"))
				.map(RecordComponent::getType)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(recordType.getName() + "에 joinCallMode가 없습니다"));
		return (E) Enum.valueOf(type, name);
	}
}
