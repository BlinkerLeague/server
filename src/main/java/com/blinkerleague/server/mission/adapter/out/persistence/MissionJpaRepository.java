package com.blinkerleague.server.mission.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.MissionPeriod;

interface MissionJpaRepository extends JpaRepository<Mission, Long> {

	Optional<Mission> findFirstByPeriodAndActiveFromLessThanEqualAndActiveToGreaterThanEqualOrderByIdAsc(
		MissionPeriod period, LocalDate from, LocalDate to);
}
