package com.blinkerleague.server.league.application.service;

import java.time.Clock;

import com.blinkerleague.server.league.application.port.out.LoadLeagueRecordsPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;
import com.blinkerleague.server.league.application.port.out.SeasonRewardPort;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;

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
	private final SeasonRewardPort seasonRewardPort;
	private final Clock clock;

	/** 지난 시즌을 정산하고 보상받은 운전자 수를 돌려준다. */
	public int settleLastSeason() {
		throw new UnsupportedOperationException();
	}
}
