package com.blinkerleague.server.report.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import com.blinkerleague.server.report.application.service.GenerateLastWeekReportsService;
import com.blinkerleague.server.report.application.service.GenerateWeeklyReportService;

class WeeklyReportSchedulerTest {

	@Test
	void 매주_월요일_4시_한국_시간에_지난주_리포트_배치를_돌린다() throws Exception {
		Scheduled scheduled = WeeklyReportScheduler.class.getMethod("run").getAnnotation(Scheduled.class);
		assertThat(scheduled).isNotNull();
		assertThat(scheduled.cron()).isEqualTo("0 0 4 * * MON");
		assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");

		List<Instant> asked = new ArrayList<>();
		ZoneId seoul = ZoneId.of("Asia/Seoul");
		GenerateLastWeekReportsService batch = new GenerateLastWeekReportsService((from, to) -> {
			asked.add(from);
			return List.of();
		}, new GenerateWeeklyReportService(null, null, null, metric -> Optional.empty(), seoul),
			Clock.fixed(Instant.parse("2026-11-08T19:00:00Z"), seoul));

		new WeeklyReportScheduler(batch).run();

		assertThat(asked).hasSize(1);
	}
}
