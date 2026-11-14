package com.blinkerleague.server.report.adapter.out.record;

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
import com.blinkerleague.server.report.domain.TripMetricRecord;
import com.blinkerleague.server.report.domain.WeekDriving;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripMetric;

@DataJpaTest
@ActiveProfiles("test")
@Import({WeekDrivingAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class WeekDrivingAdapterTest {

	private static final Instant FROM = Instant.parse("2026-11-01T15:00:00Z");
	private static final Instant TO = Instant.parse("2026-11-08T15:00:00Z");

	@Autowired
	WeekDrivingAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 그_주에_끝난_운행의_수_거리와_측정한_항목_점수만_읽는다() {
		trip("in", 1L, FROM.plusSeconds(3600), 20.0, List.of(
			new TripMetric(ScoreItem.TURN_SIGNAL, 0.8, 80, 1.0),
			new TripMetric(ScoreItem.SEAT_BELT, "UNSUPPORTED", "SIGNAL_NOT_SUPPORTED", null, null, null, "{}")));
		trip("no-score", 1L, FROM.plusSeconds(7200), 10.0, List.of());
		trip("short", 1L, FROM.plusSeconds(9000), 2.0, null);
		trip("before", 1L, FROM.minusSeconds(1), 30.0, List.of(new TripMetric(ScoreItem.TURN_SIGNAL, 0.5, 50, 1.0)));
		trip("end", 1L, TO, 30.0, List.of(new TripMetric(ScoreItem.TURN_SIGNAL, 0.5, 50, 1.0)));
		trip("other", 2L, FROM.plusSeconds(3600), 30.0, List.of(new TripMetric(ScoreItem.TURN_SIGNAL, 0.5, 50, 1.0)));
		entityManager.flush();

		WeekDriving week = adapter.load(1L, FROM, TO);

		assertThat(week.trips()).isEqualTo(2);
		assertThat(week.distanceKm()).isEqualTo(30.0);
		assertThat(week.metrics()).containsExactly(new TripMetricRecord("in", ScoreItem.TURN_SIGNAL, 80, 20.0));
	}

	@Test
	void 그_주에_점수_대상_운행이_있는_운전자를_한_번씩_읽는다() {
		trip("a1", 1L, FROM.plusSeconds(3600), 20.0, List.of());
		trip("a2", 1L, FROM.plusSeconds(7200), 20.0, List.of());
		trip("b-short", 2L, FROM.plusSeconds(3600), 2.0, null);
		trip("c", 3L, FROM.plusSeconds(3600), 20.0, List.of());
		trip("d-later", 4L, TO, 20.0, List.of());
		entityManager.flush();

		assertThat(adapter.driversWithTrips(FROM, TO)).containsExactly(1L, 3L);
	}

	// metrics가 null이면 점수를 매기지 않은 운행(최소 거리 미만), 빈 목록이면 측정할 항목이 없는 운행
	private void trip(String tripId, Long driverId, Instant endedAt, double distanceKm, List<TripMetric> metrics) {
		Trip trip = Trip.upload(tripId, driverId, endedAt.minusSeconds(1800), endedAt, distanceKm,
			Set.of(ScoreItem.TURN_SIGNAL), List.of(), 3.0);
		if (metrics != null) {
			trip.applyScore(metrics.isEmpty() ? null : 80, metrics);
		}
		entityManager.persist(trip);
	}
}
