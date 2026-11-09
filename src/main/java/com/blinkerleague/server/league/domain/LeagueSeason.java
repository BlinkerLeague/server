package com.blinkerleague.server.league.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/** 리그 시즌(D29): 한국 시간 달력 월. 1일 00:00에 시작해 다음 달 1일 00:00에 끝난다. id는 {@code 2026-11}. */
public record LeagueSeason(YearMonth month, ZoneId zone) {

	public static LeagueSeason containing(Instant at, ZoneId zone) {
		return new LeagueSeason(YearMonth.from(at.atZone(zone)), zone);
	}

	public String id() {
		return month.toString();
	}

	public Instant startsAt() {
		return month.atDay(1).atStartOfDay(zone).toInstant();
	}

	public Instant endsAt() {
		return month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
	}

	public LeagueSeason previous() {
		return new LeagueSeason(month.minusMonths(1), zone);
	}
}
