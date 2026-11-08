package com.blinkerleague.server.mission.domain;

import java.util.List;

import com.blinkerleague.server.mission.domain.ConditionProgress.ConditionType;

/** 회전 깜빡이 미션 조건: 회전 requiredTurns회 이상, 깜빡이 사용률 requiredSignalRate% 이상. */
public record TurnSignalCondition(int requiredTurns, int requiredSignalRate) {

	public MissionJudgement judge(RoundTurns turns) {
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

	/** rounds는 종료 시각 순서다. */
	public DayJudgement judgeDay(List<RoundTurns> rounds) {
		for (RoundTurns round : rounds) {
			MissionJudgement judgement = judge(round);
			if (judgement.status() == MissionStatus.ACHIEVED) {
				return new DayJudgement(MissionStatus.ACHIEVED, round, judgement.conditions());
			}
		}
		// 달성 전에는 실패한 라운드가 있어도 그날은 끝나지 않는다. 진행 링은 가장 최근 라운드를 보여 준다.
		RoundTurns latest = rounds.get(rounds.size() - 1);
		return new DayJudgement(MissionStatus.IN_PROGRESS, latest, judge(latest).conditions());
	}

	// 내림해서 모든 회전이 성공하기 전에는 100%로 보이지 않게 한다. 회전이 없으면 0%.
	private static int signalRate(RoundTurns turns) {
		return turns.turns() == 0 ? 0 : turns.successTurns() * 100 / turns.turns();
	}
}
