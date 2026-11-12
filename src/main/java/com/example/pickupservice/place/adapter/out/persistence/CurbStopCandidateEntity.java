package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.place.domain.LineKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "curb_stop_candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurbStopCandidateEntity extends BaseTimeEntity {

	@Id
	private Long candidateId;

	@Column(nullable = false, columnDefinition = "geography(Point,4326)")
	private Point location;

	@Column(length = 100)
	private String roadName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LineKind lineKind;

	@Column(nullable = false)
	private boolean nearEnforcementCctv;

	public CurbStopCandidateEntity(Long candidateId, Point location, String roadName, LineKind lineKind,
			boolean nearEnforcementCctv) {
		this.candidateId = candidateId;
		this.location = location;
		this.roadName = roadName;
		this.lineKind = lineKind;
		this.nearEnforcementCctv = nearEnforcementCctv;
	}

}
