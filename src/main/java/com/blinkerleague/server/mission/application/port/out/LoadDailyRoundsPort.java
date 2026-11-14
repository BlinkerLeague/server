package com.blinkerleague.server.mission.application.port.out;

import java.time.Instant;
import java.util.List;

import com.blinkerleague.server.mission.domain.RoundTurns;

public interface LoadDailyRoundsPort {

	/** 종료 시각이 [from, to)인 운전자의 점수 대상 운행(EXCLUDED_SHORT 제외)마다 회전 결과. 종료 시각 순서다. */
	List<RoundTurns> load(Long driverId, Instant from, Instant to);
}
