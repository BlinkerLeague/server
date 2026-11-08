package com.blinkerleague.server.mission.application.port.out;

import java.time.LocalDate;
import java.util.Set;

public interface LoadAchievedDaysPort {

	/** 운전자의 일일 미션을 달성(ACHIEVED)한 날들. */
	Set<LocalDate> achievedDays(Long driverId);
}
