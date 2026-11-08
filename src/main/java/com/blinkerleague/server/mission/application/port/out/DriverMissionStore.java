package com.blinkerleague.server.mission.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.mission.domain.DriverMission;

public interface DriverMissionStore {

	Optional<DriverMission> find(Long driverId, Long missionId, String periodKey);

	void save(DriverMission driverMission);
}
