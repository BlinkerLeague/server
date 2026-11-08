package com.blinkerleague.server.mission.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** 일일 미션 기간. 운행은 종료 시각이 속한 날(Asia/Seoul)에 집계한다. */
public record DailyPeriod(String key) {

	public static DailyPeriod of(Instant instant, ZoneId zone) {
		return new DailyPeriod(LocalDate.ofInstant(instant, zone).toString());
	}
}
