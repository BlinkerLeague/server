package com.blinkerleague.server.league.adapter.out.catalog;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;
import com.blinkerleague.server.league.domain.LeagueRegions;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** {@code leagues/league-regions.json}의 매핑표. 리그를 나누거나 합칠 때는 이 파일만 바꾼다(D11). */
@Component
@RequiredArgsConstructor
public class LeagueRegionsCatalog implements LoadLeagueRegionsPort {

	private final ObjectMapper objectMapper;

	@Override
	public LeagueRegions load() {
		throw new UnsupportedOperationException();
	}
}
