package com.blinkerleague.server.badge.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LongestStreakTest {

	private static final LocalDate D = LocalDate.of(2026, 11, 1);

	@Test
	void 달성한_날들_중_가장_길게_이어진_일수를_센다() {
		Set<LocalDate> achieved = Set.of(D, D.plusDays(1), D.plusDays(2),
			D.plusDays(4), D.plusDays(5),
			D.minusDays(30));

		assertThat(LongestStreak.of(achieved)).isEqualTo(3);
		assertThat(LongestStreak.of(Set.of())).isZero();
	}
}
