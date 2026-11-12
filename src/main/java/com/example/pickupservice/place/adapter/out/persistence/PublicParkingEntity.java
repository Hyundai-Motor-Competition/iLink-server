package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "public_parking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicParkingEntity extends BaseTimeEntity {

	@Id
	@Column(length = 20)
	private String parkingId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, columnDefinition = "geography(Point,4326)")
	private Point location;

	private Integer spaces;

	private Integer baseMinutes;

	private Integer baseFeeWon;

	public PublicParkingEntity(String parkingId, String name, Point location, Integer spaces, Integer baseMinutes,
			Integer baseFeeWon) {
		this.parkingId = parkingId;
		this.name = name;
		this.location = location;
		this.spaces = spaces;
		this.baseMinutes = baseMinutes;
		this.baseFeeWon = baseFeeWon;
	}

}
