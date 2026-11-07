package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "vehicle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long vehicleId;

	private Long familyId;

	@Column(nullable = false, columnDefinition = "char(17)")
	private String vin;

	@Column(length = 50)
	private String model;

	@Column(length = 20)
	private String plateNumber;

	private Instant unlinkedAt;

	public VehicleEntity(Long vehicleId, Long familyId, String vin, String model, String plateNumber,
			Instant unlinkedAt) {
		this.vehicleId = vehicleId;
		this.familyId = familyId;
		this.vin = vin;
		this.model = model;
		this.plateNumber = plateNumber;
		this.unlinkedAt = unlinkedAt;
	}

}
