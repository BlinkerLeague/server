package com.blinkerleague.server.trip.application.port.out;

import com.blinkerleague.server.trip.domain.Trip;

public interface SaveTripPort {

	/** 새 운행을 이벤트와 함께 저장한다. 같은 tripId가 이미 있으면 {@link TripAlreadyExistsException}. */
	void saveNew(Trip trip);

	/** 동시 요청으로 같은 tripId가 먼저 저장된 경우. */
	class TripAlreadyExistsException extends RuntimeException {

		public TripAlreadyExistsException(String tripId, Throwable cause) {
			super("이미 저장된 운행입니다: " + tripId, cause);
		}
	}
}
