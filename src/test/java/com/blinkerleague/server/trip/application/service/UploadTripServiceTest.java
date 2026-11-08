package com.blinkerleague.server.trip.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;
import com.blinkerleague.server.trip.application.port.out.TripMissionPort;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripEventType;
import com.blinkerleague.server.trip.domain.TripMetric;
import com.blinkerleague.server.trip.domain.TripStatus;

class UploadTripServiceTest {

	private static final Instant STARTED_AT = Instant.parse("2026-11-03T08:00:00Z");
	private static final Instant ENDED_AT = Instant.parse("2026-11-03T08:40:00Z");

	private final InMemoryTripRepository trips = new InMemoryTripRepository();
	private final List<Object[]> missionCalls = new ArrayList<>();
	private final UploadTripService service = new UploadTripService(trips, trips,
		() -> new VersionedThresholds(1, ThresholdsFixture.withMinDistanceKm(3.0)),
		(driverId, endedAt) -> {
			missionCalls.add(new Object[] {driverId, endedAt});
			return Optional.of(new TripMissionPort.TripMission(7L, "ACHIEVED"));
		});

	@Test
	void 최소_거리_이상_운행은_이벤트와_함께_저장하고_새로_만들었다고_알린다() {
		UploadTripService.Result result = service.upload(1L, command("trip-1", 12.3,
			new RecordedEvent("event-1", TripEventType.TURN, STARTED_AT.plusSeconds(600),
				"{\"direction\":\"LEFT\",\"signalOn\":true}")));

		assertThat(result).extracting(UploadTripService.Result::tripId, UploadTripService.Result::status, UploadTripService.Result::created)
			.containsExactly("trip-1", TripStatus.SCORED, true);
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

	@ParameterizedTest(name = "{0}km → {1}")
	@CsvSource({"0.0, EXCLUDED_SHORT", "2.9, EXCLUDED_SHORT", "3.0, NO_TURN", "3.1, NO_TURN"})
	void 기준값의_최소_거리_미만_운행은_EXCLUDED_SHORT로_저장한다(double distanceKm, TripStatus expected) {
		UploadTripService.Result result = service.upload(1L, command("trip-1", distanceKm));

		assertThat(result.status()).isEqualTo(expected);
		assertThat(trips.findById("trip-1").orElseThrow().getStatus()).isEqualTo(expected);
	}

	@Test
	void 같은_tripId를_다시_올리면_저장하지_않고_처음_저장한_결과를_돌려준다() {
		service.upload(1L, command("trip-1", 12.3));

		// 재전송 중 내용이 달라져도 처음 저장한 운행이 기준이다.
		UploadTripService.Result retried = service.upload(1L, command("trip-1", 1.0));

		assertThat(retried).extracting(UploadTripService.Result::tripId, UploadTripService.Result::status, UploadTripService.Result::created)
			.containsExactly("trip-1", TripStatus.NO_TURN, false);
		assertThat(trips.findAll()).singleElement()
			.satisfies(trip -> assertThat(trip.getDistanceKm()).isEqualTo(12.3));
	}

	@Test
	void 다른_운전자가_올린_tripId로_업로드하면_CONFLICT로_거부하고_남의_결과를_알려주지_않는다() {
		service.upload(1L, command("trip-1", 12.3));

		assertThatThrownBy(() -> service.upload(2L, command("trip-1", 12.3)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode())
			.isEqualTo(ErrorCode.CONFLICT);
		assertThat(trips.findAll()).singleElement()
			.satisfies(trip -> assertThat(trip.getDriverId()).isEqualTo(1L));
	}

	@Test
	void 확인과_저장_사이에_같은_운행이_먼저_저장되면_다시_읽어_기존_결과를_돌려준다() {
		trips.saveConcurrentlyBeforeNextSave(Trip.upload("trip-1", 1L, STARTED_AT, ENDED_AT, 12.3,
			Set.of(ScoreItem.TURN_SIGNAL), List.of(), 3.0));

		UploadTripService.Result result = service.upload(1L, command("trip-1", 12.3));

		assertThat(result).extracting(UploadTripService.Result::tripId, UploadTripService.Result::status, UploadTripService.Result::created)
			.containsExactly("trip-1", TripStatus.UPLOADED, false);
		assertThat(trips.findAll()).hasSize(1);
	}

	@Test
	void 최소_거리_이상_운행은_업로드할_때_회전_깜빡이_비율로_점수를_매겨_SCORED로_저장한다() {
		UploadTripService.Result result = service.upload(1L, command("trip-1", 12.3,
			turn("event-1", 600, true), turn("event-2", 700, true), turn("event-3", 800, true),
			turn("event-4", 900, false)));

		assertThat(result.status()).isEqualTo(TripStatus.SCORED);
		Trip saved = trips.findById("trip-1").orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(TripStatus.SCORED);
		assertThat(saved.getScore()).isEqualTo(75);
		assertThat(saved.getMetrics())
			.extracting(TripMetric::getMetric, TripMetric::getValue, TripMetric::getScore, TripMetric::getWeight)
			.containsExactly(tuple(ScoreItem.TURN_SIGNAL, 0.75, 75, 1.0));
	}

	@Test
	void signalOn이_없거나_boolean이_아니거나_payload가_없는_회전은_깜빡이를_켜지_않은_회전으로_센다() {
		service.upload(1L, command("trip-1", 12.3,
			turn("event-1", 600, true),
			new RecordedEvent("event-2", TripEventType.TURN, STARTED_AT.plusSeconds(700), "{\"direction\":\"LEFT\"}"),
			new RecordedEvent("event-3", TripEventType.TURN, STARTED_AT.plusSeconds(800), "{\"signalOn\":\"true\"}"),
			new RecordedEvent("event-4", TripEventType.TURN, STARTED_AT.plusSeconds(900), null)));

		assertThat(trips.findById("trip-1").orElseThrow().getScore()).isEqualTo(25);
	}

	private static RecordedEvent turn(String eventId, long secondsAfterStart, boolean signalOn) {
		return new RecordedEvent(eventId, TripEventType.TURN, STARTED_AT.plusSeconds(secondsAfterStart),
			"{\"direction\":\"LEFT\",\"signalOn\":" + signalOn + "}");
	}

	private static UploadTripService.Command command(String tripId, double distanceKm, RecordedEvent... events) {
		return new UploadTripService.Command(tripId, STARTED_AT, ENDED_AT, distanceKm, Set.of(ScoreItem.TURN_SIGNAL),
			List.of(events));
	}

	@Test
	void 업로드하면_운행이_끝난_날의_미션_판정을_기록하고_결과에_담는다() {
		UploadTripService.Result result = service.upload(1L, command("trip-1", 12.3));

		assertThat(missionCalls).singleElement()
			.satisfies(call -> assertThat(call).containsExactly(1L, ENDED_AT));
		assertThat(result.trip().mission()).isEqualTo(new TripMissionPort.TripMission(7L, "ACHIEVED"));
	}

	@Test
	void 같은_운행을_다시_올려도_그날_미션_판정을_결과에_담는다() {
		service.upload(1L, command("trip-1", 12.3));

		UploadTripService.Result retried = service.upload(1L, command("trip-1", 12.3));

		assertThat(retried.created()).isFalse();
		assertThat(retried.trip().mission()).isEqualTo(new TripMissionPort.TripMission(7L, "ACHIEVED"));
	}
}
