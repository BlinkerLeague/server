package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StreakTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

	@Test
	void 오늘_달성했으면_오늘부터_거꾸로_이어진_달성_일수를_센다() {
		Set<LocalDate> achieved = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(4));

		assertThat(Streak.current(achieved, TODAY)).isEqualTo(3);
	}

	@Test
	void 오늘_아직_달성하지_않았으면_어제부터_센다() {
		Set<LocalDate> achieved = Set.of(TODAY.minusDays(1), TODAY.minusDays(2));

		assertThat(Streak.current(achieved, TODAY)).isEqualTo(2);
		assertThat(Streak.current(Set.of(TODAY.minusDays(2)), TODAY)).isZero();
	}
}
