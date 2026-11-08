package com.blinkerleague.server.badge.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.badge.domain.BadgeAward;

interface BadgeAwardJpaRepository extends JpaRepository<BadgeAward, Long> {

	List<BadgeAward> findByDriverIdOrderByAwardedAtAsc(Long driverId);

	boolean existsByDriverIdAndBadgeCode(Long driverId, String badgeCode);
}
