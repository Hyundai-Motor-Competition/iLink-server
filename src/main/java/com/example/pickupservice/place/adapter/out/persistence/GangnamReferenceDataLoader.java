package com.example.pickupservice.place.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GangnamReferenceDataLoader implements ApplicationRunner {

	private static final int BATCH_SIZE = 500;
	private static final String PARKING_CSV = "reference/gangnam/parking_public.csv";
	private static final String CURB_CSV = "reference/gangnam/curb_stop_candidates.csv";

	private static final String PARKING_SQL = """
			insert into public_parking (parking_id, name, location, spaces, base_minutes, base_fee_won,
			                            created_at, updated_at)
			values (?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, ?, ?, ?, ?)
			""";
	private static final String CURB_SQL = """
			insert into curb_stop_candidate (candidate_id, location, road_name, line_kind, near_enforcement_cctv,
			                                 created_at, updated_at)
			values (?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, ?, ?, ?, ?)
			""";

	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	@Override
	public void run(ApplicationArguments args) {
		load();
	}

	public void load() {
		if (isEmpty("public_parking")) {
			transactionTemplate.executeWithoutResult(status -> loadParking());
		}
		if (isEmpty("curb_stop_candidate")) {
			transactionTemplate.executeWithoutResult(status -> loadCurb());
		}
	}

	private boolean isEmpty(String table) {
		Long count = jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
		return count == null || count == 0;
	}

	private void loadParking() {
		Timestamp now = Timestamp.from(Instant.now());
		List<Object[]> rows = new ArrayList<>();
		for (Csv.Row row : Csv.read(PARKING_CSV)) {
			rows.add(new Object[] {
					row.get("parking_id"), row.get("name"),
					Double.valueOf(row.get("longitude")), Double.valueOf(row.get("latitude")),
					integerOrNull(row.get("spaces")), integerOrNull(row.get("base_minutes")),
					integerOrNull(row.get("base_fee_won")), now, now });
		}
		int[] types = { Types.VARCHAR, Types.VARCHAR, Types.DOUBLE, Types.DOUBLE, Types.INTEGER, Types.INTEGER,
				Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP };
		batchInsert(PARKING_SQL, rows, types);
	}

	private void loadCurb() {
		Timestamp now = Timestamp.from(Instant.now());
		List<Object[]> rows = new ArrayList<>();
		for (Csv.Row row : Csv.read(CURB_CSV)) {
			String roadName = row.get("road_name");
			rows.add(new Object[] {
					Long.valueOf(row.get("candidate_id")),
					Double.valueOf(row.get("longitude")), Double.valueOf(row.get("latitude")),
					roadName.isEmpty() ? null : roadName, row.get("line_kind"),
					"Y".equals(row.get("near_enforcement_cctv")), now, now });
		}
		int[] types = { Types.BIGINT, Types.DOUBLE, Types.DOUBLE, Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN,
				Types.TIMESTAMP, Types.TIMESTAMP };
		batchInsert(CURB_SQL, rows, types);
	}

	private void batchInsert(String sql, List<Object[]> rows, int[] types) {
		jdbcTemplate.batchUpdate(sql, rows, BATCH_SIZE, (ps, args) -> {
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null) {
					ps.setNull(i + 1, types[i]);
				}
				else {
					ps.setObject(i + 1, args[i], types[i]);
				}
			}
		});
	}

	private static Integer integerOrNull(String value) {
		return value.isEmpty() ? null : Integer.valueOf(value);
	}

	private static final class Csv {

		private Csv() {
		}

		static List<Row> read(String classpathLocation) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
					new ClassPathResource(classpathLocation).getInputStream(), StandardCharsets.UTF_8))) {
				String header = reader.readLine();
				if (header == null) {
					return List.of();
				}
				if (header.startsWith("\uFEFF")) {
					header = header.substring(1);
				}
				List<String> names = Arrays.asList(header.split(",", -1));
				List<Row> rows = new ArrayList<>();
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank()) {
						continue;
					}
					rows.add(new Row(names, line.split(",", -1)));
				}
				return rows;
			}
			catch (IOException e) {
				throw new UncheckedIOException("참조 CSV를 읽지 못했습니다: " + classpathLocation, e);
			}
		}

		record Row(List<String> names, String[] cells) {

			String get(String column) {
				int index = names.indexOf(column);
				if (index < 0 || index >= cells.length) {
					return "";
				}
				return cells[index].trim();
			}
		}
	}

}
