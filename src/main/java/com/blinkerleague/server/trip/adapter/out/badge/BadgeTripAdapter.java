package com.blinkerleague.server.trip.adapter.out.badge;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.badge.application.port.in.AwardBadgesUseCase;
import com.blinkerleague.server.trip.application.port.out.TripBadgePort;

import lombok.RequiredArgsConstructor;

/** trip의 배지 포트를 badge 유스케이스로 연결한다. */
@Component
@RequiredArgsConstructor
public class BadgeTripAdapter implements TripBadgePort {

	private final AwardBadgesUseCase awardBadgesUseCase;

	@Override
	public void awardEarned(Long driverId) {
		awardBadgesUseCase.awardEarned(driverId);
	}
}
