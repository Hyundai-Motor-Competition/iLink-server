package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.IncidentType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pickup_session",
		indexes = @Index(name = "idx_pickup_session_vehicle_date", columnList = "vehicleId, date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupSessionEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sessionId;

	@Column(nullable = false)
	private Long vehicleId;

	@Column(nullable = false)
	private Long familyId;

	@Column(nullable = false)
	private Long spotId;

	@Column(nullable = false, length = 50)
	private String spotName;

	@Column(nullable = false, columnDefinition = "geography(Point,4326)")
	private Point stopPoint;

	@Column(nullable = false, columnDefinition = "geography(Point,4326)")
	private Point meetingPoint;

	@Column(nullable = false)
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SessionStatus sessionStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VehiclePhase vehiclePhase;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SpotStatus spotStatus;

	@Column(nullable = false)
	@ColumnDefault("0")
	private int stateVersion;

	@Column(nullable = false)
	@ColumnDefault("0")
	private int spotVersion;

	@Column(nullable = false)
	@ColumnDefault("0")
	private int routeVersion;

	private Integer routeSpotVersion;

	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean callHeld;

	private Instant etaAt;

	private Integer remainingMeters;

	private Instant etaUpdatedAt;

	private Instant boardingDetectedAt;

	private Instant boardingCancelableUntil;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private BoardingDoor boardingDoor;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private IncidentType incidentType;

	@Column(length = 100)
	private String incidentCause;

	private Instant incidentOpenedAt;

	private Instant incidentResolvedAt;

	public PickupSessionEntity(Long sessionId, Long vehicleId, Long familyId, Long spotId, String spotName,
			Point stopPoint, Point meetingPoint, LocalDate date, SessionStatus sessionStatus, VehiclePhase vehiclePhase,
			SpotStatus spotStatus, int stateVersion, int spotVersion, int routeVersion, Integer routeSpotVersion,
			boolean callHeld, Instant etaAt, Integer remainingMeters, Instant etaUpdatedAt, Instant boardingDetectedAt,
			Instant boardingCancelableUntil, BoardingDoor boardingDoor, IncidentType incidentType,
			String incidentCause, Instant incidentOpenedAt, Instant incidentResolvedAt) {
		this.sessionId = sessionId;
		this.vehicleId = vehicleId;
		this.familyId = familyId;
		this.spotId = spotId;
		this.spotName = spotName;
		this.stopPoint = stopPoint;
		this.meetingPoint = meetingPoint;
		this.date = date;
		this.sessionStatus = sessionStatus;
		this.vehiclePhase = vehiclePhase;
		this.spotStatus = spotStatus;
		this.stateVersion = stateVersion;
		this.spotVersion = spotVersion;
		this.routeVersion = routeVersion;
		this.routeSpotVersion = routeSpotVersion;
		this.callHeld = callHeld;
		this.etaAt = etaAt;
		this.remainingMeters = remainingMeters;
		this.etaUpdatedAt = etaUpdatedAt;
		this.boardingDetectedAt = boardingDetectedAt;
		this.boardingCancelableUntil = boardingCancelableUntil;
		this.boardingDoor = boardingDoor;
		this.incidentType = incidentType;
		this.incidentCause = incidentCause;
		this.incidentOpenedAt = incidentOpenedAt;
		this.incidentResolvedAt = incidentResolvedAt;
	}

}
