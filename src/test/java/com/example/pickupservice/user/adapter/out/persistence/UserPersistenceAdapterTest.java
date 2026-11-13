package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.JoinCallMode;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
class UserPersistenceAdapterTest extends IntegrationTestSupport {

	private static final String PHONE = "+821012345678";
	private static final Instant UNLINKED_AT = Instant.parse("2026-11-03T09:12:34.123456Z");

	private final SaveFamilyPort saveFamilyPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SaveUserPort saveUserPort;
	private final LoadUserPort loadUserPort;
	private final SaveVehiclePort saveVehiclePort;
	private final LoadVehiclePort loadVehiclePort;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private final List<Long> familyIds = new ArrayList<>();
	private final List<Long> vehicleIds = new ArrayList<>();

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> {
			if (!vehicleIds.isEmpty()) {
				entityManager.createQuery("delete from VehicleEntity v where v.vehicleId in :ids")
						.setParameter("ids", vehicleIds)
						.executeUpdate();
			}
			if (!familyIds.isEmpty()) {
				entityManager.createQuery("delete from UserEntity u where u.familyId in :ids")
						.setParameter("ids", familyIds)
						.executeUpdate();
				entityManager.createQuery("delete from VehicleEntity v where v.familyId in :ids")
						.setParameter("ids", familyIds)
						.executeUpdate();
				entityManager.createQuery("delete from FamilyEntity f where f.familyId in :ids")
						.setParameter("ids", familyIds)
						.executeUpdate();
			}
		});
	}

	@Test
	@DisplayName("Family · User · Vehicle을 저장한 뒤 다시 읽으면 도메인 객체의 값이 같다")
	void savedFamilyUserVehicleAreLoadedWithSameValues() {
		Family savedFamily = saveFamilyPort.saveFamily(Family.restore(null, JoinCallMode.GUARDIAN_CONFIRM, 7));
		assertThat(savedFamily.getFamilyId()).as("저장된 familyId").isNotNull();
		familyIds.add(savedFamily.getFamilyId());
		assertThat(savedFamily.getJoinCallMode()).isEqualTo(JoinCallMode.GUARDIAN_CONFIRM);
		assertThat(savedFamily.getSettingsVersion()).isEqualTo(7);
		long familyId = savedFamily.getFamilyId();

		User savedGuardian = saveUserPort.saveUser(User.create(familyId, Role.GUARDIAN, "김보호", PHONE));
		User savedChild = saveUserPort.saveUser(User.create(familyId, Role.CHILD, "민준", null));
		assertThat(savedGuardian.getUserId()).as("저장된 보호자 userId").isNotNull();
		assertThat(savedChild.getUserId()).as("저장된 자녀 userId").isNotNull();
		assertThat(savedGuardian.getPhone()).as("저장 결과의 phone은 평문").isEqualTo(PHONE);

		String linkedVin = randomVin();
		Vehicle savedLinked = saveVehiclePort.saveVehicle(
				Vehicle.restore(null, familyId, linkedVin, "아이오닉 5", "12가 3456", null));
		Vehicle savedUnlinked = saveVehiclePort.saveVehicle(
				Vehicle.restore(null, familyId, randomVin(), "쏘렌토", "34나 5678", UNLINKED_AT));
		Vehicle savedUnowned = saveVehiclePort.saveVehicle(Vehicle.register(randomVin(), "EV9"));
		assertThat(savedLinked.getVehicleId()).as("저장된 연결 차량 vehicleId").isNotNull();
		assertThat(savedUnlinked.getVehicleId()).as("저장된 해제 차량 vehicleId").isNotNull();
		assertThat(savedUnowned.getVehicleId()).as("저장된 미연결 차량 vehicleId").isNotNull();
		vehicleIds.add(savedUnowned.getVehicleId());

		clearPersistenceContext();

		assertThat(loadFamilyPort.loadFamily(familyId)).get()
				.usingRecursiveComparison()
				.isEqualTo(Family.restore(familyId, JoinCallMode.GUARDIAN_CONFIRM, 7));

		assertThat(loadUserPort.loadUser(savedGuardian.getUserId())).get()
				.usingRecursiveComparison()
				.isEqualTo(User.restore(savedGuardian.getUserId(), familyId, Role.GUARDIAN, "김보호", PHONE));
		assertThat(loadUserPort.loadUser(savedChild.getUserId())).get()
				.usingRecursiveComparison()
				.isEqualTo(User.restore(savedChild.getUserId(), familyId, Role.CHILD, "민준", null));

		assertThat(loadVehiclePort.loadVehicle(savedLinked.getVehicleId())).get()
				.usingRecursiveComparison()
				.ignoringFields("version")
				.isEqualTo(Vehicle.restore(savedLinked.getVehicleId(), familyId, linkedVin, "아이오닉 5",
						"12가 3456", null));
		assertThat(loadVehiclePort.loadVehicle(savedUnlinked.getVehicleId())).get()
				.usingRecursiveComparison()
				.ignoringFields("version")
				.isEqualTo(Vehicle.restore(savedUnlinked.getVehicleId(), familyId, savedUnlinked.getVin(), "쏘렌토",
						"34나 5678", UNLINKED_AT));
		assertThat(loadVehiclePort.loadVehicle(savedUnowned.getVehicleId())).get()
				.usingRecursiveComparison()
				.ignoringFields("version")
				.isEqualTo(Vehicle.restore(savedUnowned.getVehicleId(), null, savedUnowned.getVin(), "EV9", null,
						null));
	}

	@Test
	@DisplayName("childLocationEnabled를 true로 저장하고 다시 읽으면 true이고 false로 바꿔 저장하면 false로 읽힌다")
	void childLocationEnabledIsSavedAndLoaded() {
		Family saved = saveFamilyPort.saveFamily(Family.restore(null, JoinCallMode.AUTO, 2, true));
		long familyId = saved.getFamilyId();
		familyIds.add(familyId);
		assertThat(saved.isChildLocationEnabled()).as("저장 결과").isTrue();
		clearPersistenceContext();

		Family loaded = loadFamilyPort.loadFamily(familyId).orElseThrow();
		assertThat(loaded.isChildLocationEnabled()).as("다시 읽은 값").isTrue();
		assertThat(loaded.getSettingsVersion()).isEqualTo(2);

		loaded.changeChildLocation(false);
		saveFamilyPort.saveFamily(loaded);
		clearPersistenceContext();

		Family reloaded = loadFamilyPort.loadFamily(familyId).orElseThrow();
		assertThat(reloaded.isChildLocationEnabled()).as("false로 바꿔 저장한 뒤 읽은 값").isFalse();
		assertThat(reloaded.getSettingsVersion()).isEqualTo(3);
	}

	@Test
	@DisplayName("child_location_enabled 컬럼은 boolean NOT NULL이고 기본값이 false여서 값 없이 넣은 행은 false로 읽힌다")
	void childLocationEnabledColumnIsNotNullWithDefaultFalse() {
		Map<String, Object> column = jdbcTemplate.queryForMap("""
				select data_type, is_nullable, column_default
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = 'family'
				  and column_name = 'child_location_enabled'
				""");
		assertThat(column.get("data_type")).isEqualTo("boolean");
		assertThat(column.get("is_nullable")).isEqualTo("NO");
		assertThat(column.get("column_default")).as("child_location_enabled의 DB 기본값").isNotNull();
		assertThat(String.valueOf(column.get("column_default"))).isEqualTo("false");

		Long familyId = jdbcTemplate.queryForObject("""
				insert into family (join_call_mode, settings_version, created_at, updated_at)
				values ('AUTO', 0, now(), now())
				returning family_id
				""", Long.class);
		familyIds.add(familyId);

		assertThat(loadFamilyPort.loadFamily(familyId)).get()
				.extracting(Family::isChildLocationEnabled)
				.isEqualTo(false);
		assertThat(jdbcTemplate.queryForObject(
				"select child_location_enabled from family where family_id = ?", Boolean.class, familyId))
				.isFalse();
	}

	@Test
	@DisplayName("저장된 Family · User · Vehicle을 바꿔 다시 저장하면 새 행 없이 같은 id의 값이 바뀐다")
	void resavingRestoredObjectsUpdatesSameRows() {
		Family family = saveFamilyPort.saveFamily(Family.create());
		long familyId = family.getFamilyId();
		familyIds.add(familyId);
		User member = saveUserPort.saveUser(User.create(familyId, Role.DELEGATE, "이대리", PHONE));
		Vehicle vehicle = saveVehiclePort.saveVehicle(
				Vehicle.restore(null, familyId, randomVin(), "아이오닉 5", "12가 3456", null));
		clearPersistenceContext();

		Family loadedFamily = loadFamilyPort.loadFamily(familyId).orElseThrow();
		loadedFamily.changeCallMode(JoinCallMode.GUARDIAN_CONFIRM);
		saveFamilyPort.saveFamily(loadedFamily);

		User loadedMember = loadUserPort.loadUser(member.getUserId()).orElseThrow();
		loadedMember.changeName("박대리");
		loadedMember.changePhone("+821098765432");
		loadedMember.changeRole(Role.TEACHER);
		saveUserPort.saveUser(loadedMember);

		Vehicle loadedVehicle = loadVehiclePort.loadVehicle(vehicle.getVehicleId()).orElseThrow();
		loadedVehicle.unlink(UNLINKED_AT);
		saveVehiclePort.saveVehicle(loadedVehicle);
		clearPersistenceContext();

		assertThat(loadFamilyPort.loadFamily(familyId)).get()
				.usingRecursiveComparison()
				.isEqualTo(Family.restore(familyId, JoinCallMode.GUARDIAN_CONFIRM, 1));
		assertThat(loadUserPort.loadUsers(familyId)).as("가족 멤버 행 수").hasSize(1);
		assertThat(loadUserPort.loadUser(member.getUserId())).get()
				.usingRecursiveComparison()
				.isEqualTo(User.restore(member.getUserId(), familyId, Role.TEACHER, "박대리", "+821098765432"));
		assertThat(loadVehiclePort.loadVehicle(vehicle.getVehicleId())).get()
				.usingRecursiveComparison()
				.ignoringFields("version")
				.isEqualTo(Vehicle.restore(vehicle.getVehicleId(), familyId, vehicle.getVin(), "아이오닉 5", "12가 3456",
						UNLINKED_AT));
	}

	@Test
	@DisplayName("같은 Vehicle을 두 번 읽어 각각 familyId를 바꿔 저장하면 두 번째 저장은 낙관적 잠금 충돌로 실패하고 첫 번째 값이 남는다")
	void secondSaveOfStaleVehicleFailsWithOptimisticLockConflict() {
		long firstFamilyId = newFamilyId();
		long secondFamilyId = newFamilyId();
		Vehicle pending = saveVehiclePort.saveVehicle(Vehicle.register(randomVin(), "EV9"));
		long vehicleId = pending.getVehicleId();
		vehicleIds.add(vehicleId);
		clearPersistenceContext();

		Vehicle readByFirst = loadVehiclePort.loadVehicle(vehicleId).orElseThrow();
		Vehicle readBySecond = loadVehiclePort.loadVehicle(vehicleId).orElseThrow();

		readByFirst.linkTo(firstFamilyId);
		saveVehiclePort.saveVehicle(readByFirst);

		readBySecond.linkTo(secondFamilyId);
		assertThatThrownBy(() -> saveVehiclePort.saveVehicle(readBySecond))
				.as("먼저 읽은 버전으로 저장하면 충돌")
				.isInstanceOf(OptimisticLockingFailureException.class);
		clearPersistenceContext();

		assertThat(loadVehiclePort.loadVehicle(vehicleId)).get()
				.extracting(Vehicle::getFamilyId)
				.as("먼저 저장한 가족이 남음")
				.isEqualTo(firstFamilyId);
	}

	@Test
	@DisplayName("loadLinkedVehicleByVin은 unlinkedAt이 빈 행만 찾는다")
	void loadLinkedVehicleByVinFindsOnlyRowWithoutUnlinkedAt() {
		long oldFamilyId = newFamilyId();
		long newFamilyId = newFamilyId();
		String vin = randomVin();
		Vehicle unlinked = saveVehiclePort.saveVehicle(
				Vehicle.restore(null, oldFamilyId, vin, "아이오닉 5", "12가 3456", UNLINKED_AT));
		Vehicle linked = saveVehiclePort.saveVehicle(
				Vehicle.restore(null, newFamilyId, vin, "아이오닉 5", "12가 3456", null));
		clearPersistenceContext();

		assertThat(loadVehiclePort.loadLinkedVehicleByVin(vin)).get()
				.usingRecursiveComparison()
				.ignoringFields("version")
				.isEqualTo(Vehicle.restore(linked.getVehicleId(), newFamilyId, vin, "아이오닉 5", "12가 3456", null));
		assertThat(linked.getVehicleId()).as("해제된 행과 다른 새 행").isNotEqualTo(unlinked.getVehicleId());
	}

	@Test
	@DisplayName("loadLinkedVehicleByVin은 같은 VIN의 해제된 행만 있으면 빈 값을 돌려준다")
	void loadLinkedVehicleByVinReturnsEmptyWhenOnlyUnlinkedRowsExist() {
		long familyId = newFamilyId();
		String vin = randomVin();
		saveVehiclePort.saveVehicle(Vehicle.restore(null, familyId, vin, "아이오닉 5", "12가 3456", UNLINKED_AT));
		clearPersistenceContext();

		assertThat(loadVehiclePort.loadLinkedVehicleByVin(vin)).isEmpty();
		assertThat(loadVehiclePort.loadLinkedVehicleByVin(randomVin())).as("행이 없는 VIN").isEmpty();
	}

	@Test
	@DisplayName("loadLinkedVehicleOfFamily는 그 가족의 연결 중인 차량을 찾는다")
	void loadLinkedVehicleOfFamilyFindsLinkedVehicleOfThatFamily() {
		long familyA = newFamilyId();
		long familyB = newFamilyId();
		saveVehiclePort.saveVehicle(Vehicle.restore(null, familyA, randomVin(), "쏘렌토", "34나 5678", UNLINKED_AT));
		String linkedVinA = randomVin();
		Vehicle linkedA = saveVehiclePort.saveVehicle(
				Vehicle.restore(null, familyA, linkedVinA, "아이오닉 5", "12가 3456", null));
		Vehicle linkedB = saveVehiclePort.saveVehicle(
				Vehicle.restore(null, familyB, randomVin(), "EV9", "56다 7890", null));
		clearPersistenceContext();

		assertThat(loadVehiclePort.loadLinkedVehicleOfFamily(familyA)).get()
				.usingRecursiveComparison()
				.ignoringFields("version")
				.isEqualTo(Vehicle.restore(linkedA.getVehicleId(), familyA, linkedVinA, "아이오닉 5", "12가 3456", null));
		assertThat(loadVehiclePort.loadLinkedVehicleOfFamily(familyB)).get()
				.extracting(Vehicle::getVehicleId)
				.as("가족 B는 B의 차량")
				.isEqualTo(linkedB.getVehicleId());
	}

	@Test
	@DisplayName("loadLinkedVehicleOfFamily는 연결 중인 차량이 없는 가족이면 빈 값을 돌려준다")
	void loadLinkedVehicleOfFamilyReturnsEmptyWhenFamilyHasNoLinkedVehicle() {
		long familyWithUnlinkedOnly = newFamilyId();
		long familyWithoutVehicle = newFamilyId();
		saveVehiclePort.saveVehicle(
				Vehicle.restore(null, familyWithUnlinkedOnly, randomVin(), "쏘렌토", "34나 5678", UNLINKED_AT));
		clearPersistenceContext();

		assertThat(loadVehiclePort.loadLinkedVehicleOfFamily(familyWithUnlinkedOnly)).as("해제된 차량만 있는 가족").isEmpty();
		assertThat(loadVehiclePort.loadLinkedVehicleOfFamily(familyWithoutVehicle)).as("차량이 없는 가족").isEmpty();
	}

	@Test
	@DisplayName("countNonChildMembers는 CHILD를 뺀 가족 멤버 수를 센다")
	void countNonChildMembersCountsFamilyMembersExceptChildren() {
		long familyId = newFamilyId();
		long otherFamilyId = newFamilyId();
		saveUserPort.saveUser(User.create(familyId, Role.GUARDIAN, "김보호", PHONE));
		saveUserPort.saveUser(User.create(familyId, Role.CO_GUARDIAN, "이공동", "+821011112222"));
		saveUserPort.saveUser(User.create(familyId, Role.TEACHER, "박선생", "+821033334444"));
		saveUserPort.saveUser(User.create(familyId, Role.CHILD, "민준", null));
		saveUserPort.saveUser(User.create(familyId, Role.CHILD, "서윤", null));
		saveUserPort.saveUser(User.create(otherFamilyId, Role.DELEGATE, "최대리", "+821055556666"));
		clearPersistenceContext();

		assertThat(loadUserPort.countNonChildMembers(familyId)).isEqualTo(3L);
		assertThat(loadUserPort.countNonChildMembers(otherFamilyId)).as("다른 가족").isEqualTo(1L);
	}

	@Test
	@DisplayName("countNonChildMembers는 CHILD만 있거나 멤버가 없는 가족이면 0을 돌려준다")
	void countNonChildMembersReturnsZeroWithoutNonChildMembers() {
		long childOnlyFamilyId = newFamilyId();
		long emptyFamilyId = newFamilyId();
		saveUserPort.saveUser(User.create(childOnlyFamilyId, Role.CHILD, "민준", null));
		clearPersistenceContext();

		assertThat(loadUserPort.countNonChildMembers(childOnlyFamilyId)).isZero();
		assertThat(loadUserPort.countNonChildMembers(emptyFamilyId)).isZero();
	}

	private long newFamilyId() {
		long familyId = saveFamilyPort.saveFamily(Family.create()).getFamilyId();
		familyIds.add(familyId);
		return familyId;
	}

	private void clearPersistenceContext() {
		transactionTemplate.executeWithoutResult(status -> entityManager.clear());
	}

	private static String randomVin() {
		return "PRS" + String.format("%014d", ThreadLocalRandom.current().nextLong(100_000_000_000_000L));
	}

}
