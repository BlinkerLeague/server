package com.blinkerleague.server.badge.domain;

import java.time.LocalDate;
import java.util.Set;

/** 달성한 날들 중 가장 길게 이어진 연속 일수. 배지는 한 번 이어 낸 기록으로 주므로 지금의 연속과 다르다. */
public final class LongestStreak {

	private LongestStreak() {
	}

	public static int of(Set<LocalDate> achievedDays) {
		int longest = 0;
		for (LocalDate day : achievedDays) {
			// 연속의 첫날에서만 센다.
			if (achievedDays.contains(day.minusDays(1))) {
				continue;
			}
			int length = 0;
			for (LocalDate d = day; achievedDays.contains(d); d = d.plusDays(1)) {
				length++;
			}
			longest = Math.max(longest, length);
		}
		return longest;
	}
}
