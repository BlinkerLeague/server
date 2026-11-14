package com.blinkerleague.server.league.adapter.out.badge;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.badge.application.port.in.AwardBadgesUseCase;
import com.blinkerleague.server.league.application.port.out.ChampionBadgePort;

import lombok.RequiredArgsConstructor;

/** league의 우승 배지 포트를 badge 유스케이스로 연결한다. */
@Component
@RequiredArgsConstructor
public class BadgeChampionAdapter implements ChampionBadgePort {

	private final AwardBadgesUseCase awardBadgesUseCase;

	@Override
	public void awardBadges(Long driverId) {
		awardBadgesUseCase.awardEarned(driverId);
	}
}
