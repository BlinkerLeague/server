package com.blinkerleague.server.report.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.report.application.service.GetWeeklyReportService;
import com.blinkerleague.server.report.application.service.WeeklyReportView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "리포트", description = "주간 리포트")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

	private final GetWeeklyReportService getWeeklyReportService;

	@Operation(summary = "주간 리포트 조회",
		description = "한 주(ISO 주, 월요일 시작, Asia/Seoul) 동안의 운전을 지난주와 비교한다(plan.md 4.5, SCR-07). "
			+ "metrics는 항목별 거리 가중 평균 점수(0~100)와 지난주 대비 변화, weakestMetric은 이번 주 가장 약한 항목이다. "
			+ "lastMission은 이 주에 수행한 맞춤 미션의 판정, nextMission은 가장 약한 항목으로 다음 주에 자동 등록한 맞춤 미션이다(수락 단계 없음, D7 B). "
			+ "매주 월요일 04:00에 미리 만들어 두고, 없으면 조회할 때 만든다. 한 번 만든 리포트는 저장본을 그대로 돌려준다.")
	@ApiResponse(responseCode = "200", description = "주간 리포트")
	@ApiResponse(responseCode = "400", description = "INVALID_INPUT: week 형식이 2026-W45가 아니거나 아직 끝나지 않은 주",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/weekly")
	public WeeklyReportView weekly(@CurrentDriver Long driverId, @Parameter(description = "ISO 주(예: 2026-W45). 없으면 지난주") @RequestParam(required = false) String week) {
		return getWeeklyReportService.get(driverId, week);
	}
}
