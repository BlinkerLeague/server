package com.blinkerleague.server.league.application.service;

import java.time.Clock;
import java.util.List;

import com.blinkerleague.server.league.application.port.out.LoadLeagueRecordsPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;
import com.blinkerleague.server.league.application.port.out.ChampionBadgePort;
import com.blinkerleague.server.league.application.port.out.SeasonChampionStore;
import com.blinkerleague.server.league.domain.LeagueBoard;
import com.blinkerleague.server.league.domain.LeagueSeason;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.Thresholds;

import lombok.RequiredArgsConstructor;

/**
 * 끝난 시즌 정산(plan.md 4.4, D30). 지난 시즌의 안전지수 1위 권역(순위 공개 권역 중)의 순위 자격자에게
 * 시즌 보상을 적립한다. 보상은 운전자 · 시즌마다 한 번이라 다시 돌려도 안전하다.
 */
@RequiredArgsConstructor
public class SettleSeasonService {

	private final LoadLeagueRecordsPort loadLeagueRecordsPort;
	private final LoadLeagueRegionsPort loadLeagueRegionsPort;
	private final ThresholdProvider thresholdProvider;
	private final SeasonChampionStore seasonChampionStore;
	private final ChampionBadgePort championBadgePort;
	private final Clock clock;

	/** 지난 시즌을 정산하고 보상받은 운전자 수를 돌려준다. */
	public int settleLastSeason() {
		LeagueSeason season = LeagueSeason.containing(clock.instant(), clock.getZone()).previous();
		Thresholds.League rule = thresholdProvider.current().thresholds().league();
		LeagueBoard board = LeagueBoard.build(loadLeagueRecordsPort.records(season), loadLeagueRegionsPort.load().all(),
			new LeagueBoard.Rules(rule.minParticipants(), rule.minSeasonDistanceKm(), rule.minSeasonTrips()));
		List<Long> rewardees = board.seasonRewardees();
		return rewardees.size();
	}
}
