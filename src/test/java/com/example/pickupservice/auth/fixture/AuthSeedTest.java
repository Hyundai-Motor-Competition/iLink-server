package com.example.pickupservice.auth.fixture;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Import({UserSeed.class, AuthSeed.class})
class AuthSeedTest extends IntegrationTestSupport {

	private final AuthSeed authSeed;
	private final JdbcTemplate jdbcTemplate;

	private AuthSeed.Result seeded;

	@AfterEach
	void cleanUp() {
		if (seeded != null) {
			authSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("테스트 시드로 연결 전 차량 기기 1개와 연결된 차량 기기 1개를 만든다")
	void seedCreatesUnlinkedVehicleDeviceAndLinkedVehicleDevice() {
		seeded = authSeed.seed();

		Map<String, Object> unlinkedVehicle = jdbcTemplate.queryForMap(
				"select family_id, vin, unlinked_at from vehicle where vehicle_id = ?", seeded.unlinkedVehicleId());
		assertThat(unlinkedVehicle.get("family_id")).isNull();
		assertThat(unlinkedVehicle.get("unlinked_at")).isNull();
		assertThat(unlinkedVehicle.get("vin")).isEqualTo(seeded.unlinkedVin());
		assertThat(seeded.unlinkedVin()).matches("[A-HJ-NPR-Z0-9]{17}");

		Map<String, Object> linkedVehicle = jdbcTemplate.queryForMap(
				"select family_id, unlinked_at from vehicle where vehicle_id = ?", seeded.linkedVehicleId());
		assertThat(((Number) linkedVehicle.get("family_id")).longValue()).isEqualTo(seeded.familyId());
		assertThat(linkedVehicle.get("unlinked_at")).isNull();

		Integer unlinkedCount = jdbcTemplate.queryForObject(
				"select count(*) from vehicle where vehicle_id in (?, ?) and family_id is null",
				Integer.class, seeded.unlinkedVehicleId(), seeded.linkedVehicleId());
		Integer linkedCount = jdbcTemplate.queryForObject(
				"select count(*) from vehicle where vehicle_id in (?, ?) and family_id is not null",
				Integer.class, seeded.unlinkedVehicleId(), seeded.linkedVehicleId());
		assertThat(unlinkedCount).isEqualTo(1);
		assertThat(linkedCount).isEqualTo(1);

		List<Map<String, Object>> devices = jdbcTemplate.queryForList("""
				select device_id, owner_type, owner_id, platform, refresh_token_hash, revoked_at
				from device
				where device_id in (?, ?)
				order by device_id
				""", seeded.unlinkedDeviceId(), seeded.linkedDeviceId());
		assertThat(devices).hasSize(2);
		assertThat(devices).allSatisfy(device -> {
			assertThat(device.get("owner_type")).isEqualTo("VEHICLE");
			assertThat(device.get("platform")).isEqualTo("VEHICLE");
			assertThat((String) device.get("refresh_token_hash")).matches("[0-9a-f]{64}");
			assertThat(device.get("revoked_at")).isNull();
		});

		Map<String, Object> unlinkedDevice = deviceRow(devices, seeded.unlinkedDeviceId());
		assertThat(((Number) unlinkedDevice.get("owner_id")).longValue()).isEqualTo(seeded.unlinkedVehicleId());
		assertThat(unlinkedDevice.get("refresh_token_hash")).isEqualTo(AuthSeed.sha256(seeded.unlinkedRefreshToken()));

		Map<String, Object> linkedDevice = deviceRow(devices, seeded.linkedDeviceId());
		assertThat(((Number) linkedDevice.get("owner_id")).longValue()).isEqualTo(seeded.linkedVehicleId());
		assertThat(linkedDevice.get("refresh_token_hash")).isEqualTo(AuthSeed.sha256(seeded.linkedRefreshToken()));
	}

	private static Map<String, Object> deviceRow(List<Map<String, Object>> devices, long deviceId) {
		return devices.stream()
				.filter(device -> ((Number) device.get("device_id")).longValue() == deviceId)
				.findFirst()
				.orElseThrow();
	}

}
