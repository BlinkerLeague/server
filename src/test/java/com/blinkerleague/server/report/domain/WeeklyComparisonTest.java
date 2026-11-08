package com.blinkerleague.server.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.domain.ScoreItem;

class WeeklyComparisonTest {

	@Test
	void 항목별_이번_주와_지난주_점수와_변화를_항목_순서로_담고_가장_약한_항목을_고른다() {
		WeeklyComparison comparison = WeeklyComparison.of(
			Map.of(ScoreItem.TURN_SIGNAL, 82, ScoreItem.SAFETY_WARNING, 60, ScoreItem.SEAT_BELT, 60),
			Map.of(ScoreItem.TURN_SIGNAL, 75, ScoreItem.SMOOTH_DRIVING, 90));

		assertThat(comparison.metrics()).containsExactly(
			new MetricChange(ScoreItem.TURN_SIGNAL, 82, 75, 7),
			new MetricChange(ScoreItem.SMOOTH_DRIVING, null, 90, null),
			new MetricChange(ScoreItem.SAFETY_WARNING, 60, null, null),
			new MetricChange(ScoreItem.SEAT_BELT, 60, null, null));
		// 60점 동점이면 항목 순서가 앞선 SAFETY_WARNING
		assertThat(comparison.weakest()).contains(ScoreItem.SAFETY_WARNING);
		assertThat(WeeklyComparison.of(Map.of(), Map.of(ScoreItem.TURN_SIGNAL, 75)).weakest()).isEmpty();
	}
}
