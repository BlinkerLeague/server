package com.blinkerleague.server.threshold.adapter.in.startup;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultThresholdsSeederTest {

	private final DefaultThresholdsSeeder seeder = new DefaultThresholdsSeeder(null, new ObjectMapper());

	@Test
	void 기본_기준값_파일에서_숫자_필드가_빠지면_0으로_채우지_않고_읽기에_실패한다() {
		// given
		// turn.lateSignalScore만 빠진 파일. 0으로 채우면 0~1 범위 검증을 통과해 늦은 깜빡이가 0점이 된다
		String path = "thresholds/missing-late-signal-score.json";

		// when & then
		assertThatThrownBy(() -> seeder.readDefaults(path))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(path)
			.rootCause()
			.hasMessageContaining("lateSignalScore");
	}
}
