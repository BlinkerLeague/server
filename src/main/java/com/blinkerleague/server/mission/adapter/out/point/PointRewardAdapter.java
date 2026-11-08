package com.blinkerleague.server.mission.adapter.out.point;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.MissionRewardPort;
import com.blinkerleague.server.point.application.port.in.EarnPointUseCase;

import lombok.RequiredArgsConstructor;

/** mission의 보상 포트를 point 유스케이스로 연결한다. */
@Component
@RequiredArgsConstructor
public class PointRewardAdapter implements MissionRewardPort {

	private final EarnPointUseCase earnPointUseCase;

	@Override
	public void reward(Long driverId, String refId, int rewardPoint) {
		earnPointUseCase.earnForMission(driverId, refId, rewardPoint);
	}
}
