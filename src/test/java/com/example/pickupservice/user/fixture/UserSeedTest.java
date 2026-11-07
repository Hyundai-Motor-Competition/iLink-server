package com.example.pickupservice.user.fixture;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RequiredArgsConstructor
class UserSeedTest extends IntegrationTestSupport {

	private final UserSeed userSeed;
	private final JdbcTemplate jdbcTemplate;

	private UserSeed.Result seeded;

	@AfterEach
	void cleanUp() {
		if (seeded != null) {
			userSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("테스트 시드로 가족 1개 · GUARDIAN 1명(김보호) · CHILD 2명(민준 · 서윤) · 연결된 차량 1대(아이오닉 5)를 만든다")
	void seedCreatesFamilyWithGuardianTwoChildrenAndLinkedVehicle() {
		seeded = userSeed.seed();

		Map<String, Object> family = jdbcTemplate.queryForMap(
				"select join_call_mode, settings_version from family where family_id = ?", seeded.familyId());
		assertThat(family)
				.containsEntry("join_call_mode", "AUTO")
				.containsEntry("settings_version", 0);

		List<Map<String, Object>> users = jdbcTemplate.queryForList(
				"select user_id, role, name from users where family_id = ? order by user_id", seeded.familyId());
		assertThat(users)
				.extracting(row -> ((Number) row.get("user_id")).longValue(), row -> row.get("role"), row -> row.get("name"))
				.containsExactly(
						tuple(seeded.guardianId(), "GUARDIAN", "김보호"),
						tuple(seeded.minjunId(), "CHILD", "민준"),
						tuple(seeded.seoyunId(), "CHILD", "서윤"));

		Integer phonelessChildren = jdbcTemplate.queryForObject(
				"select count(*) from users where family_id = ? and role = 'CHILD' and phone is null",
				Integer.class, seeded.familyId());
		assertThat(phonelessChildren).isEqualTo(2);
		Integer guardiansWithPhone = jdbcTemplate.queryForObject(
				"select count(*) from users where family_id = ? and role = 'GUARDIAN' and phone is not null",
				Integer.class, seeded.familyId());
		assertThat(guardiansWithPhone).isEqualTo(1);

		List<Map<String, Object>> vehicles = jdbcTemplate.queryForList(
				"select vehicle_id, vin, model, plate_number, unlinked_at from vehicle where family_id = ?",
				seeded.familyId());
		assertThat(vehicles).singleElement().satisfies(vehicle -> {
			assertThat(((Number) vehicle.get("vehicle_id")).longValue()).isEqualTo(seeded.vehicleId());
			assertThat(vehicle.get("vin")).isEqualTo(seeded.vin());
			assertThat(seeded.vin()).hasSize(17);
			assertThat(vehicle.get("model")).isEqualTo("아이오닉 5");
			assertThat(vehicle.get("plate_number")).isEqualTo("12가 3456");
			assertThat(vehicle.get("unlinked_at")).isNull();
		});
	}

}
