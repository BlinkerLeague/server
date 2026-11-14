package com.blinkerleague.server.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.report.domain.WeekDriving;

class GetWeeklyReportServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	// 한국 시간 2026-11-09(월) 09:00 → 이번 주 W46, 지난주 W45
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T00:00:00Z"), SEOUL);

	private final GetWeeklyReportService service = new GetWeeklyReportService(new GenerateWeeklyReportService(
		(driverId, from, to) -> new WeekDriving(0, 0, List.of()), new InMemoryWeeklyStores(),
		new InMemoryWeeklyStores(), metric -> Optional.empty(), SEOUL), clock);

	@Test
	void 주를_정하지_않으면_지난주_리포트를_돌려준다() {
		assertThat(service.get(1L, null).week()).isEqualTo("2026-W45");
		assertThat(service.get(1L, "2026-W44").week()).isEqualTo("2026-W44");
	}

	@Test
	void 끝나지_않은_주와_형식이_틀린_주는_INVALID_INPUT이다() {
		for (String week : new String[] {"2026-W46", "2026-W47", "2026-45", "2026-W60"}) {
			assertThatThrownBy(() -> service.get(1L, week))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);
		}
	}
}
