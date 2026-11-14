package com.blinkerleague.server.mission.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import com.blinkerleague.server.mission.application.port.out.LoadAchievedDaysPort;
import com.blinkerleague.server.mission.domain.Streak;

import lombok.RequiredArgsConstructor;

/** 미션 연속 달성 조회(plan.md 4.3, GET /api/streak). */
@RequiredArgsConstructor
public class GetStreakService {

	private final LoadAchievedDaysPort loadAchievedDaysPort;
	private final Clock clock;

	public StreakView get(Long driverId) {
		LocalDate today = LocalDate.ofInstant(clock.instant(), clock.getZone());
		Set<LocalDate> achieved = loadAchievedDaysPort.achievedDays(driverId);
		return new StreakView(Streak.current(achieved, today), achieved.contains(today));
	}

	public record StreakView(int streakDays, boolean todayAchieved) {
	}
}
