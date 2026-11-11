package com.blinkerleague.server.threshold.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
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

	@Test
	void 이미_저장된_기준값이_있으면_재시작해도_기본값으로_덮어쓰지_않는다() {
		VersionedThresholds changed = new VersionedThresholds(3, ThresholdsFixture.withMinDistanceKm(2.0));
		store.save(changed);

		service.seedIfAbsent(ThresholdsFixture.planExample());

		assertThat(store.stored()).contains(changed);
		assertThat(service.current()).isEqualTo(changed);
	}

	@Test
	void 기준값을_바꾸면_version이_1_증가하고_현재값이_바뀐_값이_된다() {
		service.seedIfAbsent(ThresholdsFixture.planExample());
		Thresholds changed = ThresholdsFixture.withMinDistanceKm(2.0);

		VersionedThresholds updated = service.update(changed);

		VersionedThresholds expected = new VersionedThresholds(2, changed);
		assertThat(updated).isEqualTo(expected);
		assertThat(store.stored()).contains(expected);
		assertThat(service.current()).isEqualTo(expected);
	}

	@Test
	void 현재값은_캐시에서_읽어_조회할_때마다_저장소를_읽지_않는다() {
		service.seedIfAbsent(ThresholdsFixture.planExample());
		int loadsAfterSeed = store.loadCount();

		service.current();
		service.current();
		service.current();

		assertThat(store.loadCount()).isEqualTo(loadsAfterSeed);
	}

	@Test
	void 검증에_어긋난_기준값으로_바꾸면_INVALID_INPUT이고_version과_현재값이_그대로다() {
		// given
		service.seedIfAbsent(ThresholdsFixture.planExample());
		VersionedThresholds before = service.current();
		Thresholds invalid = ThresholdsFixture.planExampleWith("weights.SMOOTH_DRIVING", -0.1);

		// when & then
		assertThatThrownBy(() -> service.update(invalid))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("weights.SMOOTH_DRIVING 값은 0 이상이어야 합니다.")
			.extracting(e -> ((BusinessException) e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_INPUT);
		assertThat(store.stored()).contains(before);
		assertThat(service.current()).isEqualTo(before);
	}

	@Test
	void 저장된_기준값이_없을_때_기본값이_검증에_어긋나면_저장하지_않고_시작을_멈춘다() {
		// given
		Thresholds invalidDefaults = ThresholdsFixture.planExampleWith("point.streakRewardDays", 0);

		// when & then
		assertThatThrownBy(() -> service.seedIfAbsent(invalidDefaults))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("point.streakRewardDays 값은 1 이상이어야 합니다.");
		assertThat(store.stored()).isEmpty();
	}
}
