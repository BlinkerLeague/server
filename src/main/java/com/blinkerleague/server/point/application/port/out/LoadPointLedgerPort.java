package com.blinkerleague.server.point.application.port.out;

import java.time.Instant;

public interface LoadPointLedgerPort {

	/** 운전자가 [from, to) 동안 적립한 포인트 합계. */
	int sumEarned(Long driverId, Instant from, Instant to);
}
