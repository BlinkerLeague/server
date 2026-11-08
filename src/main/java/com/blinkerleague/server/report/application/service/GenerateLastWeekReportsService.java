package com.blinkerleague.server.report.application.service;

import java.time.Clock;

import com.blinkerleague.server.report.application.port.out.LoadWeekDriversPort;

import lombok.RequiredArgsConstructor;

/** 매주 월요일 새벽 배치: 지난주에 운행한 운전자마다 지난주 리포트를 미리 만든다(plan.md 4.5). */
@RequiredArgsConstructor
public class GenerateLastWeekReportsService {

	private final LoadWeekDriversPort loadWeekDriversPort;
	private final GenerateWeeklyReportService generateWeeklyReportService;
	private final Clock clock;

	/** 만든 리포트 수. 한 운전자가 실패해도 나머지는 계속 만든다. */
	public int generate() {
		throw new UnsupportedOperationException();
	}
}
