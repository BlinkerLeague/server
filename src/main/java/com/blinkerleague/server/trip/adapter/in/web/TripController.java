package com.blinkerleague.server.trip.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.score.domain.PlayResult;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.application.service.GetTripScoreService;
import com.blinkerleague.server.trip.application.service.ListRecentTripsService;
import com.blinkerleague.server.trip.application.service.TripScoreView;
import com.blinkerleague.server.trip.application.service.UploadTripService;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.TripEventType;
import com.blinkerleague.server.trip.domain.TripStatus;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

	private final UploadTripService uploadTripService;
	private final GetTripScoreService getTripScoreService;
	private final ListRecentTripsService listRecentTripsService;

	/** 새로 저장하면 201, 이미 업로드된 운행을 다시 받으면 저장하지 않고 처음 결과를 200으로 돌려준다. */
	@PostMapping
	public ResponseEntity<TripResult> upload(@CurrentDriver Long driverId,
		@Valid @RequestBody UploadTripRequest request) {
		UploadTripService.Result result = uploadTripService.upload(driverId, request.toCommand());
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(TripResult.from(result.trip()));
	}

	/** 최근 경기 목록(SCR-01). 종료 시각 최신순. */
	@GetMapping
	public List<RecentTripResult> recent(@CurrentDriver Long driverId,
		@RequestParam(required = false) Integer limit) {
		return listRecentTripsService.list(driverId, limit).stream().map(RecentTripResult::from).toList();
	}

	@GetMapping("/{tripId}/score")
	public TripResult score(@CurrentDriver Long driverId, @PathVariable String tripId) {
		return TripResult.from(getTripScoreService.get(driverId, tripId));
	}

	/** 원시 위치 좌표 필드는 두지 않는다(개인정보 요구사항). */
	public record UploadTripRequest(
		@NotBlank @Size(max = 36) String tripId,
		@NotNull OffsetDateTime startedAt,
		@NotNull OffsetDateTime endedAt,
		@NotNull @PositiveOrZero Double distanceKm,
		@NotNull Set<@NotNull ScoreItem> supported,
		@NotNull List<@Valid @NotNull EventRequest> events) {

		UploadTripService.Command toCommand() {
			List<RecordedEvent> recorded = events.stream().map(EventRequest::toRecordedEvent).toList();
			return new UploadTripService.Command(tripId, startedAt.toInstant(), endedAt.toInstant(), distanceKm,
				supported, recorded);
		}
	}

	/** payload는 해석하지 않고 JSON 문자열 그대로 저장한다. */
	public record EventRequest(
		@NotBlank @Size(max = 36) String eventId,
		@NotNull TripEventType type,
		@NotNull OffsetDateTime occurredAt,
		JsonNode payload) {

		RecordedEvent toRecordedEvent() {
			return new RecordedEvent(eventId, type, occurredAt.toInstant(), payload == null ? null : payload.toString());
		}
	}

	/** 업로드 응답과 점수 조회가 같은 구조(plan.md 3.5). mission은 feat/daily-mission 전까지 null이다. */
	public record TripResult(String tripId, TripStatus status, Integer score, String grade,
		List<MetricResult> metrics, TurnsResult turns, SummaryResult summary, List<PlayItem> plays,
		Object mission) {

		static TripResult from(TripScoreView view) {
			List<MetricResult> metrics = view.metrics().stream()
				.map(m -> new MetricResult(m.metric(), m.value(), m.score(), m.weight()))
				.toList();
			List<PlayItem> plays = view.plays().stream()
				.map(p -> new PlayItem(OffsetDateTime.ofInstant(p.occurredAt(), TimeConfig.ZONE), p.type(),
					p.result()))
				.toList();
			return new TripResult(view.tripId(), view.status(), view.score(), view.grade(), metrics,
				new TurnsResult(view.turns().total(), view.turns().success()),
				new SummaryResult(view.summary().goals(), view.summary().conceded(), view.summary().fouls()),
				plays, null);
		}
	}

	public record RecentTripResult(String tripId, OffsetDateTime endedAt, double distanceKm, TripStatus status,
		Integer score, String grade) {

		static RecentTripResult from(ListRecentTripsService.RecentTrip trip) {
			return new RecentTripResult(trip.tripId(), OffsetDateTime.ofInstant(trip.endedAt(), TimeConfig.ZONE),
				trip.distanceKm(), trip.status(), trip.score(), trip.grade());
		}
	}

	public record MetricResult(ScoreItem metric, double value, int score, double weight) {
	}

	public record TurnsResult(int total, int success) {
	}

	/** 득점 = 깜빡이를 켠 회전, 실점 = 켜지 않은 회전, 반칙 = 급가감속 · 안전 경고(P0은 0, D22). */
	public record SummaryResult(int goals, int conceded, int fouls) {
	}

	/** TURN이 아닌 이벤트는 result가 null이다(D22). */
	public record PlayItem(OffsetDateTime occurredAt, TripEventType type, PlayResult result) {
	}
}
