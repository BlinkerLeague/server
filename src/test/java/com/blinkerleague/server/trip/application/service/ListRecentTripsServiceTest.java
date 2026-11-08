package com.blinkerleague.server.trip.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;
import com.blinkerleague.server.trip.domain.Trip;

class ListRecentTripsServiceTest {

	private static final Instant STARTED_AT = Instant.parse("2026-11-03T08:00:00Z");

	private final InMemoryTripRepository trips = new InMemoryTripRepository();
	private final ListRecentTripsService service = new ListRecentTripsService(trips,
		() -> new VersionedThresholds(1, ThresholdsFixture.planExample()));

	@Test
	void limit을_주지_않으면_최근_10건을_돌려준다() {
		saveTrips(1L, 11);

		assertThat(service.list(1L, null)).hasSize(10);
	}

	@Test
	void limit이_50을_넘으면_50건으로_자른다() {
		saveTrips(1L, 51);

		assertThat(service.list(1L, 100)).hasSize(50);
	}

	private void saveTrips(Long driverId, int count) {
		for (int i = 0; i < count; i++) {
			trips.saveNew(Trip.upload("trip-" + i, driverId, STARTED_AT.plusSeconds(3600L * i),
				STARTED_AT.plusSeconds(3600L * i + 1800), 12.3, Set.of(ScoreItem.TURN_SIGNAL), List.of(), 3.0));
		}
	}
}
