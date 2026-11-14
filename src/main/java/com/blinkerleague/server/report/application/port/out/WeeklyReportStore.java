package com.blinkerleague.server.report.application.port.out;

import java.util.Optional;

/** 만든 주간 리포트를 JSON으로 저장한다. 조회는 저장본을 그대로 돌려준다(plan.md 4.5). */
public interface WeeklyReportStore {

	Optional<String> findReport(Long driverId, String weekKey);

	void saveReport(Long driverId, String weekKey, String reportJson);
}
