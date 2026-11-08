package com.blinkerleague.server.mission.domain;

import java.time.LocalDate;
import java.util.Set;

/** 미션 연속 달성 일수(plan.md 4.3). 달성한 날(ACHIEVED)만 센다. */
public final class Streak {

	private Streak() {
	}

	/** 오늘(달성했으면) 또는 어제부터 거꾸로 이어진 달성 일수. 오늘 아직 달성하지 못한 것은 끊김이 아니다. */
	public static int current(Set<LocalDate> achievedDays, LocalDate today) {
		throw new UnsupportedOperationException();
	}
}
