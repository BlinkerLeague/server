package com.blinkerleague.server.threshold.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

class ThresholdServiceTest {

	private final InMemoryThresholdStore store = new InMemoryThresholdStore();
	private final ThresholdService service = new ThresholdService(store, store);

	@Test
	void 저장된_기준값이_없으면_기본값을_version_1로_저장하고_현재값으로_돌려준다() {
		Thresholds defaults = ThresholdsFixture.planExample();

		service.seedIfAbsent(defaults);

		VersionedThresholds expected = new VersionedThresholds(1, defaults);
		assertThat(store.stored()).contains(expected);
		assertThat(service.current()).isEqualTo(expected);
	}
}
