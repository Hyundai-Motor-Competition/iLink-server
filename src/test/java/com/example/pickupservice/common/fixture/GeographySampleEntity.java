package com.example.pickupservice.common.fixture;

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
@Table(name = "geography_sample")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeographySampleEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(columnDefinition = "geography(Point,4326)")
	private Point location;

	public GeographySampleEntity(Point location) {
		this.location = location;
	}

}
