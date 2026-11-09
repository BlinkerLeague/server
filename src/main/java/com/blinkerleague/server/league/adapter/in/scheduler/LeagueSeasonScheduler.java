package com.blinkerleague.server.league.adapter.in.scheduler;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.service.SettleSeasonService;

import lombok.RequiredArgsConstructor;

/** 시즌(달력 월, D29)이 끝나면 지난 시즌을 정산한다(D30). */
@Component
@RequiredArgsConstructor
public class LeagueSeasonScheduler {

	private final SettleSeasonService settleSeasonService;

	public void run() {
	}
}
