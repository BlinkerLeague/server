package com.blinkerleague.server.report.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 주간 리포트의 한 주(ISO 주, 월요일 시작, Asia/Seoul). key는 2026-W45 형식이다. */
public record ReportWeek(int year, int week) {

	private static final Pattern KEY = Pattern.compile("(\\d{4})-W(\\d{2})");

	public ReportWeek {
		LocalDate probe = LocalDate.of(year, 6, 1);
		if (week < 1 || week > probe.range(IsoFields.WEEK_OF_WEEK_BASED_YEAR).getMaximum()) {
			throw new IllegalArgumentException("없는 주입니다: " + year + "-W" + week);
		}
	}

	/** 형식이 맞지 않거나 없는 주면 IllegalArgumentException. */
	public static ReportWeek parse(String key) {
		Matcher matcher = KEY.matcher(key == null ? "" : key);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("주는 2026-W45 형식이어야 합니다: " + key);
		}
		return new ReportWeek(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
	}

	public static ReportWeek containing(Instant instant, ZoneId zone) {
		return of(LocalDate.ofInstant(instant, zone));
	}

	private static ReportWeek of(LocalDate date) {
		return new ReportWeek(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
	}

	public String key() {
		return String.format("%04d-W%02d", year, week);
	}

	/** 이 주 월요일 00:00(포함)부터 다음 주 월요일 00:00(제외)까지. */
	public Instant start(ZoneId zone) {
		return monday().atStartOfDay(zone).toInstant();
	}

	public Instant end(ZoneId zone) {
		return monday().plusWeeks(1).atStartOfDay(zone).toInstant();
	}

	public ReportWeek previous() {
		return of(monday().minusWeeks(1));
	}

	public ReportWeek next() {
		return of(monday().plusWeeks(1));
	}

	private LocalDate monday() {
		return LocalDate.of(year, 6, 1)
			.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
			.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}
}
