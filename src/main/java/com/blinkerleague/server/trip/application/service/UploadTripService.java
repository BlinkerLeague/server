package com.blinkerleague.server.trip.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.TripStatus;

import lombok.RequiredArgsConstructor;

/** 운행 · 이벤트 업로드(plan.md 3.4). */
@RequiredArgsConstructor
public class UploadTripService {

	private final LoadTripPort loadTripPort;
	private final SaveTripPort saveTripPort;
	private final ThresholdProvider thresholdProvider;

	public Result upload(Long driverId, Command command) {
		throw new UnsupportedOperationException();
	}

	public record Command(String tripId, Instant startedAt, Instant endedAt, double distanceKm,
		Set<ScoreItem> supportedSignals, List<RecordedEvent> events) {
	}

	/** created가 false면 이미 업로드된 운행을 다시 받은 것이다. */
	public record Result(String tripId, TripStatus status, boolean created) {
	}
}
