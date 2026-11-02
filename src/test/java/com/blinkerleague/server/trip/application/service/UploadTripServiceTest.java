package com.blinkerleague.server.trip.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripEventType;
import com.blinkerleague.server.trip.domain.TripStatus;

class UploadTripServiceTest {

	private static final Instant STARTED_AT = Instant.parse("2026-11-03T08:00:00Z");
	private static final Instant ENDED_AT = Instant.parse("2026-11-03T08:40:00Z");

	private final InMemoryTripRepository trips = new InMemoryTripRepository();
	private final UploadTripService service = new UploadTripService(trips, trips,
		() -> new VersionedThresholds(1, ThresholdsFixture.withMinDistanceKm(3.0)));

	@Test
	void 최소_거리_이상_운행은_이벤트와_함께_UPLOADED로_저장하고_새로_만들었다고_알린다() {
		UploadTripService.Result result = service.upload(1L, command("trip-1", 12.3,
			new RecordedEvent("event-1", TripEventType.TURN, STARTED_AT.plusSeconds(600),
				"{\"direction\":\"LEFT\",\"signalOn\":true}")));

		assertThat(result).isEqualTo(new UploadTripService.Result("trip-1", TripStatus.UPLOADED, true));
		Trip saved = trips.findById("trip-1").orElseThrow();
		assertThat(saved.getDriverId()).isEqualTo(1L);
		assertThat(saved.getDistanceKm()).isEqualTo(12.3);
		assertThat(saved.getSupportedSignals()).containsExactly(ScoreItem.TURN_SIGNAL);
		assertThat(saved.getEvents()).singleElement().satisfies(event -> {
			assertThat(event.getId()).isEqualTo("event-1");
			assertThat(event.getType()).isEqualTo(TripEventType.TURN);
			assertThat(event.getPayload()).isEqualTo("{\"direction\":\"LEFT\",\"signalOn\":true}");
		});
	}

	private static UploadTripService.Command command(String tripId, double distanceKm, RecordedEvent... events) {
		return new UploadTripService.Command(tripId, STARTED_AT, ENDED_AT, distanceKm, Set.of(ScoreItem.TURN_SIGNAL),
			List.of(events));
	}
}
