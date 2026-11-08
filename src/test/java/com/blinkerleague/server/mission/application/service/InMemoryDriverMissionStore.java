package com.blinkerleague.server.mission.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.domain.DriverMission;

/** {@link DriverMissionStore}의 메모리 구현. */
class InMemoryDriverMissionStore implements DriverMissionStore {

	final List<DriverMission> saved = new ArrayList<>();

	@Override
	public Optional<DriverMission> find(Long driverId, Long missionId, String periodKey) {
		return saved.stream()
			.filter(m -> m.getDriverId().equals(driverId) && java.util.Objects.equals(m.getMissionId(), missionId)
				&& m.getPeriodKey().equals(periodKey))
			.findFirst();
	}

	@Override
	public void save(DriverMission driverMission) {
		if (!saved.contains(driverMission)) {
			saved.add(driverMission);
		}
	}
}
