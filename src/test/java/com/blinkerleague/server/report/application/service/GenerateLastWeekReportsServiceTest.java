package com.blinkerleague.server.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.report.domain.ReportWeek;
import com.blinkerleague.server.report.domain.WeekDriving;

class GenerateLastWeekReportsServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	// 한국 시간 2026-11-09(월) 04:00 → 지난주 W45
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T19:00:00Z"), SEOUL);

	@Test
	void 지난주에_운행한_운전자마다_지난주_리포트를_만들고_한_명이_실패해도_계속한다() {
		List<Instant[]> askedRanges = new ArrayList<>();
		InMemoryWeeklyStores stores = new InMemoryWeeklyStores();
		GenerateWeeklyReportService generator = new GenerateWeeklyReportService((driverId, from, to) -> {
			if (driverId == 2L) {
				throw new IllegalStateException("일시 장애");
			}
			return new WeekDriving(0, 0, List.of());
		}, stores, stores, metric -> Optional.empty(), SEOUL);
		GenerateLastWeekReportsService batch = new GenerateLastWeekReportsService((from, to) -> {
			askedRanges.add(new Instant[] {from, to});
			return List.of(1L, 2L, 3L);
		}, generator, clock);

		int generated = batch.generate();

		ReportWeek w45 = ReportWeek.parse("2026-W45");
		assertThat(askedRanges).singleElement().satisfies(range -> {
			assertThat(range[0]).isEqualTo(w45.start(SEOUL));
			assertThat(range[1]).isEqualTo(w45.end(SEOUL));
		});
		assertThat(generated).isEqualTo(2);
		assertThat(stores.reports).containsOnlyKeys("1/2026-W45", "3/2026-W45");
	}
}
