package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.user.application.port.in.ChangeProfileImageCommand;
import com.example.pickupservice.user.application.port.in.ChangeProfileImageUseCase;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ChangeProfileImageService implements ChangeProfileImageUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;
	private final ProfileImageStoragePort profileImageStoragePort;

	@Override
	@Transactional
	public void changeProfileImage(ChangeProfileImageCommand command) {
		ProfileImage image = command.profileImage();
		if (image == null) {
			return;
		}
		User user = loadUserPort.loadUser(command.userId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		String previousKey = user.getProfileImageKey();
		user.changeProfileImageKey(ProfileImageUploads.upload(profileImageStoragePort, image));
		saveUserPort.saveUser(user);
		if (previousKey != null) {
			deleteAfterCommit(previousKey);
		}
	}

	private void deleteAfterCommit(String key) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			profileImageStoragePort.delete(key);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				profileImageStoragePort.delete(key);
			}
		});
	}
}
