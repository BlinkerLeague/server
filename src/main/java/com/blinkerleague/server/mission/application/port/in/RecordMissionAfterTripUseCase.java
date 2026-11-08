package com.blinkerleague.server.mission.application.port.in;

import java.time.Instant;
import java.util.Optional;

import com.blinkerleague.server.mission.domain.MissionStatus;

/**
 * 운행을 올린 뒤 그 운행이 끝난 날(Asia/Seoul)의 일일 미션을 판정해 driver_mission에 남긴다(plan.md 3.6).
 * trip이 부르므로 in-port로 둔다. 그날 운영 중인 미션이 없으면 빈 값이다.
 */
public interface RecordMissionAfterTripUseCase {

	Optional<MissionOutcome> recordAfterTrip(Long driverId, Instant tripEndedAt);

	record MissionOutcome(Long missionId, MissionStatus status) {
	}
}
