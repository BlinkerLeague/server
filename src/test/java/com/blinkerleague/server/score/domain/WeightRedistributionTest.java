package com.blinkerleague.server.score.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.domain.ScoreItem;

class WeightRedistributionTest {

	private static final Map<ScoreItem, Double> WEIGHTS = Map.of(
		ScoreItem.TURN_SIGNAL, 0.4,
		ScoreItem.SMOOTH_DRIVING, 0.3,
		ScoreItem.SAFETY_WARNING, 0.2,
		ScoreItem.NIGHT_HEADLIGHT, 0.1);

	@Test
	void 측정한_항목만_남기고_가중치_합이_1이_되도록_비율대로_나눈다() {
		Map<ScoreItem, Double> result = WeightRedistribution.redistribute(
			WEIGHTS, Set.of(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING));

		assertThat(result).containsOnlyKeys(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING);
		assertThat(result.get(ScoreItem.TURN_SIGNAL)).isCloseTo(0.4 / 0.7, within(1e-9));
		assertThat(result.get(ScoreItem.SMOOTH_DRIVING)).isCloseTo(0.3 / 0.7, within(1e-9));
		assertThat(result.values().stream().mapToDouble(Double::doubleValue).sum()).isCloseTo(1.0, within(1e-9));
	}
}
