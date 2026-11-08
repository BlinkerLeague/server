package com.blinkerleague.server.badge.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.badge.application.service.GetBadgesService;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.common.time.TimeConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "배지", description = "배지 진열장")
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

	private final GetBadgesService getBadgesService;

	@Operation(summary = "배지 진열장 조회",
		description = "전체 배지 카탈로그를 진열 순서로 돌려주고, 받은 배지는 earned=true와 받은 시각을 담는다(plan.md 4.3, SCR-05). "
			+ "배지는 운행을 올릴 때 누적 기록(점수가 난 운행 수, 미션 최장 연속 달성 일수, 깜빡이를 켠 회전 수)으로 판정한다.")
	@ApiResponse(responseCode = "200", description = "전체 배지(code, name, condition, earned, awardedAt)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
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
