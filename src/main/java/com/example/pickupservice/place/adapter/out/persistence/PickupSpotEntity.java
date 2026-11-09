package com.example.pickupservice.place.adapter.out.persistence;

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
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "pickup_spot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupSpotEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long spotId;

	@Column(nullable = false)
	private Long familyId;

	@Column(nullable = false)
	private Long academyId;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 200)
	private String memo;

	@Column(nullable = false, columnDefinition = "geography(Point,4326)")
	private Point stopPoint;

	@Column(nullable = false, columnDefinition = "geography(Point,4326)")
	private Point meetingPoint;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	@Column(nullable = false)
	private Long createdBy;

	public PickupSpotEntity(Long spotId, Long familyId, Long academyId, String name, String memo, Point stopPoint,
			Point meetingPoint, boolean isDefault, Long createdBy) {
		this.spotId = spotId;
		this.familyId = familyId;
		this.academyId = academyId;
		this.name = name;
		this.memo = memo;
		this.stopPoint = stopPoint;
		this.meetingPoint = meetingPoint;
		this.isDefault = isDefault;
		this.createdBy = createdBy;
	}

}
