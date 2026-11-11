package com.blinkerleague.server.threshold.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.ThresholdsFixture;

class ThresholdsValidatorTest {

	@Test
	void plan_예시_기준값은_위반이_없다() {
		// when & then
		assertThat(ThresholdsValidator.validate(ThresholdsFixture.planExample())).isEmpty();
	}

	@Test
	void 가중치에_음수가_있으면_위반이다() {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith("weights.SMOOTH_DRIVING", -0.1);

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("weights.SMOOTH_DRIVING 값은 0 이상이어야 합니다.");
	}

	@Test
	void 가중치_합이_0이면_위반이다() {
		// given
		Thresholds thresholds = ThresholdsFixture.planExampleWith("weights", Map.of(ScoreItem.TURN_SIGNAL, 0.0));

		// when & then
		assertThat(ThresholdsValidator.validate(thresholds))
			.containsExactly("weights 합은 0보다 커야 합니다.");
	}
}
