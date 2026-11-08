package com.blinkerleague.server.score.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.trip.domain.TripEventType;

class ScoreCalculatorTest {

	private static final Thresholds THRESHOLDS = ThresholdsFixture.planExample();

	private static final Instant AT = Instant.parse("2026-11-03T08:12:30+09:00");

	private final ScoreCalculator calculator = new ScoreCalculator();

	@Test
	void 회전_4회_중_3회_깜빡이를_켜면_75점이다() {
		List<DrivingEvent> turns = List.of(
			turn(true), turn(true), turn(true), turn(false));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.TURN_SIGNAL), 12.0, turns), THRESHOLDS);

		assertThat(result.score()).isEqualTo(75);
	}

	@Test
	void 회전이_0회면_측정할_항목이_없어_점수가_없다() {
		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.TURN_SIGNAL), 12.0, List.of()), THRESHOLDS);

		assertThat(result.score()).isNull();
	}

	@Test
	void 차량이_모든_항목을_지원해도_P0은_깜빡이만_측정해_가중치_1로_재분배한다() {
		List<DrivingEvent> turns = List.of(
			turn(true), turn(true), turn(true), turn(false));

		ScoreResult result = calculator.calculate(new TripFacts(EnumSet.allOf(ScoreItem.class), 12.0, turns), THRESHOLDS);

		assertThat(result.metrics()).containsExactly(MetricScore.scored(ScoreItem.TURN_SIGNAL, 0.75, 75, 1.0, java.util.Map.of()));
		assertThat(result.score()).isEqualTo(75);
	}

	@Test
	void 차량이_깜빡이_신호를_지원하지_않으면_회전이_있어도_점수가_없다() {
		List<DrivingEvent> turns = List.of(turn(true), turn(false));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.SMOOTH_DRIVING), 12.0, turns), THRESHOLDS);

		assertThat(result.score()).isNull();
		assertThat(result.metrics()).isEmpty();
	}

	@Test
	void 회전이_아닌_이벤트는_깜빡이_점수에_세지_않는다() {
		List<DrivingEvent> events = List.of(turn(true), new DrivingEvent(TripEventType.LANE_CHANGE, AT, false));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.TURN_SIGNAL), 12.0, events), THRESHOLDS);

		assertThat(result.score()).isEqualTo(100);
	}

	private static DrivingEvent turn(boolean signalOn) {
		return new DrivingEvent(TripEventType.TURN, AT, signalOn);
	}
}
