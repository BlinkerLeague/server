package com.blinkerleague.server.mission.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.TurnSignalCondition;

class SeedMissionServiceTest {

	private final InMemoryMissionStore store = new InMemoryMissionStore();
	private final SeedMissionService service = new SeedMissionService(store);

	@Test
	void 저장된_미션이_없으면_기본_미션을_넣는다() {
		Mission seed = mission("회전 깜빡이 완벽");

		service.seedIfAbsent(List.of(seed));

		assertThat(store.saved).containsExactly(seed);
	}

	private static Mission mission(String title) {
		return Mission.daily(title, new TurnSignalCondition(3, 100), 20,
			LocalDate.of(2026, 11, 1), LocalDate.of(2099, 12, 31));
	}
}
