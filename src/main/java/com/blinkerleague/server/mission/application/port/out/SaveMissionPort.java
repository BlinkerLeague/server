package com.blinkerleague.server.mission.application.port.out;

import java.util.List;

import com.blinkerleague.server.mission.domain.Mission;

public interface SaveMissionPort {

	boolean existsAny();

	void saveAll(List<Mission> missions);
}
