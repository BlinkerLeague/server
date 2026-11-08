package com.blinkerleague.server.badge.application.service;

import java.time.Clock;
import java.util.List;

import com.blinkerleague.server.badge.application.port.in.AwardBadgesUseCase;
import com.blinkerleague.server.badge.application.port.out.BadgeAwardStore;
import com.blinkerleague.server.badge.application.port.out.BadgeProgressPort;
import com.blinkerleague.server.badge.application.port.out.LoadBadgeCatalogPort;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AwardBadgesService implements AwardBadgesUseCase {

	private final LoadBadgeCatalogPort loadBadgeCatalogPort;
	private final BadgeProgressPort badgeProgressPort;
	private final BadgeAwardStore badgeAwardStore;
	private final Clock clock;

	@Override
	public List<String> awardEarned(Long driverId) {
		throw new UnsupportedOperationException();
	}
}
