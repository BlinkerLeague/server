package com.blinkerleague.server.trip.adapter.out.mission;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.in.RecordMissionAfterTripUseCase;
import com.blinkerleague.server.trip.application.port.out.TripMissionPort;

import lombok.RequiredArgsConstructor;

/** trip의 미션 포트를 mission 유스케이스로 연결한다. */
@Component
@RequiredArgsConstructor
public class MissionTripAdapter implements TripMissionPort {

	private final RecordMissionAfterTripUseCase recordMissionAfterTripUseCase;

	@Override
	public Optional<TripMission> recordAfterUpload(Long driverId, Instant tripEndedAt) {
		return recordMissionAfterTripUseCase.recordAfterTrip(driverId, tripEndedAt)
			.map(outcome -> new TripMission(outcome.missionId(), outcome.status().name()));
	}
}
