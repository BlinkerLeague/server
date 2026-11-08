package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 하루 미션 판정: 라운드 하나라도 조건을 채우면 그날은 달성. */
class DayJudgementTest {

	private final TurnSignalCondition condition = new TurnSignalCondition(3, 100);

	@Test
	void 실패한_라운드가_있어도_다음_라운드에서_달성하면_ACHIEVED다() {
		DayJudgement day = condition.judgeDay(List.of(new RoundTurns(1, 0), new RoundTurns(3, 3)));

		assertThat(day.status()).isEqualTo(MissionStatus.ACHIEVED);
	}
}
