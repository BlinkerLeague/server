package com.blinkerleague.server.mission.domain;

import java.util.List;

import com.blinkerleague.server.mission.domain.ConditionProgress.ConditionType;

/** 회전 깜빡이 미션 조건: 회전 requiredTurns회 이상, 깜빡이 사용률 requiredSignalRate% 이상. */
public record TurnSignalCondition(int requiredTurns, int requiredSignalRate) {

	public MissionJudgement judge(DailyTurns turns) {
		List<ConditionProgress> conditions = List.of(
			new ConditionProgress(ConditionType.TURN_COUNT, turns.turns(), requiredTurns),
			new ConditionProgress(ConditionType.SIGNAL_RATE, signalRate(turns), requiredSignalRate));
		if (turns.successTurns() < turns.turns()) {
			return new MissionJudgement(MissionStatus.FAILED, conditions);
		}
		if (turns.turns() < requiredTurns) {
			return new MissionJudgement(MissionStatus.IN_PROGRESS, conditions);
		}
		return new MissionJudgement(MissionStatus.ACHIEVED, conditions);
	}

	// 내림해서 모든 회전이 성공하기 전에는 100%로 보이지 않게 한다.
	private static int signalRate(DailyTurns turns) {
		return turns.successTurns() * 100 / turns.turns();
	}
}
