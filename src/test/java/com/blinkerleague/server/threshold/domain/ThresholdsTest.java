package com.blinkerleague.server.threshold.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.Thresholds.GradeRule;

class ThresholdsTest {

	// 설정 순서와 상관없이 높은 기준부터 비교해야 하므로 일부러 섞어 둔다.
	private final Thresholds thresholds = ThresholdsFixture.withGrades(List.of(
		new GradeRule("B", 70),
		new GradeRule("S", 95),
		new GradeRule("C", 50),
		new GradeRule("A", 85)));

	@ParameterizedTest(name = "{0}점 → {1}")
	@CsvSource(value = {"100, S", "95, S", "94, A", "85, A", "84, B", "70, B", "69, C", "50, C", "49, NONE", "0, NONE"})
	void 점수는_높은_기준부터_비교해_처음_만족하는_등급이_되고_최저_기준_미만은_등급이_없다(int score, String grade) {
		Optional<String> expected = grade.equals("NONE") ? Optional.empty() : Optional.of(grade);

		assertThat(thresholds.gradeOf(score)).isEqualTo(expected);
	}
}
