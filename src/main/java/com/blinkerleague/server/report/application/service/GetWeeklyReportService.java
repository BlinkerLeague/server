package com.blinkerleague.server.report.application.service;

import java.time.Clock;

import lombok.RequiredArgsConstructor;

/** 주간 리포트 조회(GET /api/reports/weekly). 저장본이 없으면 그 자리에서 만든다. */
@RequiredArgsConstructor
public class GetWeeklyReportService {

	private final GenerateWeeklyReportService generateWeeklyReportService;
	private final Clock clock;

	/** weekKey가 없으면 지난주. 끝나지 않은 주나 형식이 틀린 주는 INVALID_INPUT. */
	public WeeklyReportView get(Long driverId, String weekKey) {
		throw new UnsupportedOperationException();
	}
}
