package com.blinkerleague.server.badge.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.badge.application.service.GetBadgesService;
import com.blinkerleague.server.common.time.TimeConfig;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

	private final GetBadgesService getBadgesService;

	@GetMapping
	public List<BadgeResponse> badges(@CurrentDriver Long driverId) {
		return getBadgesService.get(driverId).stream().map(BadgeResponse::from).toList();
	}

	/** awardedAt은 받지 않았으면 null이다. */
	public record BadgeResponse(String code, String name, String condition, boolean earned,
		OffsetDateTime awardedAt) {

		static BadgeResponse from(GetBadgesService.BadgeView view) {
			return new BadgeResponse(view.code(), view.name(), view.condition(), view.earned(),
				view.awardedAt() == null ? null : view.awardedAt().atZone(TimeConfig.ZONE).toOffsetDateTime());
		}
	}
}
