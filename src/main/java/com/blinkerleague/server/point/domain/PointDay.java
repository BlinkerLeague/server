package com.blinkerleague.server.point.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** 일일 상한을 세는 하루 [from, to). 시간대는 Asia/Seoul(주입한 Clock의 시간대)이다(D4). */
public record PointDay(Instant from, Instant to) {

	public static PointDay containing(Instant now, ZoneId zone) {
		LocalDate day = LocalDate.ofInstant(now, zone);
		return new PointDay(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant());
	}
}
