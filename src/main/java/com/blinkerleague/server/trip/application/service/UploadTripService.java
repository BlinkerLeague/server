package com.blinkerleague.server.trip.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripStatus;

import lombok.RequiredArgsConstructor;

/** 운행 · 이벤트 업로드(plan.md 3.4). */
@RequiredArgsConstructor
public class UploadTripService {

	private final LoadTripPort loadTripPort;
	private final SaveTripPort saveTripPort;
	private final ThresholdProvider thresholdProvider;

	public Result upload(Long driverId, Command command) {
		Optional<Trip> existing = loadTripPort.findById(command.tripId());
		if (existing.isPresent()) {
			if (!existing.get().getDriverId().equals(driverId)) {
				throw new BusinessException(ErrorCode.CONFLICT, "이미 다른 운전자가 사용한 운행 ID입니다.");
			}
			return new Result(existing.get().getId(), existing.get().getStatus(), false);
		}
		double minDistanceKm = thresholdProvider.current().thresholds().trip().minDistanceKm();
		Trip trip = Trip.upload(command.tripId(), driverId, command.startedAt(), command.endedAt(),
			command.distanceKm(), command.supportedSignals(), command.events(), minDistanceKm);
		saveTripPort.saveNew(trip);
		return new Result(trip.getId(), trip.getStatus(), true);
	}

	public record Command(String tripId, Instant startedAt, Instant endedAt, double distanceKm,
		Set<ScoreItem> supportedSignals, List<RecordedEvent> events) {
	}

	/** created가 false면 이미 업로드된 운행을 다시 받은 것이다. */
	public record Result(String tripId, TripStatus status, boolean created) {
	}
}
