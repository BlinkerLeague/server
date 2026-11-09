package com.blinkerleague.server.league.adapter.out.point;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.out.SeasonRewardPort;
import com.blinkerleague.server.point.application.port.in.EarnPointUseCase;

import lombok.RequiredArgsConstructor;

/** league의 시즌 보상 포트를 point 유스케이스로 연결한다. */
@Component
@RequiredArgsConstructor
public class PointSeasonRewardAdapter implements SeasonRewardPort {

	private final EarnPointUseCase earnPointUseCase;

	@Override
	public int rewardSeason(Long driverId, String refId) {
		return earnPointUseCase.earnForSeasonLeague(driverId, refId);
	}
}
