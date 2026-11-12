package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
class GangnamReferenceDataLoaderTest extends IntegrationTestSupport {

	private static final int PARKING_ROWS = 60;
	private static final int CURB_ROWS = 3_147;

	private final GangnamReferenceDataLoader loader;
	private final JdbcTemplate jdbcTemplate;

	/** 테스트가 테이블을 비우거나 바꿨어도 끝나면 CSV 상태(60행 · 3,147행)로 되돌린다. */
	@AfterEach
	void restoreReferenceData() {
		jdbcTemplate.update("delete from public_parking");
		jdbcTemplate.update("delete from curb_stop_candidate");
		loader.load();
	}

	@Test
	@DisplayName("서버가 뜬 뒤 public_parking은 60행, curb_stop_candidate는 3,147행이다")
	void tablesAreFilledFromCsvWhenContextStarts() {
		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS);
		assertThat(count("curb_stop_candidate")).isEqualTo(CURB_ROWS);
	}

	@Test
	@DisplayName("주차장 122-1-000027은 name 한티역 · 좌표 · spaces 33 · baseMinutes 5 · baseFeeWon 400으로 들어간다")
	void parkingSampleRowHasCsvValues() {
		Map<String, Object> row = jdbcTemplate.queryForMap("""
				select name, st_y(location::geometry) as lat, st_x(location::geometry) as lng,
				       spaces, base_minutes, base_fee_won
				from public_parking where parking_id = '122-1-000027'
				""");

		assertThat(row.get("name")).isEqualTo("한티역");
		assertThat(((Number) row.get("lat")).doubleValue()).isCloseTo(37.4981503, within(0.0000001));
		assertThat(((Number) row.get("lng")).doubleValue()).isCloseTo(127.0506429, within(0.0000001));
		assertThat(row.get("spaces")).isEqualTo(33);
		assertThat(row.get("base_minutes")).isEqualTo(5);
		assertThat(row.get("base_fee_won")).isEqualTo(400);
	}

	@Test
	@DisplayName("길가 후보 1번은 DOTTED_YELLOW · 봉은사로16길 · 좌표 · 단속 카메라 없음으로 들어간다")
	void curbSampleRowHasCsvValues() {
		Map<String, Object> row = jdbcTemplate.queryForMap("""
				select road_name, line_kind, near_enforcement_cctv,
				       st_y(location::geometry) as lat, st_x(location::geometry) as lng
				from curb_stop_candidate where candidate_id = 1
				""");

		assertThat(row.get("road_name")).isEqualTo("봉은사로16길");
		assertThat(row.get("line_kind")).isEqualTo("DOTTED_YELLOW");
		assertThat(row.get("near_enforcement_cctv")).isEqualTo(false);
		assertThat(((Number) row.get("lat")).doubleValue()).isCloseTo(37.505018, within(0.0000001));
		assertThat(((Number) row.get("lng")).doubleValue()).isCloseTo(127.029508, within(0.0000001));
	}

	@Test
	@DisplayName("near_enforcement_cctv가 Y인 14번은 true로 들어가고 Y인 행은 718개다")
	void nearEnforcementCctvYBecomesTrue() {
		Boolean cctv14 = jdbcTemplate.queryForObject(
				"select near_enforcement_cctv from curb_stop_candidate where candidate_id = 14", Boolean.class);

		assertThat(cctv14).isTrue();
		assertThat(countWhere("curb_stop_candidate", "near_enforcement_cctv")).isEqualTo(718);
	}

	@Test
	@DisplayName("road_name이 빈 47번은 NULL로 들어가고 빈 행은 812개다")
	void emptyRoadNameBecomesNull() {
		String roadName = jdbcTemplate.queryForObject(
				"select road_name from curb_stop_candidate where candidate_id = 47", String.class);

		assertThat(roadName).isNull();
		assertThat(countWhere("curb_stop_candidate", "road_name is null")).isEqualTo(812);
	}

	@Test
	@DisplayName("line_kind별 행 수는 DOTTED_YELLOW 984 · NONE 2,163이다")
	void curbRowsPerLineKind() {
		assertThat(countWhere("curb_stop_candidate", "line_kind = 'DOTTED_YELLOW'")).isEqualTo(984);
		assertThat(countWhere("curb_stop_candidate", "line_kind = 'NONE'")).isEqualTo(2_163);
	}

	@Test
	@DisplayName("load를 한 번 더 불러도 두 테이블의 행 수가 그대로다")
	void loadAgainKeepsRowCounts() {
		loader.load();

		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS);
		assertThat(count("curb_stop_candidate")).isEqualTo(CURB_ROWS);
	}

	@Test
	@DisplayName("이미 행이 있는 테이블은 건드리지 않는다: 한 행을 지우고 load해도 다시 채우지 않고 바꾼 값도 그대로다")
	void loadDoesNotTouchNonEmptyTables() {
		jdbcTemplate.update("delete from public_parking where parking_id = '122-1-000027'");
		jdbcTemplate.update("delete from curb_stop_candidate where candidate_id = 1");
		jdbcTemplate.update("update public_parking set name = '바뀐 이름' where parking_id = '122-2-000005'");

		loader.load();

		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS - 1);
		assertThat(count("curb_stop_candidate")).isEqualTo(CURB_ROWS - 1);
		assertThat(jdbcTemplate.queryForObject(
				"select name from public_parking where parking_id = '122-2-000005'", String.class))
				.isEqualTo("바뀐 이름");
	}

	@Test
	@DisplayName("public_parking만 비우고 load하면 그 테이블만 60행으로 다시 채우고 curb_stop_candidate는 그대로다")
	void loadRefillsOnlyEmptiedParkingTable() {
		jdbcTemplate.update("delete from public_parking");
		jdbcTemplate.update("delete from curb_stop_candidate where candidate_id = 1");

		loader.load();

		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS);
		assertThat(count("curb_stop_candidate")).isEqualTo(CURB_ROWS - 1);
	}

	@Test
	@DisplayName("curb_stop_candidate만 비우고 load하면 그 테이블만 3,147행으로 다시 채우고 public_parking은 그대로다")
	void loadRefillsOnlyEmptiedCurbTable() {
		jdbcTemplate.update("delete from curb_stop_candidate");
		jdbcTemplate.update("delete from public_parking where parking_id = '122-1-000027'");

		loader.load();

		assertThat(count("curb_stop_candidate")).isEqualTo(CURB_ROWS);
		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS - 1);
	}

	@Test
	@DisplayName("두 테이블을 모두 비우고 load하면 60행 · 3,147행으로 다시 채운다")
	void loadRefillsBothEmptiedTables() {
		jdbcTemplate.update("delete from public_parking");
		jdbcTemplate.update("delete from curb_stop_candidate");

		loader.load();

		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS);
		assertThat(count("curb_stop_candidate")).isEqualTo(CURB_ROWS);
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
	}

	private long countWhere(String table, String condition) {
		return jdbcTemplate.queryForObject("select count(*) from " + table + " where " + condition, Long.class);
	}
}
