package com.blinkerleague.server.mission.application.port.out;

import java.time.LocalDate;
import java.util.Optional;

import com.blinkerleague.server.mission.domain.Mission;

public interface LoadActiveMissionPort {

	/** date에 운영 중인 일일 미션. */
	Optional<Mission> findActiveDaily(LocalDate date);
}
