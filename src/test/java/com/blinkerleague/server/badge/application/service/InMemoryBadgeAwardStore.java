package com.blinkerleague.server.badge.application.service;

import java.util.ArrayList;
import java.util.List;

import com.blinkerleague.server.badge.application.port.out.BadgeAwardStore;
import com.blinkerleague.server.badge.domain.BadgeAward;

/** {@link BadgeAwardStore}의 메모리 구현. */
class InMemoryBadgeAwardStore implements BadgeAwardStore {

	final List<BadgeAward> saved = new ArrayList<>();

	@Override
	public List<BadgeAward> findByDriver(Long driverId) {
		return saved.stream().filter(a -> a.getDriverId().equals(driverId)).toList();
	}

	@Override
	public void saveIfAbsent(BadgeAward award) {
		boolean exists = saved.stream()
			.anyMatch(a -> a.getDriverId().equals(award.getDriverId()) && a.getBadgeCode().equals(award.getBadgeCode()));
		if (!exists) {
			saved.add(award);
		}
	}
}
