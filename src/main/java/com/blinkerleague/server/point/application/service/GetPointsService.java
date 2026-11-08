package com.blinkerleague.server.point.application.service;

import java.time.Clock;
import java.util.List;

import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.domain.PointDay;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;

import lombok.RequiredArgsConstructor;

/** 내 포인트 조회(plan.md 4.2, SCR-06). */
@RequiredArgsConstructor
public class GetPointsService {

	private final LoadPointLedgerPort loadPointLedgerPort;
	private final ThresholdProvider thresholdProvider;
	private final Clock clock;

	public Points get(Long driverId) {
		PointDay today = PointDay.containing(clock.instant(), clock.getZone());
		return new Points(loadPointLedgerPort.balance(driverId),
			loadPointLedgerPort.sumEarned(driverId, today.from(), today.to()),
			thresholdProvider.current().thresholds().point().dailyCap(), List.of());
	}

	/** balance는 전체 적립 합계, todayEarned는 오늘(Asia/Seoul) 적립 합계. */
	public record Points(int balance, int todayEarned, int dailyCap, List<PointEntry> entries) {
	}
}
