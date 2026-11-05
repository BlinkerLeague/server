package com.blinkerleague.server.score.domain;

import java.time.Instant;
import java.util.List;

import com.blinkerleague.server.trip.domain.TripEventType;

/** 경기 비유 요약과 시각순 경기 기록(plan.md 3.5, D22). */
public record MatchReport(Summary summary, List<Play> plays) {

	public static MatchReport of(List<DrivingEvent> events) {
		throw new UnsupportedOperationException();
	}

	public record Summary(int goals, int conceded, int fouls) {
	}

	/** TURN이 아닌 이벤트는 result가 null이다. */
	public record Play(Instant occurredAt, TripEventType type, PlayResult result) {
	}
}
