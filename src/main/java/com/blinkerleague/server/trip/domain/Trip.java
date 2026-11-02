package com.blinkerleague.server.trip.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;
import com.blinkerleague.server.threshold.domain.ScoreItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운행 한 건. id는 앱이 만든 UUID라 재전송해도 같은 값이다(멱등 처리 기준). 원시 위치 좌표는 두지 않는다. */
@Getter
@Entity
@Table(name = "trip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {

	@Id
	@Column(name = "trip_id", length = 36)
	private String id;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "ended_at", nullable = false)
	private Instant endedAt;

	@Column(name = "distance_km", nullable = false)
	private double distanceKm;

	@Convert(converter = ScoreItemSetConverter.class)
	@Column(name = "supported_signals", nullable = false)
	private Set<ScoreItem> supportedSignals = EnumSet.noneOf(ScoreItem.class);

	private Integer score;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TripStatus status;

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("occurredAt")
	private List<TripEvent> events = new ArrayList<>();

	/** 업로드된 운행을 만든다. 최소 거리는 기준값 설정에서 받는다(D10). */
	public static Trip upload(String tripId, Long driverId, Instant startedAt, Instant endedAt, double distanceKm,
		Set<ScoreItem> supportedSignals, List<RecordedEvent> events, double minDistanceKm) {
		Trip trip = new Trip();
		trip.id = tripId;
		trip.driverId = driverId;
		trip.startedAt = startedAt;
		trip.endedAt = endedAt;
		trip.distanceKm = distanceKm;
		trip.supportedSignals.addAll(supportedSignals);
		trip.status = distanceKm < minDistanceKm ? TripStatus.EXCLUDED_SHORT : TripStatus.UPLOADED;
		events.forEach(event -> trip.events.add(new TripEvent(trip, event)));
		return trip;
	}
}
