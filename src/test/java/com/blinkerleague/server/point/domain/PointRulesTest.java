package com.blinkerleague.server.point.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PointRulesTest {

	@ParameterizedTest(name = "{0}km × {1}점 → {2}P")
	@CsvSource({
		"12.3, 86, 10",
		"50.0, 100, 50",
		"80.0, 100, 50",
		"80.0, 50, 25",
		"3.0, 0, 0"})
	void 운행_적립은_최대_거리까지의_거리에_점수_비율을_곱해_내린다(double distanceKm, int score, int expected) {
		assertThat(PointRules.tripPoints(distanceKm, score, 50)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "오늘 {1}P 적립 상태에서 {0}P 요청 → {2}P")
	@CsvSource({
		"20, 0, 20",
		"20, 130, 20",
		"20, 140, 10",
		"20, 150, 0",
		"20, 170, 0"})
	void 일일_상한을_넘는_적립은_남은_만큼만_준다(int requested, int todayEarned, int expected) {
		assertThat(PointRules.capToDailyLimit(requested, todayEarned, 150)).isEqualTo(expected);
	}
}
