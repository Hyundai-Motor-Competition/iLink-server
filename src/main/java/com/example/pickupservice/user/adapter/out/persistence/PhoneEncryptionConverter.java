package com.example.pickupservice.user.adapter.out.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class PhoneEncryptionConverter implements AttributeConverter<String, String> {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final String KEY_ALGORITHM = "AES";
	private static final int KEY_BYTES = 32;
	private static final int NONCE_BYTES = 12;
	private static final int TAG_BITS = 128;

	private final SecretKey key;
	private final SecureRandom secureRandom = new SecureRandom();

	public PhoneEncryptionConverter(@Value("${pickup.crypto.phone-key}") String base64Key) {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		if (keyBytes.length != KEY_BYTES) {
			throw new IllegalStateException("pickup.crypto.phone-key는 Base64로 인코딩한 32바이트여야 합니다");
		}
		this.key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
	}

	@Override
	public String convertToDatabaseColumn(String phone) {
		if (phone == null) {
			return null;
		}
		byte[] nonce = new byte[NONCE_BYTES];
		secureRandom.nextBytes(nonce);
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
			byte[] cipherText = cipher.doFinal(phone.getBytes(StandardCharsets.UTF_8));
			byte[] stored = ByteBuffer.allocate(nonce.length + cipherText.length)
					.put(nonce)
					.put(cipherText)
					.array();
			return Base64.getEncoder().encodeToString(stored);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("전화번호 암호화에 실패했습니다", e);
		}
	}

	@Override
	public String convertToEntityAttribute(String stored) {
		if (stored == null) {
			return null;
		}
		try {
			byte[] decoded = Base64.getDecoder().decode(stored);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, decoded, 0, NONCE_BYTES));
			byte[] plain = cipher.doFinal(decoded, NONCE_BYTES, decoded.length - NONCE_BYTES);
			return new String(plain, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new IllegalStateException("전화번호 복호화에 실패했습니다", e);
		}
	}

}
