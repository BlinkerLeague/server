package com.blinkerleague.server.league.application.port.out;

/** 시즌 리그 보상(D30) 적립. 같은 refId는 한 번만 적립된다. */
public interface SeasonRewardPort {

	/** 실제로 적립한 포인트를 돌려준다. */
	int rewardSeason(Long driverId, String refId);
}
