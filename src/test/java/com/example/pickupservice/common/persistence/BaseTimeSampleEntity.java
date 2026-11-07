package com.example.pickupservice.common.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "base_time_sample")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseTimeSampleEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	public BaseTimeSampleEntity(String name) {
		this.name = name;
	}

	public void rename(String name) {
		this.name = name;
	}

}
