package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@Column(nullable = false)
	private Long familyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false, length = 20)
	private String name;

	@Convert(converter = PhoneEncryptionConverter.class)
	@Column(length = 255)
	private String phone;

	@Column(name = "profile_image_key", length = 200)
	private String profileImageKey;

	public UserEntity(Long userId, Long familyId, Role role, String name, String phone) {
		this(userId, familyId, role, name, phone, null);
	}

	public UserEntity(Long userId, Long familyId, Role role, String name, String phone, String profileImageKey) {
		this.userId = userId;
		this.familyId = familyId;
		this.role = role;
		this.name = name;
		this.phone = phone;
		this.profileImageKey = profileImageKey;
	}

}
