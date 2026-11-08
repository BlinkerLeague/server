package com.blinkerleague.server.report.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface WeeklyReportJpaRepository extends JpaRepository<WeeklyReportEntity, Long> {

	Optional<WeeklyReportEntity> findByDriverIdAndWeekKey(Long driverId, String weekKey);
}
