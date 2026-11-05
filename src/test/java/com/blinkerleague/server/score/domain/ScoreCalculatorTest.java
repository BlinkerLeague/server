package com.blinkerleague.server.score.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.domain.ScoreItem;

class ScoreCalculatorTest {

	private static final Map<ScoreItem, Double> WEIGHTS = Map.of(
		ScoreItem.TURN_SIGNAL, 0.4,
		ScoreItem.SMOOTH_DRIVING, 0.3,
		ScoreItem.SAFETY_WARNING, 0.2,
		ScoreItem.NIGHT_HEADLIGHT, 0.1);

	private final ScoreCalculator calculator = new ScoreCalculator();

	@Test
	void 회전_4회_중_3회_깜빡이를_켜면_75점이다() {
		List<TurnRecord> turns = List.of(
			new TurnRecord(true), new TurnRecord(true), new TurnRecord(true), new TurnRecord(false));

		ScoreResult result = calculator.calculate(Set.of(ScoreItem.TURN_SIGNAL), turns, WEIGHTS);

		assertThat(result.score()).isEqualTo(75);
	}
}
