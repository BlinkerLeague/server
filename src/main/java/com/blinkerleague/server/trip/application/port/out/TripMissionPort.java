package com.blinkerleague.server.trip.application.port.out;

import java.time.Instant;
import java.util.Optional;

/**
 * 운행을 반영한 그날 미션 판정(plan.md 3.5 TripResult.mission). 그날 운영 중인 미션이 없으면 빈 값이다.
 * status는 IN_PROGRESS · ACHIEVED 같은 미션 상태 이름이다(trip이 mission 도메인에 의존하지 않게 문자열로 받는다).
 */
public interface TripMissionPort {

	/** 운행을 올린 뒤 그날 미션을 판정해 기록하고 결과를 돌려준다. */
	Optional<TripMission> recordAfterUpload(Long driverId, Instant tripEndedAt);

	record TripMission(Long missionId, String status) {
	}
}
