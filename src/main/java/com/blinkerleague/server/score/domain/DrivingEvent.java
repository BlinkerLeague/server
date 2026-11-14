package com.blinkerleague.server.score.domain;

import java.time.Instant;

import com.blinkerleague.server.trip.domain.TripEventType;

/** 점수 · 경기 기록을 만들 운행 이벤트. */
public record DrivingEvent(TripEventType type, Instant occurredAt, EventPayload payload) {

	public DrivingEvent(TripEventType type, Instant occurredAt, boolean signalOn) {
		this(type, occurredAt, EventPayload.turn(signalOn));
	}

	/** TURN에서 깜빡이를 켰는지(P0 2단계 판정). */
	public boolean signalOn() {
		return payload.signalOn();
	}
}
