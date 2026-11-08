package com.blinkerleague.server.mission.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.blinkerleague.server.mission.application.port.in.RecordMissionAfterTripUseCase;
import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.LoadDailyRoundsPort;

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
		throw new UnsupportedOperationException();
	}
}
