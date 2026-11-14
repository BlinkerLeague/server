package com.blinkerleague.server.badge.application.port.in;

import java.util.List;

/** 운전자의 누적 기록으로 아직 받지 않은 배지를 준다(plan.md 4.3). trip이 운행 업로드 후 부른다. */
public interface AwardBadgesUseCase {

	/** 이번에 새로 준 배지 코드. */
	List<String> awardEarned(Long driverId);
}
