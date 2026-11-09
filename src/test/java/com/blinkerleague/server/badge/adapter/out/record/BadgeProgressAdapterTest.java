package com.blinkerleague.server.badge.adapter.out.record;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.badge.domain.BadgeProgress;
import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.mission.domain.DriverMission;
import com.blinkerleague.server.mission.domain.MissionStatus;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripEventType;

@DataJpaTest
@ActiveProfiles("test")
@Import({BadgeProgressAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class BadgeProgressAdapterTest {

	private static final Instant ENDED_AT = Instant.parse("2026-11-03T00:00:00Z");

	@Autowired
	BadgeProgressAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	private int eventSeq;

	@Test
	void 점수가_난_운행_수_가장_긴_연속_달성_깜빡이를_켠_회전_수를_센다() {
		// 점수가 난 운행: 켠 회전 2, 안 켠 회전 1, 문자열 "true"(D23으로 안 켠 회전)
		saveTrip("scored", 1L, 12.0, 100, turn("true"), turn("true"), turn("false"), turn("\"true\""));
		// 최소 거리 미만 운행의 회전은 세지 않는다.
		saveTrip("short", 1L, 2.0, null, turn("true"));
		saveTrip("other", 2L, 12.0, 100, turn("true"));
		achieved(1L, "2026-11-01", "2026-11-02", "2026-11-04");
		achieved(2L, "2026-11-01", "2026-11-02", "2026-11-03");
		entityManager.flush();

		assertThat(adapter.progress(1L)).isEqualTo(new BadgeProgress(1, 2, 2, 0));
	}

	private void saveTrip(String tripId, Long driverId, double distanceKm, Integer score, RecordedEvent... events) {
		Instant startedAt = ENDED_AT.minusSeconds(1800);
		List<RecordedEvent> timed = Arrays.stream(events)
			.map(e -> new RecordedEvent(e.eventId(), e.type(), startedAt.plusSeconds(60), e.payload()))
			.toList();
		Trip trip = Trip.upload(tripId, driverId, startedAt, ENDED_AT, distanceKm, Set.of(ScoreItem.TURN_SIGNAL),
			timed, 3.0);
		if (score != null) {
			trip.applyScore(score, List.of());
		}
		entityManager.persist(trip);
	}

	private RecordedEvent turn(String signalOn) {
		return new RecordedEvent("event-" + (++eventSeq), TripEventType.TURN, null, "{\"signalOn\":" + signalOn + "}");
	}

	private void achieved(Long driverId, String... days) {
		for (String day : days) {
			DriverMission record = DriverMission.start(driverId, 10L, day);
			record.apply(MissionStatus.ACHIEVED, ENDED_AT);
			entityManager.persist(record);
		}
	}
}
