package com.blinkerleague.server.point.application.port.out;

import java.time.Instant;

import com.blinkerleague.server.point.domain.PointReason;

public interface LoadPointLedgerPort {

	boolean exists(PointReason reason, String refId);

	/** 운전자가 [from, to) 동안 적립한 포인트 합계. */
	int sumEarned(Long driverId, Instant from, Instant to);
}
