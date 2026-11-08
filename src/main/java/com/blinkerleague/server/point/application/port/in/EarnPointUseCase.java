package com.blinkerleague.server.point.application.port.in;

/**
 * 포인트 적립(plan.md 4.2). trip(운행 점수)과 mission(달성 보상)이 부르므로 in-port로 둔다.
 * 같은 사유 · refId는 한 번만 적립하고, 일일 상한을 넘으면 남은 만큼만 적립한다. 실제로 적립한 포인트를 돌려준다.
 */
public interface EarnPointUseCase {

	int earnForTrip(Long driverId, String tripId, double distanceKm, int score);

	int earnForMission(Long driverId, String refId, int rewardPoint);

	/** 연속 달성 보상(기준값 point.streakRewardPoint). refId는 운전자 · 보상받는 날마다 하나다. */
	int earnForStreak(Long driverId, String refId);
}
