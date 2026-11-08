package com.example.pickupservice.place.application.service;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** pickup_spot의 (family_id, academy_id) WHERE is_default 부분 유니크 인덱스 정의를 찾는다. */
final class DefaultSpotIndexes {

	private DefaultSpotIndexes() {
	}

	static List<String> of(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = 'pickup_spot'
				""", String.class).stream()
				.filter(definition -> definition.startsWith("CREATE UNIQUE INDEX")
						&& definition.contains("(family_id, academy_id)")
						&& (definition.endsWith("WHERE is_default")
								|| definition.endsWith("WHERE (is_default = true)")))
				.toList();
	}
}
