package com.blinkerleague.server.league.application.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import com.blinkerleague.server.league.application.port.out.LoadLeagueDriverPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueDriverPort.LeagueDriver;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRecordsPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;
import com.blinkerleague.server.league.application.port.out.SeasonChampionStore;
import com.blinkerleague.server.league.domain.LeagueBoard;
import com.blinkerleague.server.league.domain.LeagueBoard.RegionBoard;
import com.blinkerleague.server.league.domain.LeagueBoard.Standing;
import com.blinkerleague.server.league.domain.LeagueRegion;
import com.blinkerleague.server.league.domain.LeagueRegions;
import com.blinkerleague.server.league.domain.LeagueSeason;
import com.blinkerleague.server.league.domain.Ranked;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.Thresholds;

import lombok.RequiredArgsConstructor;

/** 지금 시즌의 내 리그 현황(plan.md 4.4, SCR-04). 순위는 조회할 때 RDB 기록으로 계산한다. */
@RequiredArgsConstructor
public class GetCurrentLeagueService {

	private final LoadLeagueRecordsPort loadLeagueRecordsPort;
	private final LoadLeagueRegionsPort loadLeagueRegionsPort;
	private final LoadLeagueDriverPort loadLeagueDriverPort;
	private final SeasonChampionStore seasonChampionStore;
	private final ThresholdProvider thresholdProvider;
	private final Clock clock;

	public CurrentLeague current(Long driverId) {
		LeagueSeason season = LeagueSeason.containing(clock.instant(), clock.getZone());
		Thresholds.League rule = thresholdProvider.current().thresholds().league();
		LeagueRegions regions = loadLeagueRegionsPort.load();
		LeagueBoard board = LeagueBoard.build(loadLeagueRecordsPort.records(season), regions.all(),
			new LeagueBoard.Rules(rule.minParticipants(), rule.minSeasonDistanceKm(), rule.minSeasonTrips()));
		Optional<LeagueDriver> driver = loadLeagueDriverPort.find(driverId);
		Optional<Standing> standing = board.standingOf(driverId);
		// 이번 시즌 소속이 있으면 그 권역, 아직 운행 전이면 지금 등록 지역의 권역을 보여 준다.
		Optional<String> leagueCode = standing.map(Standing::leagueCode)
			.or(() -> driver.flatMap(d -> regions.regionOf(d.regionCode())).map(LeagueRegion::code));
		RegionBoard league = leagueCode.map(board::region).map(b -> top(b, rule.topN())).orElse(null);
		Me me = new Me(driver.map(LeagueDriver::visible).orElse(true),
			standing.map(Standing::eligible).orElse(false),
			standing.map(Standing::averageScore).orElse(null),
			standing.map(Standing::points).orElse(0),
			standing.map(Standing::score).orElse(null),
			standing.map(Standing::rank).orElse(null),
			standing.map(Standing::trips).orElse(0),
			standing.map(Standing::distanceKm).orElse(0.0),
			championships(driverId, regions));
		return new CurrentLeague(season, league, me, board.regions());
	}

	// 지난 시즌 우승 칭호. 권역 이름은 지금 매핑표에서 찾고, 매핑표에서 빠진 권역이면 코드를 이름으로 쓴다.
	private List<Championship> championships(Long driverId, LeagueRegions regions) {
		return seasonChampionStore.findByDriver(driverId).stream()
			.map(champion -> new Championship(champion.getSeasonId(), champion.getLeagueCode(),
				regions.all().stream()
					.filter(region -> region.code().equals(champion.getLeagueCode()))
					.map(LeagueRegion::name)
					.findFirst()
					.orElse(champion.getLeagueCode())))
			.toList();
	}

	private static RegionBoard top(RegionBoard board, int topN) {
		List<Ranked> rankings = board.rankings().stream().limit(topN).toList();
		return new RegionBoard(board.region(), board.participants(), board.published(), board.safetyIndex(),
			board.rank(), rankings);
	}

	/** league는 매핑표에 없는 지역의 운전자면 null이다. */
	public record CurrentLeague(LeagueSeason season, RegionBoard league, Me me, List<RegionBoard> regions) {
	}

	/** 내 시즌 기록. score = averageScore + points(D34). rank는 순위가 공개되고 자격이 있고 순위 공개를 켰을 때만 있다. */
	public record Me(boolean visible, boolean eligible, Double averageScore, int points, Double score, Integer rank,
		int trips, double distanceKm, List<Championship> championships) {
	}

	/** 시즌 우승 칭호(D30 · D34). */
	public record Championship(String seasonId, String leagueCode, String name) {
	}
}
