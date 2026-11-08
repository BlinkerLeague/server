package com.blinkerleague.server.point.application.port.out;

import java.time.Instant;
import java.util.List;

import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

public interface LoadPointLedgerPort {

	boolean exists(PointReason reason, String refId);

	/** 운전자의 전체 적립 합계(잔액). */
	int balance(Long driverId);

	/** 운전자가 [from, to) 동안 적립한 포인트 합계. */
	int sumEarned(Long driverId, Instant from, Instant to);

	/** 운전자의 적립을 최근 것부터 limit건. */
	List<PointEntry> findRecent(Long driverId, int limit);
}
