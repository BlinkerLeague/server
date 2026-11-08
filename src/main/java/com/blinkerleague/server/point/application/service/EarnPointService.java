package com.blinkerleague.server.point.application.service;

import java.time.Clock;

import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointRules;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;

import lombok.RequiredArgsConstructor;

/** 포인트 적립(plan.md 4.2). */
@RequiredArgsConstructor
public class EarnPointService {

	private final LoadPointLedgerPort loadPointLedgerPort;
	private final SavePointEntryPort savePointEntryPort;
	private final ThresholdProvider thresholdProvider;
	private final Clock clock;

	/** 운행 점수로 적립하고, 실제로 적립한 포인트를 돌려준다. */
	public int earnForTrip(Long driverId, String tripId, double distanceKm, int score) {
		int tripMaxKm = thresholdProvider.current().thresholds().point().tripMaxKm();
		int amount = PointRules.tripPoints(distanceKm, score, tripMaxKm);
		savePointEntryPort.saveNew(PointEntry.trip(driverId, tripId, amount, distanceKm, score, clock.instant()));
		return amount;
	}
}
