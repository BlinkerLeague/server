package com.blinkerleague.server.report.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.blinkerleague.server.report.application.port.out.LoadWeekDriversPort;
import com.blinkerleague.server.report.application.port.out.LoadWeekDrivingPort;
import com.blinkerleague.server.report.application.port.out.LoadWeeklyMissionTemplatesPort;
import com.blinkerleague.server.report.application.port.out.WeeklyMissionStore;
import com.blinkerleague.server.report.application.port.out.WeeklyReportStore;
import com.blinkerleague.server.report.application.service.GenerateLastWeekReportsService;
import com.blinkerleague.server.report.application.service.GenerateWeeklyReportService;
import com.blinkerleague.server.report.application.service.GetWeeklyReportService;

/** report 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
@EnableScheduling
public class ReportBeanConfig {

	@Bean
	public GenerateWeeklyReportService generateWeeklyReportService(LoadWeekDrivingPort loadWeekDrivingPort,
		WeeklyMissionStore weeklyMissionStore, WeeklyReportStore weeklyReportStore,
		LoadWeeklyMissionTemplatesPort templates, Clock clock) {
		return new GenerateWeeklyReportService(loadWeekDrivingPort, weeklyMissionStore, weeklyReportStore, templates,
			clock.getZone());
	}

	@Bean
	public GetWeeklyReportService getWeeklyReportService(GenerateWeeklyReportService generateWeeklyReportService,
		Clock clock) {
		return new GetWeeklyReportService(generateWeeklyReportService, clock);
	}

	@Bean
	public GenerateLastWeekReportsService generateLastWeekReportsService(LoadWeekDriversPort loadWeekDriversPort,
		GenerateWeeklyReportService generateWeeklyReportService, Clock clock) {
		return new GenerateLastWeekReportsService(loadWeekDriversPort, generateWeeklyReportService, clock);
	}
}
