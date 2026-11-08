package com.blinkerleague.server.report.domain;

import java.time.Instant;
import java.time.ZoneId;

/** 주간 리포트의 한 주(ISO 주, 월요일 시작, Asia/Seoul). key는 2026-W45 형식이다. */
public record ReportWeek(int year, int week) {

	public static ReportWeek parse(String key) {
		throw new UnsupportedOperationException();
	}

	public static ReportWeek containing(Instant instant, ZoneId zone) {
		throw new UnsupportedOperationException();
	}

	public String key() {
		throw new UnsupportedOperationException();
	}

	/** 이 주 월요일 00:00(포함)부터 다음 주 월요일 00:00(제외)까지. */
	public Instant start(ZoneId zone) {
		throw new UnsupportedOperationException();
	}

	public Instant end(ZoneId zone) {
		throw new UnsupportedOperationException();
	}

	public ReportWeek previous() {
		throw new UnsupportedOperationException();
	}

	public ReportWeek next() {
		throw new UnsupportedOperationException();
	}
}
