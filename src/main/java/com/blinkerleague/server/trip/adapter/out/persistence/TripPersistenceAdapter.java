package com.blinkerleague.server.trip.adapter.out.persistence;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
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
		try {
			// 즉시 flush해 동시 업로드로 생긴 PK 충돌을 여기서 잡는다.
			repository.saveAndFlush(trip);
		} catch (DataIntegrityViolationException e) {
			if (isUniqueViolation(e)) {
				throw new TripAlreadyExistsException(trip.getId(), e);
			}
			throw e;
		}
	}

	// PK(trip_id, event_id) 충돌은 유니크 제약 위반으로 분류된다.
	private static boolean isUniqueViolation(DataIntegrityViolationException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation.getKind() == ConstraintKind.UNIQUE;
			}
		}
		return false;
	}
}
