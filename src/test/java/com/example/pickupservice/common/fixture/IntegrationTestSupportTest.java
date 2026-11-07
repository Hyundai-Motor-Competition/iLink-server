package com.example.pickupservice.common.fixture;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class IntegrationTestSupportTest extends IntegrationTestSupport {

	private static final double LAT = 37.501234;
	private static final double LNG = 127.039876;

	private final GeographySampleRepository geographySampleRepository;
	private final JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		geographySampleRepository.deleteAll();
	}

	@Test
	@DisplayName("공통 테스트 기반 클래스를 상속한 테스트는 PostGIS 3 확장이 켜진 PostgreSQL 17 컨테이너에 붙는다")
	void connectsToPostgresql17ContainerWithPostgis3() {
		String serverVersion = jdbcTemplate.queryForObject("show server_version", String.class);
		String postgisVersion = jdbcTemplate.queryForObject("select postgis_lib_version()", String.class);

		assertThat(serverVersion).startsWith("17.");
		assertThat(postgisVersion).startsWith("3.");
	}

	@Test
	@DisplayName("공통 테스트 기반 클래스를 상속한 테스트에서 geography(Point,4326)로 매핑한 엔티티 컬럼은 타입 Point · SRID 4326인 geography 컬럼으로 만들어진다")
	void geographyPointColumnIsCreatedWithPointTypeAndSrid4326() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
				select type, srid
				from geography_columns
				where f_table_name = 'geography_sample'
				  and f_geography_column = 'location'
				""");

		assertThat(columns).singleElement().satisfies(column -> assertThat(column)
				.containsEntry("type", "Point")
				.containsEntry("srid", 4326));
	}

	@Test
	@DisplayName("공통 테스트 기반 클래스를 상속한 테스트에서 GeoPoints.toPoint(37.501234, 127.039876)를 geography 컬럼에 저장하면 DB에 경도 127.039876 · 위도 37.501234 · SRID 4326으로 들어간다")
	void savedPointIsStoredAsLongitudeAndLatitudeWithSrid4326() {
		Long id = geographySampleRepository.save(new GeographySampleEntity(GeoPoints.toPoint(LAT, LNG))).getId();

		Map<String, Object> stored = jdbcTemplate.queryForMap("""
				select ST_X(location::geometry) as x, ST_Y(location::geometry) as y, ST_SRID(location) as srid
				from geography_sample
				where id = ?
				""", id);

		assertThat(stored)
				.containsEntry("x", LNG)
				.containsEntry("y", LAT)
				.containsEntry("srid", 4326);
	}

	@Test
	@DisplayName("공통 테스트 기반 클래스를 상속한 테스트에서 geography 컬럼에 저장한 값을 다시 읽어 GeoPoints.toLatLng로 바꾸면 SRID 4326 · lat 37.501234 · lng 127.039876이 그대로 나온다")
	void savedPointIsReadBackAsSameLatLngWithSrid4326() {
		Long id = geographySampleRepository.save(new GeographySampleEntity(GeoPoints.toPoint(LAT, LNG))).getId();

		GeographySampleEntity found = geographySampleRepository.findById(id).orElseThrow();

		assertThat(found.getLocation().getSRID()).isEqualTo(4326);
		LatLng latLng = GeoPoints.toLatLng(found.getLocation());
		assertThat(latLng.lat()).isEqualTo(LAT);
		assertThat(latLng.lng()).isEqualTo(LNG);
	}

	@Test
	@DisplayName("공통 IntegrationTestSupport는 도메인 시드를 Import하지 않고 시드가 필요한 테스트가 UserSeed · AuthSeed를 직접 Import한다")
	void integrationTestSupportDoesNotImportDomainSeeds() {
		Import imports = IntegrationTestSupport.class.getAnnotation(Import.class);

		assertThat(imports).isNotNull();
		assertThat(Arrays.stream(imports.value()).map(Class::getName))
				.isNotEmpty()
				.noneMatch(name -> name.startsWith("com.example.pickupservice.user."))
				.noneMatch(name -> name.startsWith("com.example.pickupservice.auth."));
	}

}
