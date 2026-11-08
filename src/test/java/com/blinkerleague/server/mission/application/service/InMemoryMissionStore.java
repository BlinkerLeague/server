package com.blinkerleague.server.mission.application.service;

import java.util.ArrayList;
import java.util.List;

import com.blinkerleague.server.mission.application.port.out.SaveMissionPort;
import com.blinkerleague.server.mission.domain.Mission;

/** {@link SaveMissionPort}의 메모리 구현. */
class InMemoryMissionStore implements SaveMissionPort {

	final List<Mission> saved = new ArrayList<>();

	@Override
	public boolean existsAny() {
		return !saved.isEmpty();
	}

	@Override
	public void saveAll(List<Mission> missions) {
		saved.addAll(missions);
	}
}
