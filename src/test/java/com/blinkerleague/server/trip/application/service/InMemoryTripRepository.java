package com.blinkerleague.server.trip.application.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort;
import com.blinkerleague.server.trip.domain.Trip;

/** 운행 저장 포트의 메모리 구현. */
class InMemoryTripRepository implements LoadTripPort, SaveTripPort {

	private final Map<String, Trip> trips = new LinkedHashMap<>();
	private Trip racingTrip;

	@Override
	public Optional<Trip> findById(String tripId) {
		return Optional.ofNullable(trips.get(tripId));
	}

	@Override
	public void saveNew(Trip trip) {
		if (racingTrip != null) {
			trips.put(racingTrip.getId(), racingTrip);
			racingTrip = null;
		}
		if (trips.containsKey(trip.getId())) {
			throw new TripAlreadyExistsException(trip.getId(), null);
		}
		trips.put(trip.getId(), trip);
	}

	/** 다음 saveNew 직전에 다른 요청이 이 운행을 먼저 저장한 상황을 만든다(동시 업로드). */
	void saveConcurrentlyBeforeNextSave(Trip trip) {
		racingTrip = trip;
	}

	Collection<Trip> findAll() {
		return trips.values();
	}
}
