package com.blinkerleague.server.score.domain;

import java.time.Instant;

import com.blinkerleague.server.trip.domain.TripEventType;

/** 경기 기록을 만들 운행 이벤트. signalOn은 TURN에만 의미가 있다. */
public record DrivingEvent(TripEventType type, Instant occurredAt, boolean signalOn) {
}
