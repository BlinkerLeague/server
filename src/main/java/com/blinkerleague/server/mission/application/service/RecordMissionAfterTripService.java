package com.blinkerleague.server.mission.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.blinkerleague.server.mission.application.port.in.RecordMissionAfterTripUseCase;
import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.application.port.out.DriverMissionStore.DriverMissionAlreadyExistsException;
import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.LoadDailyRoundsPort;

import com.blinkerleague.server.mission.domain.DailyPeriod;
import com.blinkerleague.server.mission.domain.DriverMission;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.MissionStatus;
import com.blinkerleague.server.mission.domain.RoundTurns;

import lombok.RequiredArgsConstructor;

/** 운행 업로드 후 그날 미션 판정을 driver_mission에 남긴다(plan.md 3.6). */
@RequiredArgsConstructor
public class RecordMissionAfterTripService implements RecordMissionAfterTripUseCase {

	private final LoadActiveMissionPort loadActiveMissionPort;
	private final LoadDailyRoundsPort loadDailyRoundsPort;
	private final DriverMissionStore driverMissionStore;
	private final Clock clock;

	@Override
	public Optional<MissionOutcome> recordAfterTrip(Long driverId, Instant tripEndedAt) {
		// 운행은 종료 시각이 속한 날에 집계한다. 늦게 올린 어제 운행은 어제 미션에 반영한다.
		DailyPeriod period = DailyPeriod.of(tripEndedAt, clock.getZone());
		Optional<Mission> active = loadActiveMissionPort.findActiveDaily(period.date());
		if (active.isEmpty()) {
			return Optional.empty();
		}
		Mission mission = active.get();
		List<RoundTurns> rounds = loadDailyRoundsPort.load(driverId, period.start(), period.end());
		MissionStatus judged = mission.getCondition().judgeDay(rounds).status();
		DriverMission record;
		try {
			record = applyAndSave(driverId, mission.getId(), period.key(), judged);
		} catch (DriverMissionAlreadyExistsException e) {
			// 같은 날 다른 운행의 판정이 먼저 기록됐다. 그 기록을 다시 읽어 반영한다.
			record = applyAndSave(driverId, mission.getId(), period.key(), judged);
		}
		return Optional.of(new MissionOutcome(mission.getId(), record.getStatus()));
	}

	private DriverMission applyAndSave(Long driverId, Long missionId, String periodKey, MissionStatus judged) {
		DriverMission record = driverMissionStore.find(driverId, missionId, periodKey)
			.orElseGet(() -> DriverMission.start(driverId, missionId, periodKey));
		record.apply(judged, clock.instant());
		driverMissionStore.save(record);
		return record;
	}
}
