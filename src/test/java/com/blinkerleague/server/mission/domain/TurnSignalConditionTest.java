package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TurnSignalConditionTest {

	// 시드 미션 "회전 깜빡이 완벽": 회전 3회 이상, 깜빡이 사용률 100%
	private final TurnSignalCondition condition = new TurnSignalCondition(3, 100);

	@Test
	void 회전_3회를_모두_깜빡이_켜고_하면_ACHIEVED다() {
		assertThat(condition.judge(new DailyTurns(3, 3)).status()).isEqualTo(MissionStatus.ACHIEVED);
	}

	@Test
	void 회전이_2회뿐이면_모두_성공해도_IN_PROGRESS다() {
		assertThat(condition.judge(new DailyTurns(2, 2)).status()).isEqualTo(MissionStatus.IN_PROGRESS);
	}

	@Test
	void 깜빡이를_안_켠_회전이_한_번이라도_있으면_FAILED다() {
		assertThat(condition.judge(new DailyTurns(1, 0)).status()).isEqualTo(MissionStatus.FAILED);
	}
}
