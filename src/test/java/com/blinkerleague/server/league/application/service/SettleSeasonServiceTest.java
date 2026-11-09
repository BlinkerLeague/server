package com.blinkerleague.server.league.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.league.domain.LeagueRegion;
import com.blinkerleague.server.league.domain.LeagueRegions;
import com.blinkerleague.server.league.domain.LeagueSeason;
import com.blinkerleague.server.league.domain.MemberRecord;
import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

class SettleSeasonServiceTest {

	// 한국 시간 2026-12-01 00:10 → 지난 시즌은 2026-11
	private static final Clock DEC_1 = Clock.fixed(Instant.parse("2026-11-30T15:10:00Z"), TimeConfig.ZONE);

	@Test
	void 지난_시즌_안전지수_1위_권역의_순위_자격자에게_운전자_시즌마다_한_번_보상을_요청한다() {
		List<String> askedSeasons = new ArrayList<>();
		List<String> rewards = new ArrayList<>();
		List<MemberRecord> members = new ArrayList<>();
		for (long id = 1; id <= 10; id++) {
			members.add(new MemberRecord(id, "n" + id, "BUSAN", true, 10, 60, 60, 90 * 60, 0));
			members.add(new MemberRecord(100 + id, "m" + id, "DAEGU", true, 10, 60, 60, 80 * 60, 0));
		}
		// 1위 권역이지만 자격(50km · 10회)이 없는 운전자
		members.add(new MemberRecord(50L, "short", "BUSAN", true, 1, 5, 5, 90 * 5, 0));
		SettleSeasonService service = new SettleSeasonService(
			(LeagueSeason season) -> {
				askedSeasons.add(season.id());
				return members;
			},
			() -> new LeagueRegions(List.of(
				new LeagueRegions.Rule(new LeagueRegion("BUSAN", "부산"), List.of(), List.of("26")),
				new LeagueRegions.Rule(new LeagueRegion("DAEGU", "대구"), List.of(), List.of("27")))),
			() -> new VersionedThresholds(1, ThresholdsFixture.planExample()),
			(driverId, refId) -> {
				rewards.add(refId);
				return 100;
			},
			DEC_1);

		int rewarded = service.settleLastSeason();

		assertThat(askedSeasons).containsExactly("2026-11");
		assertThat(rewarded).isEqualTo(10);
		assertThat(rewards).hasSize(10).allMatch(refId -> refId.endsWith(":2026-11"))
			.contains("1:2026-11", "10:2026-11").doesNotContain("50:2026-11", "101:2026-11");
	}
}
