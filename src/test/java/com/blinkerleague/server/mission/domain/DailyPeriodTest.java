package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DailyPeriodTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Test
	void 한국_시간_자정을_기준으로_날짜_키가_바뀐다() {
		// 2026-11-02T14:59:59Z = 11.2 23:59:59 KST, 2026-11-02T15:00:00Z = 11.3 00:00:00 KST
		assertThat(DailyPeriod.of(Instant.parse("2026-11-02T14:59:59Z"), SEOUL).key()).isEqualTo("2026-11-02");
		assertThat(DailyPeriod.of(Instant.parse("2026-11-02T15:00:00Z"), SEOUL).key()).isEqualTo("2026-11-03");
	}

	@Test
	void 하루는_한국_시간_자정부터_다음_자정_직전까지다() {
		DailyPeriod period = DailyPeriod.of(Instant.parse("2026-11-03T03:00:00Z"), SEOUL);

		assertThat(period.start()).isEqualTo(Instant.parse("2026-11-02T15:00:00Z"));
		assertThat(period.end()).isEqualTo(Instant.parse("2026-11-03T15:00:00Z"));
	}
}
