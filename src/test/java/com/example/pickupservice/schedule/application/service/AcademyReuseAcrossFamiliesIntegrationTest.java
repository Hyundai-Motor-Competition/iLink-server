package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleUseCase;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class})
class AcademyReuseAcrossFamiliesIntegrationTest extends IntegrationTestSupport {

	private static final String SHARED_PLACE_ID = ScheduleSeed.ACADEMY_EXTERNAL_PLACE_ID;

	private static final AcademyRow FAMILY_A_ROW = new AcademyRow(ScheduleSeed.ACADEMY_NAME,
			ScheduleSeed.ACADEMY_ADDRESS, ScheduleSeed.ACADEMY_LAT, ScheduleSeed.ACADEMY_LNG,
			ScheduleSeed.ENTRANCE_LAT, ScheduleSeed.ENTRANCE_LNG, ScheduleSeed.ACADEMY_PHONE);

	private static final AcademyInput B_FIRST = new AcademyInput(SHARED_PLACE_ID, "가족B가 부르는 이름",
			"서울 강남구 역삼로 1", new LatLng(37.502000, 127.040500), new LatLng(37.502100, 127.040600),
			"02-111-2222");

	private static final AcademyInput B_SECOND = new AcademyInput(SHARED_PLACE_ID, "가족B가 고친 이름",
			"서울 강남구 역삼로 2", new LatLng(37.503000, 127.041500), new LatLng(37.503100, 127.041600),
			"02-333-4444");

	private final CreateScheduleUseCase createScheduleUseCase;
	private final UpdateScheduleUseCase updateScheduleUseCase;
	private final ScheduleSeed scheduleSeed;
	private final UserSeed userSeed;
	private final JdbcTemplate jdbcTemplate;

	private ScheduleSeed.Result familyA;
	private UserSeed.Result familyB;

	@BeforeEach
	void seed() {
		familyA = scheduleSeed.seed();
		familyB = userSeed.seed();
	}

	@AfterEach
	void clear() {
		jdbcTemplate.update("delete from pickup_schedule where child_user_id in (?, ?)", familyB.minjunId(),
				familyB.seoyunId());
		jdbcTemplate.update("delete from academy where family_id = ?", familyB.familyId());
		userSeed.clear(familyB);
		scheduleSeed.clear(familyA);
	}

	@Test
	@DisplayName("가족 A의 학원이 있는 externalPlaceId로 가족 B가 다른 이름 · 주소 · 위치 · 출입구 · 대표번호를 담아 일정을 등록하면 B는 새 academyId(B 가족 소유)를 받고 A 학원 행의 값은 그대로다")
	void createScheduleOfOtherFamilyWithSamePlaceDoesNotOverwriteFirstFamilyAcademy() {
		ScheduleResult created = createScheduleUseCase.createSchedule(createCommand(B_FIRST));

		assertThat(created.academyId()).as("B가 받은 academyId").isNotEqualTo(familyA.academyId());
		assertThat(familyIdOf(created.academyId())).as("B 학원의 family_id").isEqualTo(familyB.familyId());
		assertThat(academyRow(familyA.academyId())).as("A 학원 행").isEqualTo(FAMILY_A_ROW);
		assertThat(academyRow(created.academyId())).as("B 학원 행").isEqualTo(AcademyRow.of(B_FIRST));
		assertThat(academyCount()).as("externalPlaceId의 학원 행 수(A · B 각 1)").isEqualTo(2);
	}

	@Test
	@DisplayName("가족 B가 일정 수정으로 같은 externalPlaceId 학원의 이름 · 주소 · 위치 · 출입구 · 대표번호를 바꾸면 B의 학원만 바뀌고 가족 A 학원 행의 값은 그대로다")
	void updateScheduleOfOtherFamilyWithSamePlaceChangesOnlyItsOwnAcademy() {
		ScheduleResult created = createScheduleUseCase.createSchedule(createCommand(B_FIRST));

		ScheduleResult updated = updateScheduleUseCase.updateSchedule(new UpdateScheduleCommand(familyB.familyId(),
				created.scheduleId(), B_SECOND, Set.of(DayOfWeek.TUESDAY), LocalTime.of(17, 0), 5));

		assertThat(updated.academyId()).as("수정 뒤 B의 academyId").isEqualTo(created.academyId());
		assertThat(updated.academyId()).as("수정 뒤 B의 academyId는 A와 다름").isNotEqualTo(familyA.academyId());
		assertThat(academyRow(created.academyId())).as("B 학원 행").isEqualTo(AcademyRow.of(B_SECOND));
		assertThat(academyRow(familyA.academyId())).as("A 학원 행").isEqualTo(FAMILY_A_ROW);
		assertThat(academyCount()).as("externalPlaceId의 학원 행 수(A · B 각 1)").isEqualTo(2);
	}

	private CreateScheduleCommand createCommand(AcademyInput academy) {
		return new CreateScheduleCommand(familyB.familyId(), familyB.minjunId(), academy, Set.of(DayOfWeek.TUESDAY),
				LocalTime.of(17, 0), 5);
	}

	private Long familyIdOf(long academyId) {
		return jdbcTemplate.queryForObject("select family_id from academy where academy_id = ?", Long.class,
				academyId);
	}

	private Integer academyCount() {
		return jdbcTemplate.queryForObject(
				"select count(*) from academy where external_place_id = ? and family_id in (?, ?)", Integer.class,
				SHARED_PLACE_ID, familyA.user().familyId(), familyB.familyId());
	}

	private AcademyRow academyRow(long academyId) {
		return jdbcTemplate.queryForObject("""
				select name, address,
				       st_y(location::geometry) as lat, st_x(location::geometry) as lng,
				       st_y(entrance::geometry) as entrance_lat, st_x(entrance::geometry) as entrance_lng,
				       phone
				from academy where academy_id = ?
				""", (rs, rowNum) -> new AcademyRow(rs.getString("name"), rs.getString("address"),
				rs.getDouble("lat"), rs.getDouble("lng"), rs.getDouble("entrance_lat"), rs.getDouble("entrance_lng"),
				rs.getString("phone")), academyId);
	}

	private record AcademyRow(String name, String address, double lat, double lng, double entranceLat,
			double entranceLng, String phone) {

		static AcademyRow of(AcademyInput input) {
			return new AcademyRow(input.name(), input.address(), input.location().lat(), input.location().lng(),
					input.entrance().lat(), input.entrance().lng(), input.phone());
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof AcademyRow that)) {
				return false;
			}
			double tolerance = 1e-9;
			return name.equals(that.name) && address.equals(that.address)
					&& Math.abs(lat - that.lat) < tolerance && Math.abs(lng - that.lng) < tolerance
					&& Math.abs(entranceLat - that.entranceLat) < tolerance
					&& Math.abs(entranceLng - that.entranceLng) < tolerance && phone.equals(that.phone);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

}
