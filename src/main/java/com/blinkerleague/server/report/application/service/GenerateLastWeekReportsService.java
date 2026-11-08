package com.blinkerleague.server.report.application.service;

import java.time.Clock;

import com.blinkerleague.server.report.application.port.out.LoadWeekDriversPort;
import com.blinkerleague.server.report.domain.ReportWeek;

import lombok.RequiredArgsConstructor;

/** 매주 월요일 새벽 배치: 지난주에 운행한 운전자마다 지난주 리포트를 미리 만든다(plan.md 4.5). */
@RequiredArgsConstructor
public class GenerateLastWeekReportsService {

	private final LoadWeekDriversPort loadWeekDriversPort;
	private final GenerateWeeklyReportService generateWeeklyReportService;
	private final Clock clock;

	/** 만든 리포트 수. 한 운전자가 실패해도 나머지는 계속 만든다. */
	public int generate() {
		ReportWeek lastWeek = ReportWeek.containing(clock.instant(), clock.getZone()).previous();
		int generated = 0;
		for (Long driverId : loadWeekDriversPort.driversWithTrips(lastWeek.start(clock.getZone()),
			lastWeek.end(clock.getZone()))) {
			try {
				generateWeeklyReportService.generate(driverId, lastWeek);
				generated++;
			} catch (RuntimeException e) {
				// 만들지 못한 운전자는 조회할 때 다시 만든다(GetWeeklyReportService). 나머지 운전자는 계속 만든다.
			}
		}
		return generated;
	}
}
