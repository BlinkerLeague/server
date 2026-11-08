package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.mission.domain.ConditionProgress.ConditionType;

class TurnSignalConditionTest {

	// 시드 미션 "회전 깜빡이 완벽": 회전 3회 이상, 깜빡이 사용률 100%
	private final TurnSignalCondition condition = new TurnSignalCondition(3, 100);

	@Test
	void 회전_3회를_모두_깜빡이_켜고_하면_ACHIEVED다() {
		assertThat(condition.judge(new RoundTurns(3, 3)).status()).isEqualTo(MissionStatus.ACHIEVED);
	}

	@Test
	void 회전이_2회뿐이면_모두_성공해도_IN_PROGRESS다() {
		assertThat(condition.judge(new RoundTurns(2, 2)).status()).isEqualTo(MissionStatus.IN_PROGRESS);
	}

	@Test
	void 깜빡이를_안_켠_회전이_한_번이라도_있으면_FAILED다() {
		assertThat(condition.judge(new RoundTurns(1, 0)).status()).isEqualTo(MissionStatus.FAILED);
	}

	@Test
	void 조건별_진행도로_회전_수와_깜빡이_사용률을_돌려준다() {
		assertThat(condition.judge(new RoundTurns(3, 2)).conditions()).containsExactly(
			new ConditionProgress(ConditionType.TURN_COUNT, 3, 3),
			new ConditionProgress(ConditionType.SIGNAL_RATE, 66, 100));
	}

	@Test
	void 오늘_회전이_없으면_IN_PROGRESS이고_사용률은_0이다() {
		MissionJudgement judgement = condition.judge(new RoundTurns(0, 0));

		assertThat(judgement.status()).isEqualTo(MissionStatus.IN_PROGRESS);
		assertThat(judgement.conditions()).contains(new ConditionProgress(ConditionType.SIGNAL_RATE, 0, 100));
	}
}
