package com.blinkerleague.server.league.application.service;

import java.time.Instant;
import java.time.ZoneId;

import com.blinkerleague.server.league.application.port.in.JoinSeasonUseCase;
import com.blinkerleague.server.league.application.port.out.LeagueMemberStore;
import com.blinkerleague.server.league.application.port.out.LoadLeagueDriverPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;

import lombok.RequiredArgsConstructor;

/** 시즌 첫 참여 운행 때 등록 지역으로 리그 소속을 정한다(D12). */
@RequiredArgsConstructor
public class JoinSeasonService implements JoinSeasonUseCase {

	private final LeagueMemberStore leagueMemberStore;
	private final LoadLeagueDriverPort loadLeagueDriverPort;
	private final LoadLeagueRegionsPort loadLeagueRegionsPort;
	private final ZoneId zone;

	@Override
	public void joinSeasonOf(Long driverId, Instant tripEndedAt) {
		throw new UnsupportedOperationException();
	}
}
