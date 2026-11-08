package com.blinkerleague.server.mission.application.port.out;

/**
 * 미션 달성 보상 적립(plan.md 4.2 DAILY_MISSION). refId는 운전자 · 미션 · 날짜마다 하나라
 * 같은 날을 다시 판정해 불러도 한 번만 적립된다.
 */
public interface MissionRewardPort {

	void reward(Long driverId, String refId, int rewardPoint);

	/** 연속 달성 보상(plan.md 4.3). refId는 운전자 · 보상받는 날마다 하나다. */
	void rewardStreak(Long driverId, String refId);
}
