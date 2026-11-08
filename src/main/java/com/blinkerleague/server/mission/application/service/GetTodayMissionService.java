package com.blinkerleague.server.mission.application.service;

import java.time.Clock;
import java.util.List;

import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.LoadDailyTurnsPort;
import com.blinkerleague.server.mission.domain.ConditionProgress;
import com.blinkerleague.server.mission.domain.DailyTurns;
import com.blinkerleague.server.mission.domain.MissionStatus;

import lombok.RequiredArgsConstructor;

/** 오늘의 미션 조회(plan.md 3.6, GET /api/missions/today). */
@RequiredArgsConstructor
public class GetTodayMissionService {

	private final LoadActiveMissionPort loadActiveMissionPort;
	private final LoadDailyTurnsPort loadDailyTurnsPort;
	private final Clock clock;

	public TodayMission today(Long driverId) {
		throw new UnsupportedOperationException();
	}

	public record TodayMission(Long missionId, String title, String periodKey, MissionStatus status,
		int rewardPoint, DailyTurns turns, int requiredTurns, List<ConditionProgress> conditions) {
	}
}
