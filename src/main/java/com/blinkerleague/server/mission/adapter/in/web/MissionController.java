package com.blinkerleague.server.mission.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.mission.application.service.GetTodayMissionService;
import com.blinkerleague.server.mission.application.service.GetTodayMissionService.TodayMission;
import com.blinkerleague.server.mission.domain.ConditionProgress;
import com.blinkerleague.server.mission.domain.MissionStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

	private final GetTodayMissionService getTodayMissionService;

	/** 홈(SCR-01) 진행 링과 미션 화면(SCR-03). */
	@GetMapping("/today")
	public TodayMissionResponse today(@CurrentDriver Long driverId) {
		return TodayMissionResponse.from(getTodayMissionService.today(driverId));
	}

	public record TodayMissionResponse(Long missionId, String title, String periodKey, MissionStatus status,
		int rewardPoint, Progress progress, List<ConditionProgress> conditions) {

		static TodayMissionResponse from(TodayMission today) {
			return new TodayMissionResponse(today.missionId(), today.title(), today.periodKey(), today.status(),
				today.rewardPoint(),
				new Progress(today.turns().turns(), today.turns().successTurns(), today.requiredTurns()),
				today.conditions());
		}
	}

	public record Progress(int turns, int successTurns, int requiredTurns) {
	}
}
