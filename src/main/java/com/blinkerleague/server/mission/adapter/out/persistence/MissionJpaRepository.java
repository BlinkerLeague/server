package com.blinkerleague.server.mission.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.mission.domain.Mission;

interface MissionJpaRepository extends JpaRepository<Mission, Long> {
}
