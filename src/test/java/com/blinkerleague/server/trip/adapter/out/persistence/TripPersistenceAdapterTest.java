package com.blinkerleague.server.trip.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripEventType;
import com.blinkerleague.server.trip.domain.TripStatus;

@DataJpaTest
@ActiveProfiles("test")
@Import({TripPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class TripPersistenceAdapterTest {

	private static final Instant STARTED_AT = Instant.parse("2026-11-03T08:00:00Z");
	private static final Instant ENDED_AT = Instant.parse("2026-11-03T08:40:00Z");

	@Autowired
	TripPersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 운행을_이벤트와_함께_저장하고_tripId로_다시_읽는다() {
		adapter.saveNew(trip("trip-1", 1L,
			new RecordedEvent("event-2", TripEventType.HARSH_BRAKE, STARTED_AT.plusSeconds(900), null),
			new RecordedEvent("event-1", TripEventType.TURN, STARTED_AT.plusSeconds(600), "{\"signalOn\":true}")));
		// 영속성 컨텍스트를 비워 실제로 DB에서 읽은 값을 확인한다.
		entityManager.flush();
		entityManager.clear();

		Trip loaded = adapter.findById("trip-1").orElseThrow();

		assertThat(loaded.getDriverId()).isEqualTo(1L);
		assertThat(loaded.getStartedAt()).isEqualTo(STARTED_AT);
		assertThat(loaded.getEndedAt()).isEqualTo(ENDED_AT);
		assertThat(loaded.getDistanceKm()).isEqualTo(12.3);
		assertThat(loaded.getSupportedSignals()).containsExactlyInAnyOrder(ScoreItem.TURN_SIGNAL,
			ScoreItem.SMOOTH_DRIVING);
		assertThat(loaded.getStatus()).isEqualTo(TripStatus.UPLOADED);
		assertThat(loaded.getEvents()).extracting(event -> event.getId(), event -> event.getPayload())
			.containsExactly(
				tuple("event-1", "{\"signalOn\":true}"),
				tuple("event-2", null));
		assertThat(adapter.findById("없는-운행")).isEmpty();
	}

	private static Trip trip(String tripId, Long driverId, RecordedEvent... events) {
		return Trip.upload(tripId, driverId, STARTED_AT, ENDED_AT, 12.3,
			Set.of(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING), List.of(events), 3.0);
	}
}
