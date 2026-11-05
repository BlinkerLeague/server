package com.blinkerleague.server.trip.application.service;

import java.time.Instant;
import java.util.List;

import com.blinkerleague.server.score.domain.DrivingEvent;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.TripEvent;
import com.blinkerleague.server.trip.domain.TripEventType;

/** 저장된 운행 이벤트를 점수 엔진 입력으로 바꾼다. TURN의 signalOn은 payload에서 읽는다(D23). */
final class DrivingEvents {

	private DrivingEvents() {
	}

	static List<DrivingEvent> of(List<TripEvent> events) {
		return events.stream().map(e -> toDrivingEvent(e.getType(), e.getOccurredAt(), e.getPayload())).toList();
	}

	static List<DrivingEvent> fromRecorded(List<RecordedEvent> events) {
		return events.stream().map(e -> toDrivingEvent(e.type(), e.occurredAt(), e.payload())).toList();
	}

	private static DrivingEvent toDrivingEvent(TripEventType type, Instant occurredAt, String payload) {
		return new DrivingEvent(type, occurredAt, type == TripEventType.TURN && TurnPayload.signalOn(payload));
	}
}
