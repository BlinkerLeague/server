package com.blinkerleague.server.league.application.port.out;

/** 우승 칭호를 남긴 뒤 아직 받지 않은 배지(시즌 우승 배지)를 준다. */
public interface ChampionBadgePort {

	void awardBadges(Long driverId);
}
