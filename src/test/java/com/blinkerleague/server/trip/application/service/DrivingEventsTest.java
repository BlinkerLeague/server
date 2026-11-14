package com.blinkerleague.server.trip.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.score.domain.EventPayload;
import com.blinkerleague.server.score.domain.SignalTiming;
import com.blinkerleague.server.score.domain.TurnDirection;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.TripEventType;

class DrivingEventsTest {

	private static final Instant AT = Instant.parse("2026-11-03T00:10:00Z");

	@Test
	void 점수에_쓰는_payload_값을_읽고_모르는_값과_잘못된_JSON은_없는_값으로_둔다() {
		List<EventPayload> payloads = DrivingEvents.fromRecorded(List.of(
			event(TripEventType.TURN, "{\"signalOn\":true,\"signalTiming\":\"LATE\",\"maxSpeedKmh\":25.5}"),
			event(TripEventType.TURN, "{\"signalOn\":true,\"signalTiming\":\"NONE\"}"),
			event(TripEventType.TURN, "{\"signalTiming\":\"SOMETIMES\",\"signalOn\":false}"),
			event(TripEventType.NIGHT_SEGMENT, "{\"durationSec\":600,\"headlightOffSec\":60}"),
			event(TripEventType.RAIN_SEGMENT, "{\"durationSec\":300,\"headlightOffSec\":0,\"knownLimitSec\":200,\"overRainLimitSec\":20}"),
			event(TripEventType.SEAT_BELT, "{\"buckledAtDeparture\":false,\"unbuckledMovingSec\":90}"),
			event(TripEventType.HARSH_BRAKE, "{not json"))).stream().map(e -> e.payload()).toList();

		assertThat(payloads.get(0)).isEqualTo(new EventPayload(true, SignalTiming.LATE, 25.5, null, null, null, null, null, null));
		// 단계가 있으면 단계로 켬 여부를 정한다(NONE은 signalOn이 true여도 안 켬).
		assertThat(payloads.get(1).signalOn()).isFalse();
		assertThat(payloads.get(2)).isEqualTo(EventPayload.turn(false));
		assertThat(payloads.get(3)).isEqualTo(new EventPayload(false, null, null, 600, 60, null, null, null, null));
		assertThat(payloads.get(4)).isEqualTo(new EventPayload(false, null, null, 300, 0, 200, 20, null, null));
		assertThat(payloads.get(5)).isEqualTo(new EventPayload(false, null, null, null, null, null, null, false, 90));
		assertThat(payloads.get(6)).isEqualTo(EventPayload.EMPTY);
	}

	private static RecordedEvent event(TripEventType type, String payload) {
		return new RecordedEvent("e-" + type + payload.hashCode(), type, AT, payload);
	}

	@Test
	void 회전_방향을_읽고_LEFT_RIGHT가_아니면_없는_값으로_둔다() {
		List<TurnDirection> directions = DrivingEvents.fromRecorded(List.of(
			event(TripEventType.TURN, "{\"direction\":\"LEFT\",\"signalOn\":true}"),
			event(TripEventType.TURN, "{\"direction\":\"RIGHT\",\"signalOn\":false}"),
			event(TripEventType.TURN, "{\"direction\":\"UP\",\"signalOn\":false}"))).stream()
			.map(e -> e.payload().direction()).toList();

		assertThat(directions).containsExactly(TurnDirection.LEFT, TurnDirection.RIGHT, null);
	}
}
