package com.blinkerleague.server.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ReportWeekTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Test
	void ISO_주를_한국_시간_월요일_자정부터_다음_월요일_자정_전까지로_다룬다() {
		ReportWeek week = ReportWeek.parse("2026-W45");

		assertThat(week.key()).isEqualTo("2026-W45");
		// 2026-W45는 11월 2일(월) ~ 8일(일)
		assertThat(week.start(SEOUL)).isEqualTo(Instant.parse("2026-11-01T15:00:00Z"));
		assertThat(week.end(SEOUL)).isEqualTo(Instant.parse("2026-11-08T15:00:00Z"));
		assertThat(week.previous().key()).isEqualTo("2026-W44");
		assertThat(week.next().key()).isEqualTo("2026-W46");
		// 한국 시간 11월 9일(월) 00:30은 W46, 11월 8일(일) 23:59는 W45
		assertThat(ReportWeek.containing(Instant.parse("2026-11-08T15:30:00Z"), SEOUL).key()).isEqualTo("2026-W46");
		assertThat(ReportWeek.containing(Instant.parse("2026-11-08T14:59:00Z"), SEOUL).key()).isEqualTo("2026-W45");
		// 연도 경계: 2026-12-31(목)은 2026-W53, 2027-01-04(월)은 2027-W01
		assertThat(ReportWeek.parse("2026-W53").next().key()).isEqualTo("2027-W01");
	}
}
