package com.blinkerleague.server.mission.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.mission.domain.DriverMission;

public interface DriverMissionStore {

	Optional<DriverMission> find(Long driverId, Long missionId, String periodKey);

	/** 새 기록이 같은 운전자 · 미션 · 날짜와 겹치면(동시 업로드) {@link DriverMissionAlreadyExistsException}. */
	void save(DriverMission driverMission);

	class DriverMissionAlreadyExistsException extends RuntimeException {

		public DriverMissionAlreadyExistsException(String periodKey, Throwable cause) {
			super("이미 그날 미션 기록이 있습니다: " + periodKey, cause);
		}
	}
}
