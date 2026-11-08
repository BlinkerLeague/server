package com.blinkerleague.server.badge.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.blinkerleague.server.badge.application.port.in.AwardBadgesUseCase;
import com.blinkerleague.server.badge.application.port.out.BadgeAwardStore;
import com.blinkerleague.server.badge.application.port.out.BadgeProgressPort;
import com.blinkerleague.server.badge.application.port.out.LoadBadgeCatalogPort;
import com.blinkerleague.server.badge.domain.Badge;
import com.blinkerleague.server.badge.domain.BadgeAward;
import com.blinkerleague.server.badge.domain.BadgeProgress;

import lombok.RequiredArgsConstructor;

/** 배지 지급(plan.md 4.3). 기록이 조건에 닿았고 아직 받지 않은 배지를 준다. */
@RequiredArgsConstructor
public class AwardBadgesService implements AwardBadgesUseCase {

	private final LoadBadgeCatalogPort loadBadgeCatalogPort;
	private final BadgeProgressPort badgeProgressPort;
	private final BadgeAwardStore badgeAwardStore;
	private final Clock clock;

	@Override
	public List<String> awardEarned(Long driverId) {
		Set<String> owned = badgeAwardStore.findByDriver(driverId).stream()
			.map(BadgeAward::getBadgeCode)
			.collect(Collectors.toSet());
		BadgeProgress progress = badgeProgressPort.progress(driverId);
		Instant now = clock.instant();
		List<String> awarded = new ArrayList<>();
		for (Badge badge : loadBadgeCatalogPort.findAll()) {
			if (!owned.contains(badge.getCode()) && badge.isEarnedBy(progress)) {
				badgeAwardStore.saveIfAbsent(new BadgeAward(driverId, badge.getCode(), now));
				awarded.add(badge.getCode());
			}
		}
		return awarded;
	}
}
