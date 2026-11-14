package com.blinkerleague.server.report.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.report.domain.WeeklyMission;

public interface WeeklyMissionStore {

	Optional<WeeklyMission> find(Long driverId, String weekKey);

	void save(WeeklyMission mission);
}
