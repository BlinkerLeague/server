package com.blinkerleague.server.trip.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운행 중 이벤트. id는 앱이 만든 UUID다. */
@Getter
@Entity
@Table(name = "trip_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripEvent {

	@Id
	@Column(name = "event_id", length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TripEventType type;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Lob
	private String payload;

	TripEvent(Trip trip, RecordedEvent event) {
		this.id = event.eventId();
		this.trip = trip;
		this.type = event.type();
		this.occurredAt = event.occurredAt();
		this.payload = event.payload();
	}
}
