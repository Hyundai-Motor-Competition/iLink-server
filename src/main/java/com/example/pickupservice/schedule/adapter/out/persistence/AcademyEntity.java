package com.example.pickupservice.schedule.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "academy", uniqueConstraints = @UniqueConstraint(name = "uk_academy_family_external_place",
		columnNames = {"family_id", "external_place_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcademyEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long academyId;

	@Column(nullable = false)
	private Long familyId;

	@Column(nullable = false, length = 100)
	private String externalPlaceId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String address;

	@Column(nullable = false, columnDefinition = "geography(Point,4326)")
	private Point location;

	@Column(columnDefinition = "geography(Point,4326)")
	private Point entrance;

	@Column(length = 20)
	private String phone;

	public AcademyEntity(Long academyId, long familyId, String externalPlaceId, String name, String address,
			Point location, Point entrance, String phone) {
		this.academyId = academyId;
		this.familyId = familyId;
		this.externalPlaceId = externalPlaceId;
		this.name = name;
		this.address = address;
		this.location = location;
		this.entrance = entrance;
		this.phone = phone;
	}

}
