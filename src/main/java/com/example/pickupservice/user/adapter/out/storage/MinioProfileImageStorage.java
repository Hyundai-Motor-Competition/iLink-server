package com.example.pickupservice.user.adapter.out.storage;

import com.example.pickupservice.common.image.StorageUnavailableException;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MinioProfileImageStorage implements ProfileImageStoragePort {

	private static final String DEFAULT_AVATAR_KEY = "defaults/avatar.png";
	private static final String REGION = "us-east-1";

	private final String bucket;
	private final int urlTtlSeconds;
	private final MinioClient uploadClient;
	private final MinioClient signingClient;

	public MinioProfileImageStorage(ProfileImageProperties properties) {
		this.bucket = properties.bucket();
		this.urlTtlSeconds = properties.urlTtl() == null ? 0 : (int) properties.urlTtl().toSeconds();
		if (properties.endpoint() == null || properties.endpoint().isBlank()) {
			this.uploadClient = null;
			this.signingClient = null;
			return;
		}
		this.uploadClient = MinioClient.builder()
				.endpoint(properties.endpoint())
				.region(REGION)
				.credentials(properties.accessKey(), properties.secretKey())
				.build();
		this.signingClient = MinioClient.builder()
				.endpoint(properties.publicEndpoint())
				.region(REGION)
				.credentials(properties.accessKey(), properties.secretKey())
				.build();
	}

	@Override
	public void put(String key, byte[] bytes, String contentType) {
		if (!configured()) {
			throw new StorageUnavailableException();
		}
		try {
			uploadClient.putObject(PutObjectArgs.builder()
					.bucket(bucket)
					.object(key)
					.data(bytes, bytes.length)
					.contentType(contentType)
					.build());
		} catch (Exception e) {
			throw new StorageUnavailableException(e);
		}
	}

	@Override
	public void delete(String key) {
		if (!configured()) {
			return;
		}
		try {
			uploadClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
		} catch (Exception e) {
			log.warn("프로필 사진 삭제 실패 key={}", key, e);
		}
	}

	@Override
	public Optional<String> presignedUrl(String key) {
		if (!configured()) {
			return Optional.empty();
		}
		try {
			return Optional.of(signingClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
					.method(Http.Method.GET)
					.bucket(bucket)
					.object(key)
					.expiry(urlTtlSeconds, TimeUnit.SECONDS)
					.build()));
		} catch (Exception e) {
			throw new StorageUnavailableException(e);
		}
	}

	@Override
	public Optional<String> defaultUrl() {
		return presignedUrl(DEFAULT_AVATAR_KEY);
	}

	@Override
	public boolean configured() {
		return uploadClient != null;
	}
}
