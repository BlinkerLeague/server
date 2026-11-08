package com.blinkerleague.server.mission.application.service;

import java.time.Clock;

import com.blinkerleague.server.mission.application.port.out.LoadAchievedDaysPort;

import lombok.RequiredArgsConstructor;

/** 미션 연속 달성 조회(plan.md 4.3, GET /api/streak). */
@RequiredArgsConstructor
public class GetStreakService {

	private final LoadAchievedDaysPort loadAchievedDaysPort;
	private final Clock clock;

	public StreakView get(Long driverId) {
		throw new UnsupportedOperationException();
	}

	public record StreakView(int streakDays, boolean todayAchieved) {
	}
}
