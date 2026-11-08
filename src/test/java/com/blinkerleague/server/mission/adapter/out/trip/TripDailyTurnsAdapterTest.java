package com.blinkerleague.server.mission.adapter.out.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.mission.domain.DailyTurns;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripEventType;

@DataJpaTest
@ActiveProfiles("test")
@Import({TripDailyTurnsAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class TripDailyTurnsAdapterTest {

	// 2026-11-03 KST 하루
	private static final Instant DAY_START = Instant.parse("2026-11-02T15:00:00Z");
	private static final Instant DAY_END = Instant.parse("2026-11-03T15:00:00Z");

	@Autowired
	TripDailyTurnsAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	private int eventSeq;

	@Test
	void 그날_운행들의_회전_수와_깜빡이를_켠_회전_수를_합한다() {
		save("trip-1", 1L, Instant.parse("2026-11-03T00:00:00Z"), 12.0, turn(true), turn(false),
			new RecordedEvent(nextEventId(), TripEventType.HARSH_BRAKE, null, null));
		save("trip-2", 1L, Instant.parse("2026-11-03T05:00:00Z"), 8.0, turn(true));

		assertThat(adapter.load(1L, DAY_START, DAY_END)).isEqualTo(new DailyTurns(3, 2));
	}

	@Test
	void 다른_운전자_다른_날_3km_미만_운행의_회전은_세지_않는다() {
		save("mine", 1L, Instant.parse("2026-11-03T00:00:00Z"), 12.0, turn(true));
		save("other-driver", 2L, Instant.parse("2026-11-03T00:00:00Z"), 12.0, turn(true));
		save("next-day", 1L, DAY_END, 12.0, turn(true));
		save("short", 1L, Instant.parse("2026-11-03T01:00:00Z"), 2.9, turn(true));

		assertThat(adapter.load(1L, DAY_START, DAY_END)).isEqualTo(new DailyTurns(1, 1));
	}

	@Test
	void payload가_없는_회전은_깜빡이를_안_켠_회전으로_센다() {
		save("trip-1", 1L, Instant.parse("2026-11-03T00:00:00Z"), 12.0,
			new RecordedEvent(nextEventId(), TripEventType.TURN, null, null));

		assertThat(adapter.load(1L, DAY_START, DAY_END)).isEqualTo(new DailyTurns(1, 0));
	}

	private void save(String tripId, Long driverId, Instant endedAt, double distanceKm, RecordedEvent... events) {
		Instant startedAt = endedAt.minusSeconds(1800);
		List<RecordedEvent> timed = java.util.Arrays.stream(events)
			.map(e -> new RecordedEvent(e.eventId(), e.type(), startedAt.plusSeconds(60), e.payload()))
			.toList();
		entityManager.persist(Trip.upload(tripId, driverId, startedAt, endedAt, distanceKm,
			Set.of(ScoreItem.TURN_SIGNAL), timed, 3.0));
		entityManager.flush();
	}

	private RecordedEvent turn(boolean signalOn) {
		return new RecordedEvent(nextEventId(), TripEventType.TURN, null, "{\"signalOn\":" + signalOn + "}");
	}

	private String nextEventId() {
		return "event-" + (++eventSeq);
	}
}
