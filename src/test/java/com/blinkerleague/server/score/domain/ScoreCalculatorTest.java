package com.blinkerleague.server.score.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
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

	@Test
	void 회전이_0회면_측정할_항목이_없어_점수가_없다() {
		ScoreResult result = calculator.calculate(Set.of(ScoreItem.TURN_SIGNAL), List.of(), WEIGHTS);

		assertThat(result.score()).isNull();
	}

	@Test
	void 차량이_모든_항목을_지원해도_P0은_깜빡이만_측정해_가중치_1로_재분배한다() {
		List<TurnRecord> turns = List.of(
			new TurnRecord(true), new TurnRecord(true), new TurnRecord(true), new TurnRecord(false));

		ScoreResult result = calculator.calculate(EnumSet.allOf(ScoreItem.class), turns, WEIGHTS);

		assertThat(result.metrics()).containsExactly(new MetricScore(ScoreItem.TURN_SIGNAL, 0.75, 75, 1.0));
		assertThat(result.score()).isEqualTo(75);
	}
}
