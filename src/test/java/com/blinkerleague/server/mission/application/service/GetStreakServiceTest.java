package com.blinkerleague.server.mission.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GetStreakServiceTest {

	// 한국 시간 2026-11-08 00:30 (UTC로는 아직 11-07)
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-07T15:30:00Z"), ZoneId.of("Asia/Seoul"));

	@Test
	void 한국_날짜_기준으로_연속_달성_일수와_오늘_달성_여부를_돌려준다() {
		GetStreakService service = new GetStreakService(
			driverId -> Set.of(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 7)), clock);

		assertThat(service.get(1L)).isEqualTo(new GetStreakService.StreakView(2, true));
	}
}
