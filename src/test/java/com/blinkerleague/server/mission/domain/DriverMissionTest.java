package com.blinkerleague.server.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class DriverMissionTest {

	private static final Instant ACHIEVED_AT = Instant.parse("2026-11-03T00:40:00Z");

	@Test
	void 달성으로_판정되면_ACHIEVED와_달성_시각을_기록한다() {
		DriverMission mission = DriverMission.start(1L, 10L, "2026-11-03");

		mission.apply(MissionStatus.ACHIEVED, ACHIEVED_AT);

		assertThat(mission.getStatus()).isEqualTo(MissionStatus.ACHIEVED);
		assertThat(mission.getAchievedAt()).isEqualTo(ACHIEVED_AT);
	}
}
