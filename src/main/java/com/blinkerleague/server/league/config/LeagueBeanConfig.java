package com.blinkerleague.server.league.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.league.application.port.out.LeagueMemberStore;
import com.blinkerleague.server.league.application.port.out.LoadLeagueDriverPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;
import com.blinkerleague.server.league.application.service.JoinSeasonService;

/** league 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class LeagueBeanConfig {

	@Bean
	public JoinSeasonService joinSeasonService(LeagueMemberStore leagueMemberStore,
		LoadLeagueDriverPort loadLeagueDriverPort, LoadLeagueRegionsPort loadLeagueRegionsPort, Clock clock) {
		return new JoinSeasonService(leagueMemberStore, loadLeagueDriverPort, loadLeagueRegionsPort, clock.getZone());
	}
}
