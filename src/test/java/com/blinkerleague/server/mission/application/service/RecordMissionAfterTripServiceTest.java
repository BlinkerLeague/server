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
import com.blinkerleague.server.mission.domain.DriverMission;
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
	private final List<Object[]> rewards = new ArrayList<>();

	private final RecordMissionAfterTripService service = new RecordMissionAfterTripService(
		date -> Optional.of(seed),
		(driverId, from, to) -> {
			loadedRanges.add(new Instant[] {from, to});
			return rounds;
		},
		store, (driverId, refId, rewardPoint) -> rewards.add(new Object[] {driverId, refId, rewardPoint}),
		Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));

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

	@Test
	void 같은_날_다시_판정하면_기존_기록을_갱신하고_달성은_유지한다() {
		Instant endedAt = Instant.parse("2026-11-03T00:00:00Z");
		rounds.add(new RoundTurns(3, 3));
		service.recordAfterTrip(1L, endedAt);
		rounds.add(new RoundTurns(3, 1));

		Optional<MissionOutcome> outcome = service.recordAfterTrip(1L, endedAt.plusSeconds(3600));

		assertThat(outcome.map(MissionOutcome::status)).contains(MissionStatus.ACHIEVED);
		assertThat(store.saved).singleElement()
			.satisfies(saved -> assertThat(saved.getStatus()).isEqualTo(MissionStatus.ACHIEVED));
	}

	@Test
	void 그날_운영_중인_미션이_없으면_기록하지_않고_빈_결과를_돌려준다() {
		RecordMissionAfterTripService noMission = new RecordMissionAfterTripService(date -> Optional.empty(),
			(driverId, from, to) -> rounds, store, (driverId, refId, rewardPoint) -> rewards.add(new Object[0]),
			Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));

		assertThat(noMission.recordAfterTrip(1L, NOW)).isEmpty();
		assertThat(store.saved).isEmpty();
	}

	@Test
	void 확인과_저장_사이에_같은_날_기록이_먼저_생기면_그_기록에_판정을_반영한다() {
		store.saveConcurrentlyBeforeNextInsert(DriverMission.start(1L, seed.getId(), "2026-11-03"));
		rounds.add(new RoundTurns(3, 3));

		Optional<MissionOutcome> outcome = service.recordAfterTrip(1L, Instant.parse("2026-11-03T00:00:00Z"));

		assertThat(outcome.map(MissionOutcome::status)).contains(MissionStatus.ACHIEVED);
		assertThat(store.saved).singleElement()
			.satisfies(saved -> assertThat(saved.getStatus()).isEqualTo(MissionStatus.ACHIEVED));
	}

	@Test
	void 판정만_하면_기록하지_않고_그날_미션_결과를_돌려준다() {
		rounds.add(new RoundTurns(3, 3));

		Optional<MissionOutcome> outcome = service.judgeDay(1L, Instant.parse("2026-11-02T16:00:00Z"));

		assertThat(outcome).contains(new MissionOutcome(seed.getId(), MissionStatus.ACHIEVED));
		assertThat(loadedRanges).singleElement()
			.satisfies(range -> assertThat(range[0]).isEqualTo(Instant.parse("2026-11-02T15:00:00Z")));
		assertThat(store.saved).isEmpty();
	}

	// 한국 시간 2026-11-02 10:00(지금보다 하루 전)에 끝난 운행
	private static final Instant YESTERDAY_TRIP = Instant.parse("2026-11-02T01:00:00Z");

	@Test
	void 이미_끝난_날의_운행을_늦게_올려도_달성하지_못했으면_그날은_FAILED로_기록한다() {
		rounds.add(new RoundTurns(2, 2));

		Optional<MissionOutcome> outcome = service.recordAfterTrip(1L, YESTERDAY_TRIP);

		assertThat(outcome.map(MissionOutcome::status)).contains(MissionStatus.FAILED);
		assertThat(store.saved).singleElement().satisfies(saved -> {
			assertThat(saved.getPeriodKey()).isEqualTo("2026-11-02");
			assertThat(saved.getStatus()).isEqualTo(MissionStatus.FAILED);
		});
	}

	@Test
	void 끝난_날을_판정만_해도_달성하지_못했으면_FAILED다() {
		rounds.add(new RoundTurns(2, 2));

		assertThat(service.judgeDay(1L, YESTERDAY_TRIP).map(MissionOutcome::status)).contains(MissionStatus.FAILED);
		assertThat(store.saved).isEmpty();
	}

	@Test
	void FAILED가_된_날의_운행을_늦게_올려_조건을_채우면_ACHIEVED로_바꾼다() {
		rounds.add(new RoundTurns(2, 2));
		service.recordAfterTrip(1L, YESTERDAY_TRIP);
		rounds.add(new RoundTurns(3, 3));

		Optional<MissionOutcome> outcome = service.recordAfterTrip(1L, YESTERDAY_TRIP.plusSeconds(600));

		assertThat(outcome.map(MissionOutcome::status)).contains(MissionStatus.ACHIEVED);
		assertThat(store.saved).singleElement().satisfies(saved -> {
			assertThat(saved.getStatus()).isEqualTo(MissionStatus.ACHIEVED);
			assertThat(saved.getAchievedAt()).isEqualTo(NOW);
		});
	}

	@Test
	void 그날을_달성하면_미션의_보상_포인트를_운전자_미션_날짜_기준으로_적립한다() {
		rounds.add(new RoundTurns(3, 3));

		service.recordAfterTrip(1L, Instant.parse("2026-11-03T00:00:00Z"));

		assertThat(rewards).singleElement()
			.satisfies(call -> assertThat(call).containsExactly(1L, "1:" + seed.getId() + ":2026-11-03", 20));
	}
}
