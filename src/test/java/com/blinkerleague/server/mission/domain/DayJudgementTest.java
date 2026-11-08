package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.mission.domain.ConditionProgress.ConditionType;

/** 하루 미션 판정: 라운드 하나라도 조건을 채우면 그날은 달성. */
class DayJudgementTest {

	private final TurnSignalCondition condition = new TurnSignalCondition(3, 100);

	@Test
	void 실패한_라운드가_있어도_다음_라운드에서_달성하면_ACHIEVED다() {
		DayJudgement day = condition.judgeDay(List.of(new RoundTurns(1, 0), new RoundTurns(3, 3)));

		assertThat(day.status()).isEqualTo(MissionStatus.ACHIEVED);
	}

	@Test
	void 달성한_뒤_실패한_라운드가_와도_ACHIEVED이고_진행도는_달성한_라운드다() {
		DayJudgement day = condition.judgeDay(List.of(new RoundTurns(3, 3), new RoundTurns(2, 1)));

		assertThat(day.status()).isEqualTo(MissionStatus.ACHIEVED);
		assertThat(day.shownRound()).isEqualTo(new RoundTurns(3, 3));
		assertThat(day.conditions()).containsExactly(
			new ConditionProgress(ConditionType.TURN_COUNT, 3, 3),
			new ConditionProgress(ConditionType.SIGNAL_RATE, 100, 100));
	}

	@Test
	void 달성한_라운드가_없으면_IN_PROGRESS이고_진행도는_가장_최근_라운드다() {
		DayJudgement day = condition.judgeDay(List.of(new RoundTurns(2, 2), new RoundTurns(4, 3)));

		assertThat(day.status()).isEqualTo(MissionStatus.IN_PROGRESS);
		assertThat(day.shownRound()).isEqualTo(new RoundTurns(4, 3));
		assertThat(day.conditions()).containsExactly(
			new ConditionProgress(ConditionType.TURN_COUNT, 4, 3),
			new ConditionProgress(ConditionType.SIGNAL_RATE, 75, 100));
	}
}
