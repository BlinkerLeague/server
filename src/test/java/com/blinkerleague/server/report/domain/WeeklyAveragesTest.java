package com.blinkerleague.server.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.domain.ScoreItem;

class WeeklyAveragesTest {

	@Test
	void 항목별로_운행_거리로_가중한_평균_점수를_낸다() {
		List<TripMetricRecord> records = List.of(
			new TripMetricRecord("a", ScoreItem.TURN_SIGNAL, 100, 30.0),
			new TripMetricRecord("b", ScoreItem.TURN_SIGNAL, 50, 10.0),
			new TripMetricRecord("a", ScoreItem.SMOOTH_DRIVING, 80, 30.0));

		// 깜빡이 (100 × 30 + 50 × 10) ÷ 40 = 87.5 → 88
		assertThat(WeeklyAverages.of(records))
			.containsOnlyKeys(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING)
			.containsEntry(ScoreItem.TURN_SIGNAL, 88)
			.containsEntry(ScoreItem.SMOOTH_DRIVING, 80);
	}
}
