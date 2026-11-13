package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
class IdempotencyKeyRepositoryTest extends IntegrationTestSupport {

	private static final String KEY = "7f9c2ba4-e88f-4a3c-9d1b-2f6a8c0e5b13";
	private static final String OTHER_KEY = "0b6e4d21-5a3f-4c8e-b7d9-1e2f3a4b5c6d";
	private static final long USER_ID = 101L;
	private static final long OTHER_USER_ID = 102L;
	private static final long VEHICLE_ID = 101L;
	private static final int CREATED = 201;
	private static final int NO_CONTENT = 204;
	private static final String REQUEST_BODY = """
			{"academyId":31,"name":"정문 앞","stopPoint":{"lat":37.501234,"lng":127.039876},"isDefault":false}""";
	private static final String RESPONSE_BODY = """
			{"spotId":41,"name":"정문 앞","stopPoint":{"lat":37.501234,"lng":127.039876},"memo":null,"isDefault":true}""";

	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("delete from idempotency_key where idempotency_key in (?, ?)", KEY, OTHER_KEY);
	}

	@Test
	@DisplayName("USER 101이 키 · 요청 해시(SHA-256 16진수 64자) · 응답 상태 201 · JSON 응답 본문을 저장하면 주체 종류 · 주체 id · 키로 다시 읽었을 때 같은 키 · 요청 해시 · 상태 201 · 같은 JSON 본문이 나오고 createdAt이 채워져 있으며 다른 키로는 읽히지 않는다")
	void savedKeyIsReadBackByPrincipalAndKeyWithSameHashStatusAndBody() throws Exception {
		String requestHash = sha256Hex(REQUEST_BODY);
		idempotencyKeyRepository.save(
				completedRow(PrincipalType.USER, USER_ID, KEY, requestHash, CREATED, RESPONSE_BODY));

		IdempotencyKeyEntity found = findByPrincipalAndKey(PrincipalType.USER, USER_ID, KEY);

		assertThat(found.getPrincipalType()).isEqualTo(PrincipalType.USER);
		assertThat(found.getPrincipalId()).isEqualTo(USER_ID);
		assertThat(found.getIdempotencyKey()).isEqualTo(KEY);
		assertThat(found.getRequestHash()).isEqualTo(requestHash).hasSize(64);
		assertThat(found.getResponseStatus()).isEqualTo(CREATED);
		JSONAssert.assertEquals(RESPONSE_BODY, found.getResponseBody(), JSONCompareMode.STRICT);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(idempotencyKeyRepository.findByPrincipalTypeAndPrincipalIdAndIdempotencyKey(
				PrincipalType.USER, USER_ID, OTHER_KEY)).isEmpty();
	}

	@Test
	@DisplayName("본문 없는 204 응답을 응답 본문 null로 저장하면 다시 읽었을 때 상태 204 · 응답 본문 null이다")
	void responseWithoutBodyIsReadBackAsStatus204AndNullBody() {
		String requestHash = sha256Hex("DELETE /v1/families/me/members/201");
		idempotencyKeyRepository.save(
				completedRow(PrincipalType.USER, USER_ID, KEY, requestHash, NO_CONTENT, null));

		IdempotencyKeyEntity found = findByPrincipalAndKey(PrincipalType.USER, USER_ID, KEY);

		assertThat(found.getRequestHash()).isEqualTo(requestHash);
		assertThat(found.getResponseStatus()).isEqualTo(NO_CONTENT);
		assertThat(found.getResponseBody()).isNull();
	}

	@Test
	@DisplayName("같은 키라도 주체가 다르면(USER 101 · USER 102 · VEHICLE 101) 세 행으로 따로 저장되고 주체 종류 · 주체 id · 키로 읽으면 각자 저장한 요청 해시가 나온다")
	void sameKeyOfDifferentPrincipalsIsStoredAndReadSeparately() {
		String userHash = sha256Hex("USER 101 " + REQUEST_BODY);
		String otherUserHash = sha256Hex("USER 102 " + REQUEST_BODY);
		String vehicleHash = sha256Hex("VEHICLE 101 " + REQUEST_BODY);
		idempotencyKeyRepository.save(
				completedRow(PrincipalType.USER, USER_ID, KEY, userHash, CREATED, RESPONSE_BODY));
		idempotencyKeyRepository.save(
				completedRow(PrincipalType.USER, OTHER_USER_ID, KEY, otherUserHash, CREATED, RESPONSE_BODY));
		idempotencyKeyRepository.save(
				completedRow(PrincipalType.VEHICLE, VEHICLE_ID, KEY, vehicleHash, CREATED, RESPONSE_BODY));

		assertThat(countRowsWithKey(KEY)).isEqualTo(3);
		assertThat(findByPrincipalAndKey(PrincipalType.USER, USER_ID, KEY).getRequestHash()).isEqualTo(userHash);
		assertThat(findByPrincipalAndKey(PrincipalType.USER, OTHER_USER_ID, KEY).getRequestHash()).isEqualTo(otherUserHash);
		assertThat(findByPrincipalAndKey(PrincipalType.VEHICLE, VEHICLE_ID, KEY).getRequestHash()).isEqualTo(vehicleHash);
	}

	@Test
	@DisplayName("같은 주체(USER 101)가 같은 키로 두 번째 행을 저장하면 요청 해시가 달라도 DataIntegrityViolationException이 나고 그 키의 행은 처음 저장한 한 건만 남는다")
	void samePrincipalAndKeySavedTwiceViolatesUniqueConstraint() {
		String firstHash = sha256Hex(REQUEST_BODY);
		String secondHash = sha256Hex(REQUEST_BODY.replace("정문 앞", "후문 앞"));
		idempotencyKeyRepository.save(
				completedRow(PrincipalType.USER, USER_ID, KEY, firstHash, CREATED, RESPONSE_BODY));

		assertThatThrownBy(() -> idempotencyKeyRepository.save(
				completedRow(PrincipalType.USER, USER_ID, KEY, secondHash, CREATED, RESPONSE_BODY)))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertThat(countRowsWithKey(KEY)).isEqualTo(1);
		assertThat(findByPrincipalAndKey(PrincipalType.USER, USER_ID, KEY).getRequestHash()).isEqualTo(firstHash);
	}

	@Test
	@DisplayName("idempotency_key 테이블은 id bigint · principal_type varchar · principal_id bigint · idempotency_key varchar(NOT NULL) · request_hash char(64)(NOT NULL) · response_status integer · response_body jsonb(NULL 허용) · created_at · updated_at timestamptz 컬럼으로 만들어진다")
	void tableColumnsAreCreatedWithDesignedTypes() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
				select column_name, data_type, character_maximum_length, is_nullable
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = 'idempotency_key'
				""");
		Map<Object, Map<String, Object>> columnByName = columns.stream()
				.collect(Collectors.toMap(column -> column.get("column_name"), column -> column));

		assertThat(columnByName).containsKeys(
				"id", "principal_type", "principal_id", "idempotency_key", "request_hash",
				"response_status", "response_body", "created_at", "updated_at");
		assertThat(columnByName.get("id")).containsEntry("data_type", "bigint");
		assertThat(columnByName.get("principal_type")).containsEntry("data_type", "character varying");
		assertThat(columnByName.get("principal_id")).containsEntry("data_type", "bigint");
		assertThat(columnByName.get("idempotency_key"))
				.containsEntry("data_type", "character varying")
				.containsEntry("is_nullable", "NO");
		assertThat(columnByName.get("request_hash"))
				.containsEntry("data_type", "character")
				.containsEntry("character_maximum_length", 64)
				.containsEntry("is_nullable", "NO");
		assertThat(columnByName.get("response_status")).containsEntry("data_type", "integer");
		assertThat(columnByName.get("response_body"))
				.containsEntry("data_type", "jsonb")
				.containsEntry("is_nullable", "YES");
		assertThat(columnByName.get("created_at")).containsEntry("data_type", "timestamp with time zone");
		assertThat(columnByName.get("updated_at")).containsEntry("data_type", "timestamp with time zone");
	}

	private static IdempotencyKeyEntity completedRow(PrincipalType principalType, long principalId, String key,
			String requestHash, int responseStatus, String responseBody) {
		AuthPrincipal principal = new AuthPrincipal(principalType, principalId, null, null);
		IdempotencyKeyEntity row = new IdempotencyKeyEntity(new IdempotentRequest(principal, key, requestHash));
		row.complete(responseStatus, responseBody);
		return row;
	}

	private IdempotencyKeyEntity findByPrincipalAndKey(PrincipalType principalType, long principalId, String key) {
		return idempotencyKeyRepository
				.findByPrincipalTypeAndPrincipalIdAndIdempotencyKey(principalType, principalId, key)
				.orElseThrow();
	}

	private Integer countRowsWithKey(String key) {
		return jdbcTemplate.queryForObject(
				"select count(*) from idempotency_key where idempotency_key = ?", Integer.class, key);
	}

	private static String sha256Hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
