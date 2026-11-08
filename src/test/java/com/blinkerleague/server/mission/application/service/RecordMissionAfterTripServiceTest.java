package com.blinkerleague.server.mission.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.mission.application.port.in.RecordMissionAfterTripUseCase.MissionOutcome;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.MissionStatus;
import com.blinkerleague.server.mission.domain.RoundTurns;
import com.blinkerleague.server.mission.domain.TurnSignalCondition;

class RecordMissionAfterTripServiceTest {

	// 지금은 2026-11-03 12:00 KST
	private static final Instant NOW = Instant.parse("2026-11-03T03:00:00Z");

	private final Mission seed = Mission.daily("회전 깜빡이 완벽", new TurnSignalCondition(3, 100), 20,
		LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31));
	private final List<RoundTurns> rounds = new ArrayList<>();
	private final List<Instant[]> loadedRanges = new ArrayList<>();
	private final InMemoryDriverMissionStore store = new InMemoryDriverMissionStore();

	private final RecordMissionAfterTripService service = new RecordMissionAfterTripService(
		date -> Optional.of(seed),
		(driverId, from, to) -> {
			loadedRanges.add(new Instant[] {from, to});
			return rounds;
		},
		store, Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));

	@Test
	void 운행이_끝난_날의_미션을_판정해_driver_mission에_남기고_결과를_돌려준다() {
		rounds.add(new RoundTurns(3, 3));
		// 한국 시간 2026-11-03 01:00에 끝난 운행
		Instant endedAt = Instant.parse("2026-11-02T16:00:00Z");

		Optional<MissionOutcome> outcome = service.recordAfterTrip(1L, endedAt);

		assertThat(outcome).contains(new MissionOutcome(seed.getId(), MissionStatus.ACHIEVED));
		assertThat(loadedRanges).singleElement().satisfies(range -> {
			assertThat(range[0]).isEqualTo(Instant.parse("2026-11-02T15:00:00Z"));
			assertThat(range[1]).isEqualTo(Instant.parse("2026-11-03T15:00:00Z"));
		});
		assertThat(store.saved).singleElement().satisfies(saved -> {
			assertThat(saved.getDriverId()).isEqualTo(1L);
			assertThat(saved.getPeriodKey()).isEqualTo("2026-11-03");
			assertThat(saved.getStatus()).isEqualTo(MissionStatus.ACHIEVED);
			assertThat(saved.getAchievedAt()).isEqualTo(NOW);
		});
	}
}
