package com.blinkerleague.server.report.application.service;

import java.time.ZoneId;

import com.blinkerleague.server.report.application.port.out.LoadWeekDrivingPort;
import com.blinkerleague.server.report.application.port.out.LoadWeeklyMissionTemplatesPort;
import com.blinkerleague.server.report.application.port.out.WeeklyMissionStore;
import com.blinkerleague.server.report.application.port.out.WeeklyReportStore;
import com.blinkerleague.server.report.domain.ReportWeek;

import lombok.RequiredArgsConstructor;

/** 운전자의 한 주 리포트를 만들어 저장한다(plan.md 4.5). 이미 있으면 저장본을 돌려준다. */
@RequiredArgsConstructor
public class GenerateWeeklyReportService {

	private final LoadWeekDrivingPort loadWeekDrivingPort;
	private final WeeklyMissionStore weeklyMissionStore;
	private final WeeklyReportStore weeklyReportStore;
	private final LoadWeeklyMissionTemplatesPort templates;
	private final ZoneId zone;

	public WeeklyReportView generate(Long driverId, ReportWeek week) {
		throw new UnsupportedOperationException();
	}
}
