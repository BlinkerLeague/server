package com.blinkerleague.server.report.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.report.application.service.GetWeeklyReportService;
import com.blinkerleague.server.report.application.service.WeeklyReportView;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

	private final GetWeeklyReportService getWeeklyReportService;

	@GetMapping("/weekly")
	public WeeklyReportView weekly(@CurrentDriver Long driverId, @RequestParam(required = false) String week) {
		return getWeeklyReportService.get(driverId, week);
	}
}
