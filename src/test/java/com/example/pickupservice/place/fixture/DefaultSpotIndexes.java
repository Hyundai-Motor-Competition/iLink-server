package com.example.pickupservice.place.fixture;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public final class DefaultSpotIndexes {

	private DefaultSpotIndexes() {
	}

	public static List<String> partialUniqueOnFamilyAcademy(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = 'pickup_spot'
				""", String.class).stream()
				.filter(DefaultSpotIndexes::isPartialUniqueOnFamilyAcademyWhereDefault)
				.toList();
	}

	private static boolean isPartialUniqueOnFamilyAcademyWhereDefault(String definition) {
		return definition.startsWith("CREATE UNIQUE INDEX")
				&& definition.contains("(family_id, academy_id)")
				&& (definition.endsWith("WHERE is_default") || definition.endsWith("WHERE (is_default = true)"));
	}
}
