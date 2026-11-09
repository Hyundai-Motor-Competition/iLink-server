package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "guardian_data_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianDataSettingEntity extends BaseTimeEntity {

	@Id
	private Long userId;

	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean learningConsent;

	public GuardianDataSettingEntity(Long userId, boolean learningConsent) {
		this.userId = userId;
		this.learningConsent = learningConsent;
	}

}
