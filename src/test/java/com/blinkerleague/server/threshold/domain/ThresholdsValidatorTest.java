package com.blinkerleague.server.threshold.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.Thresholds.GradeRule;

class ThresholdsValidatorTest {

	@Test
	void plan_예시_기준값은_위반이_없다() {
		// when & then
		assertThat(ThresholdsValidator.validate(ThresholdsFixture.planExample())).isEmpty();
	}

	@Test
	void 가중치에_음수가_있으면_위반이다() {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith("weights.SMOOTH_DRIVING", -0.1);

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("weights.SMOOTH_DRIVING 값은 0 이상이어야 합니다.");
	}

	@Test
	void 가중치_합이_0이면_위반이다() {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith("weights", Map.of(ScoreItem.TURN_SIGNAL, 0.0));

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("weights 합은 0보다 커야 합니다.");
	}

	@ParameterizedTest(name = "기준 점수 {0}")
	@ValueSource(ints = {-1, 101})
	void 등급_기준_점수가_0에서_100_밖이면_위반이다(int minScore) {
		// given
		Thresholds thresholds = ThresholdsFixture.withGrades(List.of(
			new GradeRule("S", minScore),
			new GradeRule("C", 50)));

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("grades.S 기준 점수는 0~100 사이여야 합니다.");
	}

	@Test
	void 등급_이름이나_기준_점수가_겹치면_위반이다() {
		// given
		// A가 두 번, 70점이 두 번 나온다
		Thresholds thresholds = ThresholdsFixture.withGrades(List.of(
			new GradeRule("S", 95),
			new GradeRule("A", 85),
			new GradeRule("A", 70),
			new GradeRule("B", 70)));

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("grades 등급 이름이 겹칩니다: A", "grades 기준 점수가 겹칩니다: 70");
	}

	@Test
	void 리그_공통_항목이_비어_있으면_위반이다() {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith("league.commonItems", List.of());

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("league.commonItems는 하나 이상이어야 합니다.");
	}

	@Test
	void 최소_표본이_1보다_작으면_위반이다() {
		// given
		Thresholds thresholds = ThresholdsFixture.withMinSamples(Map.of(ScoreItem.TURN_SIGNAL, 0));

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("minSamples.TURN_SIGNAL 값은 1 이상이어야 합니다.");
	}

	@ParameterizedTest(name = "{0} = {1}")
	@CsvSource({"turn.lateSignalScore, -0.1", "turn.lateSignalScore, 1.1", "rain.speedRatio, -0.1", "rain.speedRatio, 1.1"})
	void 비율_값이_0에서_1_밖이면_위반이다(String path, double value) {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith(path, value);

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly(path + " 값은 0~1 사이여야 합니다.");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"trip.minDistanceKm", "trip.maxFutureMinutes", "harsh.penaltyPer10Km",
		"safetyWarning.penaltyPer100Km", "signalCancel.penalty", "seatBelt.departurePenalty",
		"seatBelt.perMinutePenalty", "point.tripMaxKm", "point.dailyCap", "point.streakRewardPoint",
		"league.minSeasonDistanceKm", "league.minSeasonTrips"})
	void 거리_포인트_감점_값이_음수면_위반이다(String path) {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith(path, -1);

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly(path + " 값은 0 이상이어야 합니다.");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"trip.maxEvents", "trip.maxPastDays", "point.streakRewardDays",
		"league.minParticipants", "league.topN"})
	void 개수_기간_값이_1보다_작으면_위반이다(String path) {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith(path, 0);

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly(path + " 값은 1 이상이어야 합니다.");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"trip", "turn", "harsh", "safetyWarning", "signalCancel", "seatBelt", "rain", "weights",
		"minSamples", "point", "league", "grades", "tips"})
	void 그룹이_빠지면_위반이다(String group) {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith(group, null);

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly(group + " 값이 없습니다.");
	}
}
