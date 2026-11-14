package com.blinkerleague.server.badge.adapter.out.persistence;

import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
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
		return awardRepository.findByDriverIdOrderByAwardedAtAsc(driverId);
	}

	@Override
	public void saveIfAbsent(BadgeAward award) {
		if (awardRepository.existsByDriverIdAndBadgeCode(award.getDriverId(), award.getBadgeCode())) {
			return;
		}
		try {
			awardRepository.saveAndFlush(award);
		} catch (DataIntegrityViolationException e) {
			// 같은 배지를 동시에 판정해 먼저 저장됐다. 그 기록이 기준이다.
			if (!isUniqueViolation(e)) {
				throw e;
			}
		}
	}

	private static boolean isUniqueViolation(DataIntegrityViolationException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation.getKind() == ConstraintKind.UNIQUE;
			}
		}
		return false;
	}
}
