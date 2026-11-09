package com.blinkerleague.server.league.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/** 리그 시즌(D29): 한국 시간 달력 월. 1일 00:00에 시작해 다음 달 1일 00:00에 끝난다. id는 {@code 2026-11}. */
public record LeagueSeason(YearMonth month, ZoneId zone) {

	public static LeagueSeason containing(Instant at, ZoneId zone) {
		throw new UnsupportedOperationException();
	}

	public String id() {
		throw new UnsupportedOperationException();
	}

	public Instant startsAt() {
		throw new UnsupportedOperationException();
	}

	public Instant endsAt() {
		throw new UnsupportedOperationException();
	}
}
