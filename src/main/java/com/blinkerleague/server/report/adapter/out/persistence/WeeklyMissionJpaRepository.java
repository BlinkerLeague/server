package com.blinkerleague.server.report.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.report.domain.WeeklyMission;

interface WeeklyMissionJpaRepository extends JpaRepository<WeeklyMission, Long> {
}
