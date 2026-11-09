package com.blinkerleague.server.score.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.domain.ScoreItem;

class LeagueScoreTest {

	private static final Map<ScoreItem, Double> WEIGHTS = Map.of(
		ScoreItem.TURN_SIGNAL, 0.25, ScoreItem.SMOOTH_DRIVING, 0.15, ScoreItem.TURN_SPEED, 0.05,
		ScoreItem.SEAT_BELT, 0.05);
	private static final Set<ScoreItem> COMMON = Set.of(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING,
		ScoreItem.TURN_SPEED);

	@Test
	void 공통_항목_중_측정한_항목만으로_가중치를_다시_나눠_리그_점수를_매긴다() {
		// 깜빡이 100점(0.25) · 가감속 60점(0.15)만 공통 항목으로 측정, 안전벨트 0점은 공통 항목이 아니라 빠진다
		List<MetricScore> metrics = List.of(
			MetricScore.scored(ScoreItem.TURN_SIGNAL, 1.0, 100, 0.5, Map.of()),
			MetricScore.scored(ScoreItem.SMOOTH_DRIVING, 0.6, 60, 0.3, Map.of()),
			MetricScore.excluded(ScoreItem.TURN_SPEED, MetricStatus.NOT_APPLICABLE, "NO_SPEED"),
			MetricScore.scored(ScoreItem.SEAT_BELT, 0.0, 0, 0.2, Map.of()));

		// (100 × 0.25 + 60 × 0.15) ÷ 0.4 = 85
		assertThat(LeagueScore.of(metrics, WEIGHTS, COMMON)).isEqualTo(85);
	}

	@Test
	void 공통_항목을_하나도_측정하지_않으면_리그_점수가_없다() {
		List<MetricScore> metrics = List.of(
			MetricScore.excluded(ScoreItem.TURN_SIGNAL, MetricStatus.NOT_APPLICABLE, "NO_TURN"),
			MetricScore.scored(ScoreItem.SEAT_BELT, 1.0, 100, 1.0, Map.of()));

		assertThat(LeagueScore.of(metrics, WEIGHTS, COMMON)).isNull();
	}
}
