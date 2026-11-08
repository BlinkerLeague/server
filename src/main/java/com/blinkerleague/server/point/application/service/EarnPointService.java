package com.blinkerleague.server.point.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.domain.PointEntry;
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
	private final Clock clock;

	/** 운행 점수로 적립하고, 실제로 적립한 포인트를 돌려준다. */
	public int earnForTrip(Long driverId, String tripId, double distanceKm, int score) {
		Thresholds.Point rule = thresholdProvider.current().thresholds().point();
		Instant now = clock.instant();
		int requested = PointRules.tripPoints(distanceKm, score, rule.tripMaxKm());
		int amount = PointRules.capToDailyLimit(requested, todayEarned(driverId, now), rule.dailyCap());
		savePointEntryPort.saveNew(PointEntry.trip(driverId, tripId, amount, distanceKm, score, now));
		return amount;
	}

	// 하루는 Asia/Seoul(주입한 Clock의 시간대) 자정부터 다음 자정 직전까지다(D4).
	private int todayEarned(Long driverId, Instant now) {
		LocalDate today = LocalDate.ofInstant(now, clock.getZone());
		Instant from = today.atStartOfDay(clock.getZone()).toInstant();
		Instant to = today.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
		return loadPointLedgerPort.sumEarned(driverId, from, to);
	}
}
