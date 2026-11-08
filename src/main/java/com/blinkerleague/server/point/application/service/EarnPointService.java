package com.blinkerleague.server.point.application.service;

import java.time.Clock;
import java.time.Instant;

import com.blinkerleague.server.point.application.port.out.DriverPointLockPort;
import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort.PointAlreadyEarnedException;
import com.blinkerleague.server.point.domain.PointDay;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;
import com.blinkerleague.server.point.domain.PointRules;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.Thresholds;

import lombok.RequiredArgsConstructor;

/** 포인트 적립(plan.md 4.2). */
@RequiredArgsConstructor
public class EarnPointService {

	private final LoadPointLedgerPort loadPointLedgerPort;
	private final SavePointEntryPort savePointEntryPort;
	private final ThresholdProvider thresholdProvider;
	private final DriverPointLockPort driverPointLockPort;
	private final Clock clock;

	/** 운행 점수로 적립하고, 실제로 적립한 포인트를 돌려준다. */
	public int earnForTrip(Long driverId, String tripId, double distanceKm, int score) {
		return driverPointLockPort.withDriverLock(driverId, () -> earnTripLocked(driverId, tripId, distanceKm, score));
	}

	private int earnTripLocked(Long driverId, String tripId, double distanceKm, int score) {
		// 같은 운행은 한 번만 적립한다(운행 재전송 · 재계산).
		if (loadPointLedgerPort.exists(PointReason.TRIP, tripId)) {
			return 0;
		}
		Thresholds.Point rule = thresholdProvider.current().thresholds().point();
		Instant now = clock.instant();
		int requested = PointRules.tripPoints(distanceKm, score, rule.tripMaxKm());
		int amount = PointRules.capToDailyLimit(requested, todayEarned(driverId, now), rule.dailyCap());
		try {
			savePointEntryPort.saveNew(PointEntry.trip(driverId, tripId, amount, distanceKm, score, now));
		} catch (PointAlreadyEarnedException e) {
			// 동시에 들어온 같은 운행의 적립이 먼저 저장됐다. 그 적립이 기준이다.
			return 0;
		}
		return amount;
	}

	private int todayEarned(Long driverId, Instant now) {
		PointDay today = PointDay.containing(now, clock.getZone());
		return loadPointLedgerPort.sumEarned(driverId, today.from(), today.to());
	}
}
