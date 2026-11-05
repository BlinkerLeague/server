package com.blinkerleague.server.score.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.score.domain.MatchReport.Play;
import com.blinkerleague.server.score.domain.MatchReport.Summary;
import com.blinkerleague.server.trip.domain.TripEventType;

class MatchReportTest {

	private static final Instant T1 = Instant.parse("2026-11-03T08:12:30+09:00");
	private static final Instant T2 = Instant.parse("2026-11-03T08:14:00+09:00");
	private static final Instant T3 = Instant.parse("2026-11-03T08:15:02+09:00");

	@Test
	void 깜빡이를_켠_회전은_득점_안_켠_회전은_실점이고_모든_이벤트를_시각순으로_기록한다() {
		List<DrivingEvent> events = List.of(
			new DrivingEvent(TripEventType.TURN, T3, false),
			new DrivingEvent(TripEventType.TURN, T1, true),
			new DrivingEvent(TripEventType.LANE_CHANGE, T2, false));

		MatchReport report = MatchReport.of(events);

		assertThat(report.summary()).isEqualTo(new Summary(1, 1, 0));
		assertThat(report.plays()).containsExactly(
			new Play(T1, TripEventType.TURN, PlayResult.GOAL),
			new Play(T2, TripEventType.LANE_CHANGE, null),
			new Play(T3, TripEventType.TURN, PlayResult.CONCEDED));
	}
}
