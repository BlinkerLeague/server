package com.blinkerleague.server.league.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.league.application.service.SettleSeasonService;
import com.blinkerleague.server.league.domain.LeagueRegions;
import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

class LeagueSeasonSchedulerTest {

	@Test
	void 매월_1일_0시_10분_한국_시간에_지난_시즌을_정산한다() throws Exception {
		Scheduled scheduled = LeagueSeasonScheduler.class.getMethod("run").getAnnotation(Scheduled.class);
		assertThat(scheduled).isNotNull();
		assertThat(scheduled.cron()).isEqualTo("0 10 0 1 * *");
		assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");

		List<String> settled = new ArrayList<>();
		SettleSeasonService service = new SettleSeasonService(season -> {
			settled.add(season.id());
			return List.of();
		}, () -> new LeagueRegions(List.of()), () -> new VersionedThresholds(1, ThresholdsFixture.planExample()),
			(driverId, refId) -> 0, Clock.fixed(Instant.parse("2026-11-30T15:10:00Z"), TimeConfig.ZONE));

		new LeagueSeasonScheduler(service).run();

		assertThat(settled).containsExactly("2026-11");
	}
}
