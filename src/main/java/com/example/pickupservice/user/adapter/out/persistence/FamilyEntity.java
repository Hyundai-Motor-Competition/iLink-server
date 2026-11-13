package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.user.domain.JoinCallMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "family")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long familyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JoinCallMode joinCallMode;

	@Column(nullable = false)
	private int settingsVersion;

	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean childLocationEnabled;

	public FamilyEntity(Long familyId, JoinCallMode joinCallMode, int settingsVersion) {
		this.familyId = familyId;
		this.joinCallMode = joinCallMode;
		this.settingsVersion = settingsVersion;
	}

	public FamilyEntity(Long familyId, JoinCallMode joinCallMode, int settingsVersion,
			boolean childLocationEnabled) {
		this.familyId = familyId;
		this.joinCallMode = joinCallMode;
		this.settingsVersion = settingsVersion;
		this.childLocationEnabled = childLocationEnabled;
	}

}
