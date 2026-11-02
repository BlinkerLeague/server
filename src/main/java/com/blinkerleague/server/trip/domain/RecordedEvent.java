package com.blinkerleague.server.trip.domain;

import java.time.Instant;

/** 앱이 운행 중 기록해 보낸 이벤트. payload는 해석하지 않고 JSON 문자열 그대로 둔다. */
public record RecordedEvent(String eventId, TripEventType type, Instant occurredAt, String payload) {
}
