package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoadMemberServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 101L;
	private static final long CHILD_ID = 131L;
	private static final long OTHER_CHILD_ID = 132L;
	private static final long CO_GUARDIAN_ID = 105L;
	private static final long TEACHER_ID = 140L;
	private static final String PHOTO_KEY = "profiles/guardian-photo.jpg";
	private static final Instant REMOVED_AT = Instant.parse("2026-11-13T09:00:00Z");

	@Mock
	private LoadUserPort loadUserPort;

	@Spy
	private InMemoryProfileImageStorage profileImageStorage = new InMemoryProfileImageStorage();

	@InjectMocks
	private LoadMemberService loadMemberService;

	@Test
	@DisplayName("user의 LoadMemberUseCase는 findMember · findMembers로 MemberInfo를 돌려주고 LoadFamilyUseCase는 findFamily로 FamilyInfo를 돌려준다")
	void findMemberReturnsMemberInfoFromLoadedUser() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));

		Optional<MemberInfo> result = loadMemberService.findMember(GUARDIAN_ID);

		assertThat(result).get()
				.extracting(MemberInfo::userId, MemberInfo::familyId, MemberInfo::role, MemberInfo::name,
						MemberInfo::phone)
				.containsExactly(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678");
	}

	@Test
	@DisplayName("findMember는 멤버가 없으면 빈 Optional을 돌려준다")
	void findMemberReturnsEmptyWhenUserMissing() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.empty());

		assertThat(loadMemberService.findMember(GUARDIAN_ID)).isEmpty();
	}

	@Test
	@DisplayName("findMembers는 그 가족의 멤버 전부를 MemberInfo로 돌려준다")
	void findMembersReturnsAllFamilyMembersAsMemberInfo() {
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));

		List<MemberInfo> result = loadMemberService.findMembers(FAMILY_ID);

		assertThat(result)
				.extracting(MemberInfo::userId, MemberInfo::familyId, MemberInfo::role, MemberInfo::name,
						MemberInfo::phone)
				.containsExactly(
						tuple(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
						tuple(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null));
	}

	@Test
	@DisplayName("findMembers는 LoadUserPort가 멤버를 섞인 순서로 돌려줘도 userId 오름차순으로 돌려준다")
	void findMembersReturnsMembersInAscendingUserIdOrder() {
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null),
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				User.restore(TEACHER_ID, FAMILY_ID, Role.TEACHER, "이선생", "+821098765432"),
				User.restore(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "박보호", "+821011112222")));

		List<MemberInfo> result = loadMemberService.findMembers(FAMILY_ID);

		assertThat(result).extracting(MemberInfo::userId)
				.containsExactly(GUARDIAN_ID, CO_GUARDIAN_ID, CHILD_ID, TEACHER_ID);
	}

	@Test
	@DisplayName("findMembers는 멤버가 없는 가족이면 빈 목록을 돌려준다")
	void findMembersReturnsEmptyListWhenNoMembers() {
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of());

		assertThat(loadMemberService.findMembers(FAMILY_ID)).isEmpty();
	}

	@Test
	@DisplayName("findMember는 사진 키가 있는 멤버에게 그 키의 서명 URL을 profileImageUrl로 준다")
	void findMemberGivesPresignedUrlOfPhotoKeyAsProfileImageUrl() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678", PHOTO_KEY)));

		MemberInfo result = loadMemberService.findMember(GUARDIAN_ID).orElseThrow();

		String keyUrl = profileImageStorage.presignedUrl(PHOTO_KEY).orElseThrow();
		assertThat(result.profileImageUrl()).isEqualTo(keyUrl);
		assertThat(keyUrl).isNotEqualTo(profileImageStorage.defaultUrl().orElseThrow());
	}

	@Test
	@DisplayName("findMember는 사진 키가 없는 멤버에게 기본 아바타 URL을 profileImageUrl로 준다")
	void findMemberGivesDefaultAvatarUrlWhenNoPhotoKey() {
		given(loadUserPort.loadUser(CHILD_ID)).willReturn(Optional.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));

		MemberInfo result = loadMemberService.findMember(CHILD_ID).orElseThrow();

		assertThat(result.profileImageUrl()).isEqualTo(profileImageStorage.defaultUrl().orElseThrow());
	}

	@Test
	@DisplayName("findMember는 저장소 설정이 없으면 profileImageUrl을 null로 준다")
	void findMemberGivesNullProfileImageUrlWhenStorageIsNotConfigured() {
		profileImageStorage.unconfigure();
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678", PHOTO_KEY)));

		MemberInfo result = loadMemberService.findMember(GUARDIAN_ID).orElseThrow();

		assertThat(result.profileImageUrl()).isNull();
	}

	@Test
	@DisplayName("findMembers는 멤버마다 사진 키 서명 URL 또는 기본 아바타 URL을 profileImageUrl로 준다")
	void findMembersGivesPhotoKeyUrlOrDefaultAvatarUrlPerMember() {
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678", PHOTO_KEY),
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));

		List<MemberInfo> result = loadMemberService.findMembers(FAMILY_ID);

		String keyUrl = profileImageStorage.presignedUrl(PHOTO_KEY).orElseThrow();
		String defaultUrl = profileImageStorage.defaultUrl().orElseThrow();
		assertThat(result)
				.extracting(MemberInfo::userId, MemberInfo::profileImageUrl)
				.containsExactly(
						tuple(GUARDIAN_ID, keyUrl),
						tuple(CHILD_ID, defaultUrl));
	}

	@Test
	@DisplayName("findMembers는 저장소 설정이 없으면 모든 멤버의 profileImageUrl을 null로 준다")
	void findMembersGivesNullProfileImageUrlToAllWhenStorageIsNotConfigured() {
		profileImageStorage.unconfigure();
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678", PHOTO_KEY),
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));

		List<MemberInfo> result = loadMemberService.findMembers(FAMILY_ID);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(MemberInfo::profileImageUrl).containsOnlyNulls();
	}

	@Test
	@DisplayName("findMember는 해제된 멤버도 돌려주고 removed를 true로 담는다")
	void findMemberReturnsRemovedMemberWithRemovedTrue() {
		given(loadUserPort.loadUser(TEACHER_ID)).willReturn(Optional.of(
				User.restore(TEACHER_ID, FAMILY_ID, Role.TEACHER, "이선생", "+821098765432", null, REMOVED_AT)));

		Optional<MemberInfo> result = loadMemberService.findMember(TEACHER_ID);

		assertThat(result).get()
				.extracting(MemberInfo::userId, MemberInfo::name, MemberInfo::removed)
				.containsExactly(TEACHER_ID, "이선생", true);
	}

	@Test
	@DisplayName("findMember는 해제되지 않은 멤버의 removed를 false로 담는다")
	void findMemberReturnsActiveMemberWithRemovedFalse() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));

		MemberInfo result = loadMemberService.findMember(GUARDIAN_ID).orElseThrow();

		assertThat(result.removed()).isFalse();
	}

	@Test
	@DisplayName("findMembers의 MemberInfo는 해제되지 않은 멤버의 removed를 false로 담는다")
	void findMembersReturnsMembersWithRemovedFalse() {
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));

		List<MemberInfo> result = loadMemberService.findMembers(FAMILY_ID);

		assertThat(result).extracting(MemberInfo::removed).containsExactly(false, false);
	}

	// #80 (user 04 #85 · findProfileImageUrls)

	@Test
	@DisplayName("findProfileImageUrls는 profileImageKey가 있는 사용자에게 그 키의 서명 URL을, 없는 사용자에게 기본 아바타 서명 URL을 준다")
	void findProfileImageUrlsReturnsKeyUrlOrDefaultAvatarUrl() {
		given(loadUserPort.loadUsersByIds(anyList())).willReturn(List.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null, "profiles/minjun-photo.jpg"),
				User.restore(OTHER_CHILD_ID, FAMILY_ID, Role.CHILD, "서연", null)));

		Map<Long, String> result = loadMemberService.findProfileImageUrls(List.of(CHILD_ID, OTHER_CHILD_ID));

		String keyUrl = profileImageStorage.presignedUrl("profiles/minjun-photo.jpg").orElseThrow();
		String defaultUrl = profileImageStorage.defaultUrl().orElseThrow();
		assertThat(result).containsOnly(
				entry(CHILD_ID, keyUrl),
				entry(OTHER_CHILD_ID, defaultUrl));
		assertThat(keyUrl).isNotEqualTo(defaultUrl);
	}

	@Test
	@DisplayName("findProfileImageUrls는 저장소 설정이 없으면 그 사용자의 키를 Map에 넣지 않는다")
	void findProfileImageUrlsOmitsUsersWhenStorageIsNotConfigured() {
		profileImageStorage.unconfigure();
		given(loadUserPort.loadUsersByIds(anyList())).willReturn(List.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null, "profiles/minjun-photo.jpg"),
				User.restore(OTHER_CHILD_ID, FAMILY_ID, Role.CHILD, "서연", null)));

		Map<Long, String> result = loadMemberService.findProfileImageUrls(List.of(CHILD_ID, OTHER_CHILD_ID));

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findProfileImageUrls는 여러 userId를 LoadUserPort.loadUsersByIds 한 번으로 읽는다")
	void findProfileImageUrlsLoadsAllUsersInOneCall() {
		given(loadUserPort.loadUsersByIds(anyList())).willReturn(List.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null),
				User.restore(OTHER_CHILD_ID, FAMILY_ID, Role.CHILD, "서연", null)));

		loadMemberService.findProfileImageUrls(List.of(CHILD_ID, OTHER_CHILD_ID));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
		verify(loadUserPort, times(1)).loadUsersByIds(idsCaptor.capture());
		assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(CHILD_ID, OTHER_CHILD_ID);
		verify(loadUserPort, never()).loadUser(anyLong());
	}
}
