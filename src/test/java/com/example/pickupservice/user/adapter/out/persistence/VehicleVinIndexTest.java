package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
class VehicleVinIndexTest extends IntegrationTestSupport {

	private static final Instant UNLINKED_AT = Instant.parse("2026-11-03T09:12:34Z");

	private final JdbcTemplate jdbcTemplate;
	private final SaveVehiclePort saveVehiclePort;

	private final List<String> vins = new ArrayList<>();

	@AfterEach
	void clear() {
		vins.forEach(vin -> jdbcTemplate.update("delete from vehicle where vin = ?", vin));
	}

	@Test
	@DisplayName("서버가 뜨면 schema.sql의 VIN 부분 유니크 인덱스가 vehicle 테이블에 만들어진다")
	void partialUniqueIndexOnLinkedVinIsCreatedOnVehicleTable() {
		List<String> indexDefinitions = jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = 'vehicle'
				""", String.class);

		assertThat(indexDefinitions)
				.filteredOn(definition -> definition.startsWith("CREATE UNIQUE INDEX")
						&& definition.contains("(vin)")
						&& definition.contains("WHERE (unlinked_at IS NULL)"))
				.hasSize(1);
	}

	@Test
	@DisplayName("unlinkedAt이 빈 같은 VIN 차량 행을 두 개 저장하면 유니크 위반이 난다")
	void savingTwoLinkedVehiclesWithSameVinViolatesUniqueIndex() {
		String vin = randomVin();
		Vehicle first = saveVehiclePort.saveVehicle(Vehicle.register(vin, "아이오닉 5"));
		assertThat(first.getVehicleId()).as("첫 행 vehicleId").isNotNull();
		assertThat(first.getUnlinkedAt()).as("첫 행 unlinkedAt").isNull();

		assertThatThrownBy(() -> saveVehiclePort.saveVehicle(Vehicle.register(vin, "아이오닉 5")))
				.isExactlyInstanceOf(DataIntegrityViolationException.class)
				.hasStackTraceContaining("uk_vehicle_vin_linked");

		assertThat(countRowsOf(vin)).as("같은 VIN 행 수").isEqualTo(1);
	}

	@Test
	@DisplayName("같은 VIN이라도 기존 행의 unlinkedAt이 채워져 있으면 새 행이 저장된다")
	void savingSameVinSucceedsWhenExistingRowIsUnlinked() {
		String vin = randomVin();
		Vehicle first = saveVehiclePort.saveVehicle(Vehicle.register(vin, "아이오닉 5"));
		first.unlink(UNLINKED_AT);
		Vehicle unlinked = saveVehiclePort.saveVehicle(first);
		assertThat(unlinked.getUnlinkedAt()).as("해제된 첫 행 unlinkedAt").isEqualTo(UNLINKED_AT);

		Vehicle second = saveVehiclePort.saveVehicle(Vehicle.register(vin, "아이오닉 5"));

		assertThat(second.getVehicleId()).as("새 행 vehicleId").isNotNull()
				.isNotEqualTo(first.getVehicleId());
		assertThat(second.getUnlinkedAt()).as("새 행 unlinkedAt").isNull();
		assertThat(countRowsOf(vin)).as("같은 VIN 행 수").isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from vehicle where vin = ? and unlinked_at is null", Long.class, vin))
				.as("같은 VIN의 연결 중 행 수").isEqualTo(1);
	}

	private long countRowsOf(String vin) {
		return jdbcTemplate.queryForObject("select count(*) from vehicle where vin = ?", Long.class, vin);
	}

	private String randomVin() {
		String vin = "VIX" + String.format("%014d", ThreadLocalRandom.current().nextLong(100_000_000_000_000L));
		vins.add(vin);
		return vin;
	}

}
