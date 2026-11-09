package com.blinkerleague.server.league.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.league.application.port.out.LeagueMemberStore;
import com.blinkerleague.server.league.domain.LeagueMember;
import com.blinkerleague.server.league.domain.LeagueRegion;
import com.blinkerleague.server.league.domain.LeagueRegions;

class JoinSeasonServiceTest {

	private static final Instant NOV_3 = Instant.parse("2026-11-03T00:00:00Z");
	private static final Instant NOV_20 = Instant.parse("2026-11-20T00:00:00Z");
	private static final Instant DEC_2 = Instant.parse("2026-12-02T00:00:00Z");

	private final Map<String, LeagueMember> members = new HashMap<>();
	private final Map<Long, String> regionCodes = new HashMap<>();
	private final JoinSeasonService service = new JoinSeasonService(new InMemoryMembers(),
		driverId -> Optional.ofNullable(regionCodes.get(driverId)),
		() -> new LeagueRegions(List.of(
			new LeagueRegions.Rule(new LeagueRegion("SEOUL_NORTHWEST", "서울 서북권"), List.of("11440"), List.of()),
			new LeagueRegions.Rule(new LeagueRegion("BUSAN", "부산"), List.of(), List.of("26")))),
		TimeConfig.ZONE);

	@Test
	void 시즌_첫_참여_운행_때의_등록_지역으로_소속을_정하고_시즌_중_지역을_바꿔도_다음_시즌부터_반영한다() {
		regionCodes.put(1L, "11440");
		service.joinSeasonOf(1L, NOV_3);

		regionCodes.put(1L, "26440");
		service.joinSeasonOf(1L, NOV_20);
		service.joinSeasonOf(1L, DEC_2);

		assertThat(members.get("2026-11:1").getLeagueCode()).isEqualTo("SEOUL_NORTHWEST");
		assertThat(members.get("2026-12:1").getLeagueCode()).isEqualTo("BUSAN");
	}

	private class InMemoryMembers implements LeagueMemberStore {

		@Override
		public Optional<LeagueMember> find(String seasonId, Long driverId) {
			return Optional.ofNullable(members.get(seasonId + ":" + driverId));
		}

		@Override
		public void saveIfAbsent(LeagueMember member) {
			members.putIfAbsent(member.getSeasonId() + ":" + member.getDriverId(), member);
		}
	}
}
