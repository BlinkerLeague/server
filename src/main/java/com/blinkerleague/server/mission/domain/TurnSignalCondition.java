package com.blinkerleague.server.mission.domain;

import java.util.List;

/** 회전 깜빡이 미션 조건: 회전 requiredTurns회 이상, 깜빡이 사용률 requiredSignalRate% 이상. */
public record TurnSignalCondition(int requiredTurns, int requiredSignalRate) {

	public MissionJudgement judge(DailyTurns turns) {
		if (turns.successTurns() < turns.turns()) {
			return new MissionJudgement(MissionStatus.FAILED, List.of());
		}
		if (turns.turns() < requiredTurns) {
			return new MissionJudgement(MissionStatus.IN_PROGRESS, List.of());
		}
		return new MissionJudgement(MissionStatus.ACHIEVED, List.of());
	}
}
