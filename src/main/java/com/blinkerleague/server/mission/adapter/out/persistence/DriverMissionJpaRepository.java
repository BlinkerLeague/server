package com.blinkerleague.server.mission.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.mission.domain.DriverMission;
import com.blinkerleague.server.mission.domain.MissionStatus;

interface DriverMissionJpaRepository extends JpaRepository<DriverMission, Long> {

	Optional<DriverMission> findByDriverIdAndMissionIdAndPeriodKey(Long driverId, Long missionId, String periodKey);

	// period_key는 yyyy-MM-dd라 문자열 순서가 날짜 순서와 같다.
	Optional<DriverMission> findFirstByDriverIdAndPeriodKeyLessThanOrderByPeriodKeyDesc(Long driverId,
		String periodKey);

	List<DriverMission> findByDriverIdAndStatus(Long driverId, MissionStatus status);
}
