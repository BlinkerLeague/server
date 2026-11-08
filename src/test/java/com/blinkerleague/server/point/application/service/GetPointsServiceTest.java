package com.blinkerleague.server.point.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

class GetPointsServiceTest {

	// 한국 시간 2026-11-03 09:40
	private static final Instant NOW = Instant.parse("2026-11-03T00:40:00Z");

	private final InMemoryPointLedger ledger = new InMemoryPointLedger();
	private final GetPointsService service = new GetPointsService(ledger,
		() -> new VersionedThresholds(1, ThresholdsFixture.planExample()), Clock.fixed(NOW, TimeConfig.ZONE));

	@Test
	void 잔액은_전체_적립_합계이고_오늘_적립과_일일_상한을_함께_돌려준다() {
		ledger.saveNew(PointEntry.trip(1L, "yesterday", 100, 50.0, 100, Instant.parse("2026-11-02T14:59:59Z")));
		ledger.saveNew(PointEntry.trip(1L, "today-1", 10, 12.3, 86, Instant.parse("2026-11-02T15:00:00Z")));
		ledger.saveNew(PointEntry.trip(1L, "today-2", 20, 20.0, 100, NOW));
		ledger.saveNew(PointEntry.trip(2L, "other", 50, 50.0, 100, NOW));

		GetPointsService.Points points = service.get(1L);

		assertThat(points.balance()).isEqualTo(130);
		assertThat(points.todayEarned()).isEqualTo(30);
		assertThat(points.dailyCap()).isEqualTo(150);
	}

	@Test
	void 원장은_최근_적립부터_최대_20건을_돌려준다() {
		for (int i = 1; i <= 21; i++) {
			ledger.saveNew(PointEntry.trip(1L, "trip-" + i, i, 10.0, 100, NOW.minusSeconds(60L * (22 - i))));
		}
		ledger.saveNew(PointEntry.trip(2L, "other", 50, 50.0, 100, NOW));

		GetPointsService.Points points = service.get(1L);

		assertThat(points.entries()).hasSize(20)
			.extracting(PointEntry::getRefId)
			.startsWith("trip-21", "trip-20")
			.endsWith("trip-2")
			.doesNotContain("trip-1", "other");
	}
}
