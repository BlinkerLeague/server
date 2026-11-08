package com.blinkerleague.server.report.adapter.in.scheduler;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.report.application.service.GenerateLastWeekReportsService;

import lombok.RequiredArgsConstructor;

/** 매주 월요일 새벽에 지난주 주간 리포트를 미리 만든다(plan.md 4.5). */
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

	private final GenerateLastWeekReportsService generateLastWeekReportsService;

	public void run() {
		throw new UnsupportedOperationException();
	}
}
