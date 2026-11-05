package com.blinkerleague.server.score.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.blinkerleague.server.trip.domain.TripEventType;

/** 경기 비유 요약과 시각순 경기 기록(plan.md 3.5, D22). */
public record MatchReport(Summary summary, List<Play> plays) {

	public static MatchReport of(List<DrivingEvent> events) {
		List<Play> plays = events.stream()
			.sorted(Comparator.comparing(DrivingEvent::occurredAt))
			.map(event -> new Play(event.occurredAt(), event.type(), resultOf(event)))
			.toList();
		int goals = (int) plays.stream().filter(play -> play.result() == PlayResult.GOAL).count();
		int conceded = (int) plays.stream().filter(play -> play.result() == PlayResult.CONCEDED).count();
		return new MatchReport(new Summary(goals, conceded, 0), plays);
	}

	private static PlayResult resultOf(DrivingEvent event) {
		if (event.type() != TripEventType.TURN) {
			return null;
		}
		return event.signalOn() ? PlayResult.GOAL : PlayResult.CONCEDED;
	}

	public record Summary(int goals, int conceded, int fouls) {
	}

	/** TURN이 아닌 이벤트는 result가 null이다. */
	public record Play(Instant occurredAt, TripEventType type, PlayResult result) {
	}
}
