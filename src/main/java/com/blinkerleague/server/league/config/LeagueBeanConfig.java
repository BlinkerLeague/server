package com.blinkerleague.server.league.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.league.application.port.out.LeagueMemberStore;
import com.blinkerleague.server.league.application.port.out.LoadLeagueDriverPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRecordsPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;
import com.blinkerleague.server.league.application.port.out.SeasonRewardPort;
import com.blinkerleague.server.league.application.service.GetCurrentLeagueService;
import com.blinkerleague.server.league.application.service.JoinSeasonService;
import com.blinkerleague.server.league.application.service.SettleSeasonService;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;

/** league 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class LeagueBeanConfig {

	@Bean
	public JoinSeasonService joinSeasonService(LeagueMemberStore leagueMemberStore,
		LoadLeagueDriverPort loadLeagueDriverPort, LoadLeagueRegionsPort loadLeagueRegionsPort, Clock clock) {
		return new JoinSeasonService(leagueMemberStore, loadLeagueDriverPort, loadLeagueRegionsPort, clock.getZone());
	}

	@Bean
	public GetCurrentLeagueService getCurrentLeagueService(LoadLeagueRecordsPort loadLeagueRecordsPort,
		LoadLeagueRegionsPort loadLeagueRegionsPort, LoadLeagueDriverPort loadLeagueDriverPort,
		ThresholdProvider thresholdProvider, Clock clock) {
		return new GetCurrentLeagueService(loadLeagueRecordsPort, loadLeagueRegionsPort, loadLeagueDriverPort,
			thresholdProvider, clock);
	}

	@Bean
	public SettleSeasonService settleSeasonService(LoadLeagueRecordsPort loadLeagueRecordsPort,
		LoadLeagueRegionsPort loadLeagueRegionsPort, ThresholdProvider thresholdProvider,
		SeasonRewardPort seasonRewardPort, Clock clock) {
		return new SettleSeasonService(loadLeagueRecordsPort, loadLeagueRegionsPort, thresholdProvider,
			seasonRewardPort, clock);
	}
}
