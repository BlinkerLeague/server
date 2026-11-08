package com.blinkerleague.server.report.application.service;

import java.time.Clock;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.report.domain.ReportWeek;

import lombok.RequiredArgsConstructor;

/** 주간 리포트 조회(GET /api/reports/weekly). 저장본이 없으면 그 자리에서 만든다. */
@RequiredArgsConstructor
public class GetWeeklyReportService {

	private final GenerateWeeklyReportService generateWeeklyReportService;
	private final Clock clock;

	/** weekKey가 없으면 지난주. 끝나지 않은 주나 형식이 틀린 주는 INVALID_INPUT. */
	public WeeklyReportView get(Long driverId, String weekKey) {
		ReportWeek week = weekKey == null
			? ReportWeek.containing(clock.instant(), clock.getZone()).previous()
			: parse(weekKey);
		// 진행 중인 주는 아직 리포트를 만들 수 없다(만들면 그 주 미션 판정이 일찍 확정된다).
		if (week.end(clock.getZone()).isAfter(clock.instant())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "아직 끝나지 않은 주입니다: " + week.key());
		}
		return generateWeeklyReportService.generate(driverId, week);
	}

	private static ReportWeek parse(String weekKey) {
		try {
			return ReportWeek.parse(weekKey);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "주는 2026-W45 형식이어야 합니다.");
		}
	}
}
