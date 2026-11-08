package com.blinkerleague.server.mission.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.mission.application.service.GetStreakService;
import com.blinkerleague.server.mission.application.service.GetStreakService.StreakView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "미션", description = "오늘의 미션")
@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

	private final GetStreakService getStreakService;

	@Operation(summary = "연속 달성 조회",
		description = "일일 미션을 연속으로 달성한 일수(plan.md 4.3). 오늘 달성했으면 오늘부터, 아직이면 어제부터 거꾸로 센다(오늘 진행 중인 것은 끊김이 아님). 날짜는 Asia/Seoul 기준.")
	@ApiResponse(responseCode = "200", description = "streakDays(연속 달성 일수), todayAchieved(오늘 달성 여부)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public StreakView streak(@CurrentDriver Long driverId) {
		return getStreakService.get(driverId);
	}
}
