package com.blinkerleague.server.report.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.blinkerleague.server.report.application.port.out.WeeklyMissionStore;
import com.blinkerleague.server.report.application.port.out.WeeklyReportStore;
import com.blinkerleague.server.report.domain.WeeklyMission;

/** 주간 미션 · 리포트 저장소의 메모리 구현. */
class InMemoryWeeklyStores implements WeeklyMissionStore, WeeklyReportStore {

	final List<WeeklyMission> missions = new ArrayList<>();
	final Map<String, String> reports = new HashMap<>();

	@Override
	public Optional<WeeklyMission> find(Long driverId, String weekKey) {
		return missions.stream().filter(m -> m.getDriverId().equals(driverId) && m.getWeekKey().equals(weekKey))
			.findFirst();
	}

	@Override
	public void save(WeeklyMission mission) {
		if (!missions.contains(mission)) {
			missions.add(mission);
		}
	}

	@Override
	public Optional<String> findReport(Long driverId, String weekKey) {
		return Optional.ofNullable(reports.get(driverId + "/" + weekKey));
	}

	@Override
	public void saveReport(Long driverId, String weekKey, String reportJson) {
		reports.put(driverId + "/" + weekKey, reportJson);
	}
}
