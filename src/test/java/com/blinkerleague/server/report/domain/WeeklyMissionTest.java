package com.blinkerleague.server.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.blinkerleague.server.threshold.domain.ScoreItem;

class WeeklyMissionTest {

	@ParameterizedTest(name = "평균 {0}점, {1}km → {2}")
	@CsvSource({
		"100, 50.0, ACHIEVED",
		"100, 49.9, FAILED",
		"99,  80.0, FAILED",
		",    80.0, FAILED"})
	void 그_주_항목_평균이_목표_이상이고_최소_거리를_달렸으면_달성이다(Integer average, double distanceKm,
		WeeklyMissionStatus expected) {
		WeeklyMission mission = new WeeklyMission(1L, "2026-W46", ScoreItem.SAFETY_WARNING, "안전 경고 없는 주", 100, 50.0);

		mission.judge(average, distanceKm);

		assertThat(mission.getStatus()).isEqualTo(expected);
	}
}
