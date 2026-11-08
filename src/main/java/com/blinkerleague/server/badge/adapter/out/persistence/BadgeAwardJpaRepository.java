package com.blinkerleague.server.badge.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.badge.domain.BadgeAward;

interface BadgeAwardJpaRepository extends JpaRepository<BadgeAward, Long> {
}
