package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
class GangnamReferenceDataLoaderRollbackTest extends IntegrationTestSupport {

	private static final int PARKING_ROWS = 60;
	private static final int CURB_ROWS = 3_147;

	private final GangnamReferenceDataLoader loader;
	private final JdbcTemplate jdbcTemplate;

	@BeforeEach
	void emptyTablesAndInstallFailingTrigger() {
		jdbcTemplate.update("delete from public_parking");
		jdbcTemplate.update("delete from curb_stop_candidate");
		installFailingTrigger();
	}

	@AfterEach
	void removeTriggerAndRestoreReferenceData() {
		dropFailingTrigger();
		jdbcTemplate.update("delete from public_parking");
		jdbcTemplate.update("delete from curb_stop_candidate");
		loader.load();
	}

	@Test
	@DisplayName("curb_stop_candidate 적재가 뒤쪽 묶음에서 실패하면 예외를 던지고 그 테이블에는 한 행도 남지 않는다")
	void failedCurbLoadLeavesNoPartialRows() {
		assertThatThrownBy(loader::load).isInstanceOf(RuntimeException.class);

		assertThat(count("curb_stop_candidate")).isZero();
	}

	@Test
	@DisplayName("curb_stop_candidate 적재가 실패해도 이미 끝난 public_parking 60행은 그대로 남는다(테이블별 트랜잭션)")
	void failedCurbLoadKeepsAlreadyLoadedParkingRows() {
		assertThatThrownBy(loader::load).isInstanceOf(RuntimeException.class);

		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS);
		assertThat(count("curb_stop_candidate")).isZero();
	}

	@Test
	@DisplayName("실패 원인을 없애고 다시 load하면 curb_stop_candidate가 3,147행으로 채워지고 public_parking은 60행 그대로다")
	void loadAfterRemovingFailureCauseFillsAllRows() {
		assertThatThrownBy(loader::load).isInstanceOf(RuntimeException.class);

		dropFailingTrigger();
		loader.load();

		assertThat(count("curb_stop_candidate")).isEqualTo(CURB_ROWS);
		assertThat(count("public_parking")).isEqualTo(PARKING_ROWS);
	}

	private void installFailingTrigger() {
		jdbcTemplate.execute("""
				create or replace function fail_curb_candidate_3000() returns trigger as $$
				begin
				    if new.candidate_id = 3000 then
				        raise exception 'test: candidate 3000 insert rejected';
				    end if;
				    return new;
				end;
				$$ language plpgsql
				""");
		jdbcTemplate.execute("drop trigger if exists fail_curb_candidate_3000_trg on curb_stop_candidate");
		jdbcTemplate.execute("""
				create trigger fail_curb_candidate_3000_trg
				before insert on curb_stop_candidate
				for each row execute function fail_curb_candidate_3000()
				""");
	}

	private void dropFailingTrigger() {
		jdbcTemplate.execute("drop trigger if exists fail_curb_candidate_3000_trg on curb_stop_candidate");
		jdbcTemplate.execute("drop function if exists fail_curb_candidate_3000()");
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
	}
}
