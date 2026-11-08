package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MissionStatusTest {

	@ParameterizedTest(name = "{0}, 하루 끝남={1} → {2}")
	@CsvSource({
		"IN_PROGRESS, false, IN_PROGRESS",
		"IN_PROGRESS, true,  FAILED",
		"ACHIEVED,    false, ACHIEVED",
		"ACHIEVED,    true,  ACHIEVED"})
	void 그날이_끝날_때까지_달성하지_못하면_FAILED로_정한다(MissionStatus judged, boolean dayEnded,
		MissionStatus expected) {
		assertThat(judged.settle(dayEnded)).isEqualTo(expected);
	}
}
