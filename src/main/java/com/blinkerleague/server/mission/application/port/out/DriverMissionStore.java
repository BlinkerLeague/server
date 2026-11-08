package com.blinkerleague.server.mission.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.mission.domain.DriverMission;

public interface DriverMissionStore {

	Optional<DriverMission> find(Long driverId, Long missionId, String periodKey);

	/** periodKey보다 앞선 날 중 가장 최근 기록(오늘 운행 전 화면에 보여 줄 마지막 미션 날). */
	Optional<DriverMission> findLatestBefore(Long driverId, String periodKey);

	/** 새 기록이 같은 운전자 · 미션 · 날짜와 겹치면(동시 업로드) {@link DriverMissionAlreadyExistsException}. */
	void save(DriverMission driverMission);

	class DriverMissionAlreadyExistsException extends RuntimeException {

		public DriverMissionAlreadyExistsException(String periodKey, Throwable cause) {
			super("이미 그날 미션 기록이 있습니다: " + periodKey, cause);
		}
	}
}
