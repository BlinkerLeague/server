package com.blinkerleague.server.mission.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.mission.application.service.GetStreakService;
import com.blinkerleague.server.mission.application.service.GetStreakService.StreakView;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

	private final GetStreakService getStreakService;

	@GetMapping
	public StreakView streak(@CurrentDriver Long driverId) {
		return getStreakService.get(driverId);
	}
}
