package com.blinkerleague.server.badge.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Sort;
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
		return badgeRepository.findAll(Sort.by("sortOrder"));
	}

	@Override
	public boolean existsAny() {
		return badgeRepository.count() > 0;
	}

	@Override
	public void saveAll(List<Badge> badges) {
		badgeRepository.saveAll(badges);
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
