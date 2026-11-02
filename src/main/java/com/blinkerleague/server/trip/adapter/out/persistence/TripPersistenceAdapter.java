package com.blinkerleague.server.trip.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort;
import com.blinkerleague.server.trip.domain.Trip;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TripPersistenceAdapter implements LoadTripPort, SaveTripPort {

	private final TripJpaRepository repository;

	@Override
	public Optional<Trip> findById(String tripId) {
		return repository.findById(tripId);
	}

	@Override
	public void saveNew(Trip trip) {
		repository.saveAndFlush(trip);
	}
}
