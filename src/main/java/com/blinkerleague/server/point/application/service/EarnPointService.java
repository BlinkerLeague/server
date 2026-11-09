package com.blinkerleague.server.point.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.function.BiFunction;

import com.blinkerleague.server.point.application.port.in.EarnPointUseCase;
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
public class EarnPointService implements EarnPointUseCase {

	private final LoadPointLedgerPort loadPointLedgerPort;
	private final SavePointEntryPort savePointEntryPort;
	private final ThresholdProvider thresholdProvider;
	private final DriverPointLockPort driverPointLockPort;
	private final Clock clock;

	/** 운행 점수로 적립하고, 실제로 적립한 포인트를 돌려준다. */
	@Override
	public int earnForTrip(Long driverId, String tripId, double distanceKm, int score) {
		Thresholds.Point rule = thresholdProvider.current().thresholds().point();
		int requested = PointRules.tripPoints(distanceKm, score, rule.tripMaxKm());
		return earn(driverId, PointReason.TRIP, tripId, requested,
			(amount, now) -> PointEntry.trip(driverId, tripId, amount, distanceKm, score, now));
	}

	/** 일일 미션 달성 보상을 적립하고, 실제로 적립한 포인트를 돌려준다. refId는 운전자 · 미션 · 날짜마다 하나다. */
	@Override
	public int earnForMission(Long driverId, String refId, int rewardPoint) {
		return earn(driverId, PointReason.DAILY_MISSION, refId, rewardPoint,
			(amount, now) -> PointEntry.dailyMission(driverId, refId, amount, now));
	}

	@Override
	public int earnForStreak(Long driverId, String refId) {
		int reward = thresholdProvider.current().thresholds().point().streakRewardPoint();
		return earn(driverId, PointReason.STREAK_7, refId, reward,
			(amount, now) -> PointEntry.streak(driverId, refId, amount, now));
	}

	@Override
	public int earnForSeasonLeague(Long driverId, String refId) {
		int reward = thresholdProvider.current().thresholds().league().seasonRewardPoint();
		return earn(driverId, PointReason.SEASON_LEAGUE, refId, reward,
			(amount, now) -> PointEntry.seasonLeague(driverId, refId, amount, now));
	}

	// 같은 운전자의 적립은 잠금 안에서 하나씩: 한 번만 적립 확인 → 오늘 합계로 상한 적용 → 저장.
	private int earn(Long driverId, PointReason reason, String refId, int requested,
		BiFunction<Integer, Instant, PointEntry> entry) {
		return driverPointLockPort.withDriverLock(driverId, () -> {
			// 같은 사유 · refId는 한 번만 적립한다(재전송 · 재판정).
			if (loadPointLedgerPort.exists(reason, refId)) {
				return 0;
			}
			Instant now = clock.instant();
			int dailyCap = thresholdProvider.current().thresholds().point().dailyCap();
			int amount = PointRules.capToDailyLimit(requested, todayEarned(driverId, now), dailyCap);
			try {
				savePointEntryPort.saveNew(entry.apply(amount, now));
			} catch (PointAlreadyEarnedException e) {
				// 동시에 들어온 같은 적립이 먼저 저장됐다. 그 적립이 기준이다.
				return 0;
			}
			return amount;
		});
	}

	private int todayEarned(Long driverId, Instant now) {
		PointDay today = PointDay.containing(now, clock.getZone());
		return loadPointLedgerPort.sumEarned(driverId, today.from(), today.to());
	}
}
