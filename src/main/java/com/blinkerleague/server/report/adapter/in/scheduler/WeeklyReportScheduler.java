package com.blinkerleague.server.report.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.report.application.service.GenerateLastWeekReportsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 매주 월요일 새벽에 지난주 주간 리포트를 미리 만든다(plan.md 4.5). */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

	private final GenerateLastWeekReportsService generateLastWeekReportsService;

	@Scheduled(cron = "0 0 4 * * MON", zone = "Asia/Seoul")
	public void run() {
		int generated = generateLastWeekReportsService.generate();
		log.info("지난주 주간 리포트 {}건을 만들었습니다.", generated);
	}
}
