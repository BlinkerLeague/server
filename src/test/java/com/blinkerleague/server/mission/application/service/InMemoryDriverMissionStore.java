package com.blinkerleague.server.mission.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.application.port.out.DriverMissionStore.DriverMissionAlreadyExistsException;
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
	public Optional<DriverMission> findLatestBefore(Long driverId, String periodKey) {
		return saved.stream()
			.filter(m -> m.getDriverId().equals(driverId) && m.getPeriodKey().compareTo(periodKey) < 0)
			.max(java.util.Comparator.comparing(DriverMission::getPeriodKey));
	}

	private DriverMission savedConcurrently;

	/** 다음 새 기록을 저장하기 직전에 다른 요청이 같은 날 기록을 먼저 저장한 상황을 흉내 낸다. */
	void saveConcurrentlyBeforeNextInsert(DriverMission other) {
		this.savedConcurrently = other;
	}

	@Override
	public void save(DriverMission driverMission) {
		if (savedConcurrently != null && !saved.contains(driverMission)) {
			saved.add(savedConcurrently);
			savedConcurrently = null;
			throw new DriverMissionAlreadyExistsException(driverMission.getPeriodKey(), null);
		}
		if (!saved.contains(driverMission)) {
			saved.add(driverMission);
		}
	}
}
