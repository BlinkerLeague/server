package com.blinkerleague.server.trip.application.service;

import java.time.Instant;
import java.util.List;

import com.blinkerleague.server.score.domain.DrivingEvent;
import com.blinkerleague.server.score.domain.EventPayload;
import com.blinkerleague.server.score.domain.SignalTiming;
import com.blinkerleague.server.score.domain.TurnDirection;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.TripEvent;
import com.blinkerleague.server.trip.domain.TripEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 저장된 운행 이벤트를 점수 엔진 입력으로 바꾼다. payload는 원본 JSON이며 점수에 쓰는 값만 읽는다(plan.md 4.1).
 * 없거나 형식이 맞지 않는 값은 null로 둔다(그 항목은 측정하지 않거나 기본값으로 계산).
 */
final class DrivingEvents {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private DrivingEvents() {
	}

	static List<DrivingEvent> of(List<TripEvent> events) {
		return events.stream().map(e -> toDrivingEvent(e.getType(), e.getOccurredAt(), e.getPayload())).toList();
	}

	static List<DrivingEvent> fromRecorded(List<RecordedEvent> events) {
		return events.stream().map(e -> toDrivingEvent(e.type(), e.occurredAt(), e.payload())).toList();
	}

	private static DrivingEvent toDrivingEvent(TripEventType type, Instant occurredAt, String payload) {
		return new DrivingEvent(type, occurredAt, parse(type, payload));
	}

	private static EventPayload parse(TripEventType type, String payload) {
		JsonNode json = read(payload);
		if (json == null) {
			return type == TripEventType.TURN ? EventPayload.turn(false) : EventPayload.EMPTY;
		}
		SignalTiming timing = type == TripEventType.TURN ? timing(json.path("signalTiming")) : null;
		return new EventPayload(type == TripEventType.TURN && signalOn(json, timing), timing,
			number(json.path("maxSpeedKmh")), integer(json.path("durationSec")), integer(json.path("headlightOffSec")),
			integer(json.path("knownLimitSec")), integer(json.path("overRainLimitSec")),
			bool(json.path("buckledAtDeparture")), integer(json.path("unbuckledMovingSec")),
			type == TripEventType.TURN ? direction(json.path("direction")) : null);
	}

	// 단계가 있으면 제때 · 늦게만 켠 회전이다. 없으면 signalOn이 boolean true일 때만 켠 회전이다(D23).
	private static boolean signalOn(JsonNode json, SignalTiming timing) {
		if (timing != null) {
			return timing == SignalTiming.ON_TIME || timing == SignalTiming.LATE;
		}
		JsonNode signalOn = json.path("signalOn");
		return signalOn.isBoolean() && signalOn.booleanValue();
	}

	private static SignalTiming timing(JsonNode node) {
		if (!node.isTextual()) {
			return null;
		}
		try {
			return SignalTiming.valueOf(node.textValue());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static TurnDirection direction(JsonNode node) {
		if (!node.isTextual()) {
			return null;
		}
		try {
			return TurnDirection.valueOf(node.textValue());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static Double number(JsonNode node) {
		return node.isNumber() ? node.doubleValue() : null;
	}

	private static Integer integer(JsonNode node) {
		return node.isNumber() ? node.intValue() : null;
	}

	private static Boolean bool(JsonNode node) {
		return node.isBoolean() ? node.booleanValue() : null;
	}

	private static JsonNode read(String payload) {
		if (payload == null) {
			return null;
		}
		try {
			return MAPPER.readTree(payload);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
