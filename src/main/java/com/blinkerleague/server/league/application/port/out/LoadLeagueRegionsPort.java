package com.blinkerleague.server.league.application.port.out;

import com.blinkerleague.server.league.domain.LeagueRegions;

/** 시군구 → 리그 권역 매핑표(D11 · D31). */
public interface LoadLeagueRegionsPort {

	LeagueRegions load();
}
