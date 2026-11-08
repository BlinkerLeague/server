package com.blinkerleague.server.mission.application.service;

import java.time.Clock;
import java.util.List;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.LoadDailyTurnsPort;
import com.blinkerleague.server.mission.domain.ConditionProgress;
import com.blinkerleague.server.mission.domain.DailyPeriod;
import com.blinkerleague.server.mission.domain.DayJudgement;
import com.blinkerleague.server.mission.domain.RoundTurns;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.MissionStatus;

import lombok.RequiredArgsConstructor;

/** 오늘의 미션 조회(plan.md 3.6, GET /api/missions/today). */
@RequiredArgsConstructor
public class GetTodayMissionService {

	private final LoadActiveMissionPort loadActiveMissionPort;
	private final LoadDailyTurnsPort loadDailyTurnsPort;
	private final Clock clock;

	public TodayMission today(Long driverId) {
		DailyPeriod period = DailyPeriod.of(clock.instant(), clock.getZone());
		Mission mission = loadActiveMissionPort.findActiveDaily(period.date())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "오늘 운영 중인 미션이 없습니다."));
		List<RoundTurns> rounds = loadDailyTurnsPort.loadRounds(driverId, period.start(), period.end());
		DayJudgement day = mission.getCondition().judgeDay(rounds);
		return new TodayMission(mission.getId(), mission.getTitle(), period.key(), day.status(),
			mission.getRewardPoint(), day.shownRound(), mission.getCondition().requiredTurns(), day.conditions());
	}

	public record TodayMission(Long missionId, String title, String periodKey, MissionStatus status,
		int rewardPoint, RoundTurns turns, int requiredTurns, List<ConditionProgress> conditions) {
	}
}
