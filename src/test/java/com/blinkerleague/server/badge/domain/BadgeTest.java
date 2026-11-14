package com.blinkerleague.server.badge.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BadgeTest {

	@ParameterizedTest(name = "{0} 목표 {1}, 기록(운행 {2}, 연속 {3}, 깜빡이 {4}, 우승 {5}) → {6}")
	@CsvSource({
		"SCORED_TRIPS,     1,   0, 0,  0,   0, false",
		"SCORED_TRIPS,     1,   1, 0,  0,   0, true",
		"STREAK_DAYS,      7,   9, 6,  99,  9, false",
		"STREAK_DAYS,      7,   0, 7,  0,   0, true",
		"SIGNAL_ON_TURNS,  100, 9, 9,  99,  9, false",
		"SIGNAL_ON_TURNS,  100, 0, 0,  100, 0, true",
		"SEASON_CHAMPIONS, 1,   9, 9,  999, 0, false",
		"SEASON_CHAMPIONS, 1,   0, 0,  0,   1, true"})
	void 배지의_조건_종류에_맞는_기록이_목표에_닿으면_획득한다(BadgeRule rule, int target, int scoredTrips,
		int longestStreakDays, int signalOnTurns, int seasonChampions, boolean expected) {
		Badge badge = new Badge("B", "배지", "조건", rule, target, 1);

		assertThat(badge.isEarnedBy(new BadgeProgress(scoredTrips, longestStreakDays, signalOnTurns, seasonChampions)))
			.isEqualTo(expected);
	}
}
