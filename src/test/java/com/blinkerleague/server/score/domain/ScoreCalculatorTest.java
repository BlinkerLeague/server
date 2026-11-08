package com.blinkerleague.server.score.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
	void 깜빡이만_지원하는_차량은_깜빡이_항목이_가중치_1로_재분배된다() {
		List<DrivingEvent> turns = List.of(
			turn(true), turn(true), turn(true), turn(false));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.TURN_SIGNAL), 12.0, turns), THRESHOLDS);

		assertThat(result.metrics()).filteredOn(m -> m.status() == MetricStatus.SCORED).singleElement()
			.satisfies(m -> {
				assertThat(m.metric()).isEqualTo(ScoreItem.TURN_SIGNAL);
				assertThat(m.score()).isEqualTo(75);
				assertThat(m.weight()).isEqualTo(1.0);
			});
		assertThat(result.score()).isEqualTo(75);
	}

	@Test
	void 차량이_측정할_수_있는_항목이_없으면_회전이_있어도_점수가_없다() {
		List<DrivingEvent> turns = List.of(turn(true), turn(false));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(), 12.0, turns), THRESHOLDS);

		assertThat(result.score()).isNull();
		assertThat(result.metrics()).allSatisfy(m -> assertThat(m.status()).isEqualTo(MetricStatus.UNSUPPORTED));
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

	@Test
	void 급가감속은_10km당_횟수만큼_감점하고_횟수와_10km당_횟수를_근거로_담는다() {
		List<DrivingEvent> events = List.of(turn(true), turn(true), turn(true), turn(false),
			new DrivingEvent(TripEventType.HARSH_ACCEL, AT, false),
			new DrivingEvent(TripEventType.HARSH_BRAKE, AT, false));

		ScoreResult result = calculator.calculate(
			new TripFacts(Set.of(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING), 14.0, events), THRESHOLDS);

		// 10km당 2 ÷ 14 × 10 = 1.43회 → 100 − 10 × 1.43 = 85.7점. 가중치 0.4 : 0.3 → 4/7 : 3/7
		assertThat(result.metrics()).filteredOn(m -> m.metric() == ScoreItem.SMOOTH_DRIVING).singleElement()
			.satisfies(m -> {
				assertThat(m.status()).isEqualTo(MetricStatus.SCORED);
				assertThat(m.score()).isEqualTo(86);
				assertThat(m.weight()).isCloseTo(3.0 / 7, org.assertj.core.data.Offset.offset(1e-9));
				assertThat(m.detail()).containsEntry("count", 2).containsEntry("per10Km", 1.4);
			});
		assertThat(result.score()).isEqualTo(80);
	}

	@Test
	void 측정하지_못한_항목도_상태와_사유를_담고_회전이_없으면_나머지_항목으로_평점한다() {
		ScoreResult result = calculator.calculate(
			new TripFacts(Set.of(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING), 12.0, List.of()), THRESHOLDS);

		assertThat(result.score()).isEqualTo(100);
		assertThat(result.metrics()).extracting(MetricScore::metric, MetricScore::status, MetricScore::reason,
				MetricScore::score, MetricScore::weight)
			.startsWith(
				org.assertj.core.groups.Tuple.tuple(ScoreItem.TURN_SIGNAL, MetricStatus.NOT_APPLICABLE, "NO_TURN", null, null),
				org.assertj.core.groups.Tuple.tuple(ScoreItem.SMOOTH_DRIVING, MetricStatus.SCORED, null, 100, 1.0),
				org.assertj.core.groups.Tuple.tuple(ScoreItem.SAFETY_WARNING, MetricStatus.UNSUPPORTED, "SIGNAL_NOT_SUPPORTED", null, null),
				org.assertj.core.groups.Tuple.tuple(ScoreItem.NIGHT_HEADLIGHT, MetricStatus.UNSUPPORTED, "SIGNAL_NOT_SUPPORTED", null, null));
	}

	@Test
	void 경고등은_10초_안의_중복을_1회로_세고_100km당_횟수만큼_감점한다() {
		List<DrivingEvent> events = List.of(
			new DrivingEvent(TripEventType.SAFETY_WARNING, AT, false),
			new DrivingEvent(TripEventType.SAFETY_WARNING, AT.plusSeconds(9), false),
			new DrivingEvent(TripEventType.SAFETY_WARNING, AT.plusSeconds(60), false));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.SAFETY_WARNING), 40.0, events), THRESHOLDS);

		// 2회(9초 뒤는 같은 경고) ÷ 40km × 100 = 100km당 5회 → 100 − 20 × 5 = 0점
		assertThat(result.metrics()).filteredOn(m -> m.metric() == ScoreItem.SAFETY_WARNING).singleElement()
			.satisfies(m -> {
				assertThat(m.score()).isZero();
				assertThat(m.detail()).containsEntry("count", 2).containsEntry("per100Km", 5.0);
			});
	}

	@Test
	void 야간_전조등은_야간_구간에서_켜_둔_비율이고_야간_구간이_없으면_측정하지_않는다() {
		List<DrivingEvent> night = List.of(nightSegment(600, 60), nightSegment(400, 40));

		ScoreResult scored = calculator.calculate(new TripFacts(Set.of(ScoreItem.NIGHT_HEADLIGHT), 12.0, night), THRESHOLDS);
		ScoreResult day = calculator.calculate(new TripFacts(Set.of(ScoreItem.NIGHT_HEADLIGHT), 12.0, List.of()), THRESHOLDS);

		// 꺼진 100초 ÷ 야간 1000초 = 10% → 90점
		assertThat(scored.metrics()).filteredOn(m -> m.metric() == ScoreItem.NIGHT_HEADLIGHT).singleElement()
			.satisfies(m -> {
				assertThat(m.score()).isEqualTo(90);
				assertThat(m.detail()).containsEntry("durationSec", 1000).containsEntry("headlightOffSec", 100);
			});
		assertThat(day.metrics()).filteredOn(m -> m.metric() == ScoreItem.NIGHT_HEADLIGHT).singleElement()
			.satisfies(m -> {
				assertThat(m.status()).isEqualTo(MetricStatus.NOT_APPLICABLE);
				assertThat(m.reason()).isEqualTo("NO_NIGHT");
			});
	}

	private static DrivingEvent nightSegment(int durationSec, int headlightOffSec) {
		return new DrivingEvent(TripEventType.NIGHT_SEGMENT, AT,
			new EventPayload(false, null, null, durationSec, headlightOffSec, null, null, null, null));
	}

	@Test
	void 회전_깜빡이는_제때_1점_늦게_0_5점_반대_방향과_안_켬은_0점의_평균이고_단계가_없으면_켬_여부로_본다() {
		List<DrivingEvent> turns = List.of(timed(SignalTiming.ON_TIME), timed(SignalTiming.LATE),
			timed(SignalTiming.WRONG_DIRECTION), timed(SignalTiming.NONE), turn(true));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.TURN_SIGNAL), 12.0, turns), THRESHOLDS);

		// (1 + 0.5 + 0 + 0 + 1) ÷ 5 = 0.5
		assertThat(result.metrics()).filteredOn(m -> m.metric() == ScoreItem.TURN_SIGNAL).singleElement()
			.satisfies(m -> {
				assertThat(m.score()).isEqualTo(50);
				assertThat(m.detail()).containsEntry("turns", 5).containsEntry("onTime", 2)
					.containsEntry("late", 1).containsEntry("missed", 2);
			});
	}

	private static DrivingEvent timed(SignalTiming timing) {
		boolean on = timing == SignalTiming.ON_TIME || timing == SignalTiming.LATE;
		return new DrivingEvent(TripEventType.TURN, AT, new EventPayload(on, timing, null, null, null, null, null, null, null));
	}

	@Test
	void 회전이_최소_표본보다_적으면_깜빡이_항목은_표본_부족으로_측정하지_않는다() {
		Thresholds minThree = ThresholdsFixture.withMinSamples(java.util.Map.of(ScoreItem.TURN_SIGNAL, 3));

		ScoreResult two = calculator.calculate(new TripFacts(Set.of(ScoreItem.TURN_SIGNAL), 12.0,
			List.of(turn(true), turn(true))), minThree);
		ScoreResult three = calculator.calculate(new TripFacts(Set.of(ScoreItem.TURN_SIGNAL), 12.0,
			List.of(turn(true), turn(true), turn(false))), minThree);

		assertThat(two.score()).isNull();
		assertThat(two.metrics()).filteredOn(m -> m.metric() == ScoreItem.TURN_SIGNAL).singleElement()
			.satisfies(m -> assertThat(m.reason()).isEqualTo("TOO_FEW_SAMPLES"));
		assertThat(three.score()).isEqualTo(67);
	}

	@Test
	void 깜빡이_끄기를_잊은_횟수마다_감점하고_횟수를_근거로_담는다() {
		List<DrivingEvent> events = List.of(
			new DrivingEvent(TripEventType.SIGNAL_LEFT_ON, AT, EventPayload.EMPTY),
			new DrivingEvent(TripEventType.SIGNAL_LEFT_ON, AT.plusSeconds(300), EventPayload.EMPTY));

		ScoreResult result = calculator.calculate(new TripFacts(Set.of(ScoreItem.SIGNAL_CANCEL), 12.0, events), THRESHOLDS);

		// 100 − 25 × 2 = 50
		assertThat(result.metrics()).filteredOn(m -> m.metric() == ScoreItem.SIGNAL_CANCEL).singleElement()
			.satisfies(m -> {
				assertThat(m.score()).isEqualTo(50);
				assertThat(m.detail()).containsEntry("count", 2);
			});
	}
}
