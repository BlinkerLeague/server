package com.blinkerleague.server.point.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.point.application.service.GetPointsService;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

	private final GetPointsService getPointsService;

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
