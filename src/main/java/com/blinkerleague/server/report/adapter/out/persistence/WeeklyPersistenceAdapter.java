package com.blinkerleague.server.report.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.report.application.port.out.WeeklyMissionStore;
import com.blinkerleague.server.report.application.port.out.WeeklyReportStore;
import com.blinkerleague.server.report.domain.WeeklyMission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WeeklyPersistenceAdapter implements WeeklyMissionStore, WeeklyReportStore {

	private final WeeklyMissionJpaRepository missionRepository;

	@Override
	public Optional<WeeklyMission> find(Long driverId, String weekKey) {
		return missionRepository.findByDriverIdAndWeekKey(driverId, weekKey);
	}

	@Override
	public void save(WeeklyMission mission) {
		missionRepository.save(mission);
	}

	@Override
	public Optional<String> findReport(Long driverId, String weekKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveReport(Long driverId, String weekKey, String reportJson) {
		throw new UnsupportedOperationException();
	}
}
