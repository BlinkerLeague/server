package com.blinkerleague.server.trip.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.trip.domain.Trip;

public interface LoadTripPort {

	Optional<Trip> findById(String tripId);
}
