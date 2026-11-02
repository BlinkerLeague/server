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

	@Override
	public Optional<Trip> findById(String tripId) {
		return Optional.ofNullable(trips.get(tripId));
	}

	@Override
	public void saveNew(Trip trip) {
		if (trips.containsKey(trip.getId())) {
			throw new TripAlreadyExistsException(trip.getId(), null);
		}
		trips.put(trip.getId(), trip);
	}

	Collection<Trip> findAll() {
		return trips.values();
	}
}
