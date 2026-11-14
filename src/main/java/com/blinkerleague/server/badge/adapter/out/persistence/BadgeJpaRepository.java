package com.blinkerleague.server.badge.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.badge.domain.Badge;

interface BadgeJpaRepository extends JpaRepository<Badge, String> {
}
