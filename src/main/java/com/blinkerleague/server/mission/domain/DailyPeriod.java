package com.blinkerleague.server.mission.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 일일 미션 기간 [start, end). key는 period_key(예: 2026-11-03)다.
 * 운행은 종료 시각이 속한 날(Asia/Seoul)에 집계한다.
 */
public record DailyPeriod(String key, Instant start, Instant end) {

	public static DailyPeriod of(Instant instant, ZoneId zone) {
		LocalDate date = LocalDate.ofInstant(instant, zone);
		return new DailyPeriod(date.toString(),
			date.atStartOfDay(zone).toInstant(),
			date.plusDays(1).atStartOfDay(zone).toInstant());
	}
}
