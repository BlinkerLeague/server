package com.blinkerleague.server.mission.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.domain.DriverMission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverMissionPersistenceAdapter implements DriverMissionStore {

	private final DriverMissionJpaRepository repository;

	@Override
	public Optional<DriverMission> find(Long driverId, Long missionId, String periodKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void save(DriverMission driverMission) {
		throw new UnsupportedOperationException();
	}
}
