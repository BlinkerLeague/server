package com.blinkerleague.server.point.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;
import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

class EarnPointServiceTest {

	// 한국 시간 2026-11-03 09:40
	private static final Instant NOW = Instant.parse("2026-11-03T00:40:00Z");

	private final InMemoryPointLedger ledger = new InMemoryPointLedger();
	private final EarnPointService service = new EarnPointService(ledger, ledger,
		() -> new VersionedThresholds(1, ThresholdsFixture.planExample()), ledger,
		Clock.fixed(NOW, TimeConfig.ZONE));

	@Test
	void 운행_점수로_적립하면_적립_시점의_거리와_점수를_원장에_남긴다() {
		int earned = service.earnForTrip(1L, "trip-1", 12.3, 86);

		assertThat(earned).isEqualTo(10);
		assertThat(ledger.entries()).singleElement().satisfies(entry -> {
			assertThat(entry.getDriverId()).isEqualTo(1L);
			assertThat(entry.getAmount()).isEqualTo(10);
			assertThat(entry.getReason()).isEqualTo(PointReason.TRIP);
			assertThat(entry.getRefId()).isEqualTo("trip-1");
			assertThat(entry.getDistanceKm()).isEqualTo(12.3);
			assertThat(entry.getScore()).isEqualTo(86);
			assertThat(entry.getEarnedAt()).isEqualTo(NOW);
		});
	}

	@Test
	void 오늘_적립이_일일_상한에_가까우면_남은_만큼만_적립한다() {
		ledger.saveNew(PointEntry.trip(1L, "trip-0", 145, 50.0, 100, NOW.minusSeconds(3600)));

		int earned = service.earnForTrip(1L, "trip-1", 12.3, 86);

		assertThat(earned).isEqualTo(5);
		assertThat(ledger.entries()).filteredOn(entry -> entry.getRefId().equals("trip-1"))
			.singleElement().satisfies(entry -> assertThat(entry.getAmount()).isEqualTo(5));
	}

	@Test
	void 일일_상한은_한국_시간_자정을_기준으로_오늘_적립만_센다() {
		// 한국 시간 어제 23:59:59(상한까지 적립)와 오늘 00:00(145P), 다른 운전자의 오늘 적립
		ledger.saveNew(PointEntry.trip(1L, "yesterday", 150, 50.0, 100, Instant.parse("2026-11-02T14:59:59Z")));
		ledger.saveNew(PointEntry.trip(1L, "midnight", 145, 50.0, 100, Instant.parse("2026-11-02T15:00:00Z")));
		ledger.saveNew(PointEntry.trip(2L, "other", 150, 50.0, 100, NOW.minusSeconds(60)));

		int earned = service.earnForTrip(1L, "trip-1", 12.3, 86);

		assertThat(earned).isEqualTo(5);
	}

	@Test
	void 같은_운행으로_다시_적립하면_원장에_한_번만_남기고_0을_돌려준다() {
		service.earnForTrip(1L, "trip-1", 12.3, 86);

		int retried = service.earnForTrip(1L, "trip-1", 12.3, 86);

		assertThat(retried).isZero();
		assertThat(ledger.entries()).singleElement()
			.satisfies(entry -> assertThat(entry.getAmount()).isEqualTo(10));
	}

	@Test
	void 확인과_저장_사이에_같은_운행이_먼저_적립되면_오류_없이_0을_돌려준다() {
		ledger.conflictOnNextSave();

		int earned = service.earnForTrip(1L, "trip-1", 12.3, 86);

		assertThat(earned).isZero();
	}
}
