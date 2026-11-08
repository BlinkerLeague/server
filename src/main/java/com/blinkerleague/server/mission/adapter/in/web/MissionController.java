package com.blinkerleague.server.mission.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.mission.application.service.GetTodayMissionService;
import com.blinkerleague.server.mission.application.service.GetTodayMissionService.TodayMission;
import com.blinkerleague.server.mission.domain.ConditionProgress;
import com.blinkerleague.server.mission.domain.MissionStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "미션", description = "오늘의 미션")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

	private final GetTodayMissionService getTodayMissionService;

	/** 홈(SCR-01) 진행 링과 미션 화면(SCR-03). */
	@Operation(summary = "오늘의 미션 조회",
		description = "오늘(Asia/Seoul) 운영 중인 일일 미션과 내 진행도. 운행 1건이 1라운드이고, 그날 라운드 중 하나라도 조건을 채우면 ACHIEVED로 확정된다. "
			+ "progress는 달성했으면 달성한 라운드, 아니면 가장 최근 라운드의 회전 수다. 최소 거리 미만 운행은 세지 않는다. "
			+ "그날이 끝날 때까지 달성하지 못하면 FAILED이고, 오늘 첫 운행 전에는 마지막 미션 날의 최종 상태(ACHIEVED · FAILED)를 그 날의 periodKey로 보여 준다(D25).")
	@ApiResponse(responseCode = "200", description = "오늘의 미션과 진행도(status: IN_PROGRESS · ACHIEVED · FAILED). 오늘 운행 전이면 마지막 미션 날의 최종 상태")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "NOT_FOUND: 오늘 운영 중인 미션이 없음",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/today")
	public TodayMissionResponse today(@CurrentDriver Long driverId) {
		return TodayMissionResponse.from(getTodayMissionService.today(driverId));
	}

	public record TodayMissionResponse(Long missionId, String title, String periodKey, MissionStatus status,
		int rewardPoint, Progress progress, List<ConditionProgress> conditions) {

		static TodayMissionResponse from(TodayMission today) {
			return new TodayMissionResponse(today.missionId(), today.title(), today.periodKey(), today.status(),
				today.rewardPoint(),
				new Progress(today.shownRound().turns(), today.shownRound().successTurns(), today.requiredTurns()),
				today.conditions());
		}
	}

	public record Progress(int turns, int successTurns, int requiredTurns) {
	}
}
