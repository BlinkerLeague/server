package com.blinkerleague.server.point.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.point.application.service.GetPointsService;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "포인트", description = "포인트 잔액 · 적립 원장")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

	private final GetPointsService getPointsService;

	@Operation(summary = "내 포인트 조회",
		description = "잔액(전체 적립 합계), 오늘(Asia/Seoul) 적립 합계와 일일 상한, 최근 원장 20건(최근 것부터). 운행 적립은 적립 당시의 거리 · 점수를 description에 담는다.")
	@ApiResponse(responseCode = "200", description = "잔액 · 오늘 적립 · 일일 상한 · 최근 원장")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public PointsResponse points(@CurrentDriver Long driverId) {
		return PointsResponse.from(getPointsService.get(driverId));
	}

	public record PointsResponse(int balance, int todayEarned, int dailyCap, List<EntryResponse> entries) {

		static PointsResponse from(GetPointsService.Points points) {
			return new PointsResponse(points.balance(), points.todayEarned(), points.dailyCap(),
				points.entries().stream().map(EntryResponse::from).toList());
		}
	}

	/** description은 적립 당시의 거리 · 점수(TRIP). 다른 사유는 null. */
	public record EntryResponse(int amount, PointReason reason, Description description, OffsetDateTime createdAt) {

		static EntryResponse from(PointEntry entry) {
			Description description = entry.getReason() == PointReason.TRIP
				? new Description(entry.getDistanceKm(), entry.getScore())
				: null;
			return new EntryResponse(entry.getAmount(), entry.getReason(), description,
				entry.getEarnedAt().atZone(TimeConfig.ZONE).toOffsetDateTime());
		}
	}

	public record Description(Double distanceKm, Integer score) {
	}
}
