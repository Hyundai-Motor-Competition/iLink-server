package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.ChangeProfileImageCommand;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.pickupservice.common.fixture.TestImages.PNG_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChangeProfileImageServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final String OLD_KEY = "profiles/11111111-2222-3333-4444-555555555555.jpg";
	@Mock
	private LoadUserPort loadUserPort;

	@Mock
	private SaveUserPort saveUserPort;

	@Mock
	private ProfileImageStoragePort profileImageStoragePort;

	@InjectMocks
	private ChangeProfileImageService changeProfileImageService;

	private static User guardianWithKey(String profileImageKey) {
		return User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", null, profileImageKey);
	}

	@Test
	@DisplayName("연결 승인 RELINK에 사진이 있으면 보호자의 profileImageKey를 새 키로 바꾸고 이전 객체를 지운다")
	void replacesGuardianProfileImageKeyAndDeletesPreviousObject() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.of(guardianWithKey(OLD_KEY)));
		lenient().when(saveUserPort.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

		changeProfileImageService.changeProfileImage(
				new ChangeProfileImageCommand(GUARDIAN_ID, ProfileImage.of(PNG_BYTES)));

		InOrder inOrder = inOrder(profileImageStoragePort, saveUserPort);
		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
		inOrder.verify(profileImageStoragePort).put(key.capture(), bytes.capture(), eq("image/png"));
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		inOrder.verify(saveUserPort).saveUser(saved.capture());
		inOrder.verify(profileImageStoragePort).delete(OLD_KEY);

		assertThat(key.getValue()).matches(
				"profiles/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
		assertThat(key.getValue()).isNotEqualTo(OLD_KEY);
		assertThat(bytes.getValue()).isEqualTo(PNG_BYTES);
		assertThat(saved.getValue().getUserId()).isEqualTo(GUARDIAN_ID);
		assertThat(saved.getValue().getProfileImageKey()).isEqualTo(key.getValue());
		verify(profileImageStoragePort, never()).delete(key.getValue());
	}

	@Test
	@DisplayName("연결 승인 RELINK에 사진이 있고 보호자에게 이전 사진이 없으면 새 키만 저장하고 아무것도 지우지 않는다")
	void storesNewKeyWithoutDeletingWhenGuardianHadNoPreviousImage() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.of(guardianWithKey(null)));
		lenient().when(saveUserPort.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

		changeProfileImageService.changeProfileImage(
				new ChangeProfileImageCommand(GUARDIAN_ID, ProfileImage.of(PNG_BYTES)));

		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		verify(profileImageStoragePort).put(key.capture(), any(), eq("image/png"));
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(saved.capture());
		assertThat(saved.getValue().getProfileImageKey()).isEqualTo(key.getValue());
		verify(profileImageStoragePort, never()).delete(anyString());
	}

	@Test
	@DisplayName("연결 승인 RELINK에 사진이 없으면 보호자의 profileImageKey를 그대로 둔다")
	void keepsGuardianProfileImageKeyWhenNoImage() {
		lenient().when(loadUserPort.loadUser(GUARDIAN_ID)).thenReturn(Optional.of(guardianWithKey(OLD_KEY)));

		changeProfileImageService.changeProfileImage(new ChangeProfileImageCommand(GUARDIAN_ID, null));

		verifyNoInteractions(profileImageStoragePort);
		verify(saveUserPort, never()).saveUser(any());
	}
}
