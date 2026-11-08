package com.blinkerleague.server.badge.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.badge.application.port.out.BadgeAwardStore;
import com.blinkerleague.server.badge.application.port.out.LoadBadgeCatalogPort;
import com.blinkerleague.server.badge.application.port.out.SaveBadgeCatalogPort;
import com.blinkerleague.server.badge.domain.Badge;
import com.blinkerleague.server.badge.domain.BadgeAward;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BadgePersistenceAdapter implements LoadBadgeCatalogPort, SaveBadgeCatalogPort, BadgeAwardStore {

	private final BadgeJpaRepository badgeRepository;
	private final BadgeAwardJpaRepository awardRepository;

	@Override
	public List<Badge> findAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsAny() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveAll(List<Badge> badges) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BadgeAward> findByDriver(Long driverId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveIfAbsent(BadgeAward award) {
		throw new UnsupportedOperationException();
	}
}
