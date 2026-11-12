package com.example.pickupservice.place.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class GangnamReferenceFilesTest {

	private static final String DIR = "reference/gangnam/";
	private static final String BOUNDARY = DIR + "boundary_gangnam.geojson";
	private static final String PARKING = DIR + "parking_public.csv";
	private static final String CURB = DIR + "curb_stop_candidates.csv";

	@Test
	@DisplayName("classpath reference/gangnam에 boundary_gangnam.geojson · parking_public.csv · curb_stop_candidates.csv가 있다")
	void threeFilesExistOnClasspath() {
		assertThat(getClass().getClassLoader().getResource(BOUNDARY)).as(BOUNDARY).isNotNull();
		assertThat(getClass().getClassLoader().getResource(PARKING)).as(PARKING).isNotNull();
		assertThat(getClass().getClassLoader().getResource(CURB)).as(CURB).isNotNull();
	}

	@Test
	@DisplayName("parking_public.csv는 머리 줄을 뺀 데이터가 60행이다")
	void parkingCsvHasSixtyDataRows() throws IOException {
		assertThat(readLines(PARKING)).hasSize(1 + 60);
	}

	@Test
	@DisplayName("curb_stop_candidates.csv는 머리 줄을 뺀 데이터가 3,147행이다")
	void curbCsvHasDataRows() throws IOException {
		assertThat(readLines(CURB)).hasSize(1 + 3147);
	}

	@Test
	@DisplayName("parking_public.csv는 UTF-8 BOM으로 시작하고 머리 줄에 읽을 컬럼 7개가 있다")
	void parkingCsvHasBomAndColumns() throws IOException {
		assertThat(startsWithBom(PARKING)).isTrue();
		assertThat(headerColumns(PARKING)).contains(
				"parking_id", "name", "latitude", "longitude", "spaces", "base_minutes", "base_fee_won");
	}

	@Test
	@DisplayName("curb_stop_candidates.csv는 UTF-8 BOM으로 시작하고 머리 줄에 읽을 컬럼 6개가 있다")
	void curbCsvHasBomAndColumns() throws IOException {
		assertThat(startsWithBom(CURB)).isTrue();
		assertThat(headerColumns(CURB)).contains(
				"candidate_id", "latitude", "longitude", "road_name", "line_kind", "near_enforcement_cctv");
	}

	@Test
	@DisplayName("boundary_gangnam.geojson은 FeatureCollection이고 첫 feature의 geometry가 Polygon이다")
	void boundaryIsFeatureCollectionWithPolygon() throws IOException {
		JsonNode root;
		try (InputStream in = open(BOUNDARY)) {
			root = JsonMapper.builder().build().readTree(in);
		}

		assertThat(root.path("type").asString()).isEqualTo("FeatureCollection");
		assertThat(root.path("features").get(0).path("geometry").path("type").asString()).isEqualTo("Polygon");
	}

	@Test
	@DisplayName("parking_public.csv의 parking_id는 60행 모두 서로 다르다")
	void parkingIdsAreAllDistinct() throws IOException {
		List<String> ids = firstColumnValues(PARKING);
		assertThat(ids).hasSize(60);
		assertThat(ids).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("curb_stop_candidates.csv의 candidate_id는 3,147행 모두 서로 다르다")
	void candidateIdsAreAllDistinct() throws IOException {
		List<String> ids = firstColumnValues(CURB);
		assertThat(ids).hasSize(3147);
		assertThat(ids).doesNotHaveDuplicates();
	}

	private static InputStream open(String path) {
		InputStream in = GangnamReferenceFilesTest.class.getClassLoader().getResourceAsStream(path);
		assertThat(in).as(path).isNotNull();
		return in;
	}

	private static byte[] readBytes(String path) throws IOException {
		try (InputStream in = open(path)) {
			return in.readAllBytes();
		}
	}

	private static boolean startsWithBom(String path) throws IOException {
		byte[] bytes = readBytes(path);
		return bytes.length >= 3
				&& (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF;
	}

	private static List<String> readLines(String path) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(open(path), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					lines.add(line);
				}
			}
		}
		return lines;
	}

	private static List<String> firstColumnValues(String path) throws IOException {
		List<String> lines = readLines(path);
		return lines.subList(1, lines.size()).stream()
				.map(line -> line.split(",", -1)[0].trim())
				.toList();
	}

	private static List<String> headerColumns(String path) throws IOException {
		String header = readLines(path).get(0);
		if (header.startsWith("\uFEFF")) {
			header = header.substring(1);
		}
		return Arrays.stream(header.split(",")).map(String::trim).toList();
	}
}
