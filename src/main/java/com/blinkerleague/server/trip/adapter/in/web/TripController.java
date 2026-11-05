package com.blinkerleague.server.trip.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.threshold.domain.ScoreItem;
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

	/** 새로 저장하면 201, 이미 업로드된 운행을 다시 받으면 저장하지 않고 처음 결과를 200으로 돌려준다. */
	@PostMapping
	public ResponseEntity<TripResult> upload(@CurrentDriver Long driverId,
		@Valid @RequestBody UploadTripRequest request) {
		UploadTripService.Result result = uploadTripService.upload(driverId, request.toCommand());
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(TripResult.from(result.trip()));
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

	/** 업로드 응답과 점수 조회가 같은 구조(plan.md 3.5). 미션 필드는 feat/daily-mission에서 채운다. */
	public record TripResult(String tripId, TripStatus status, Integer score, String grade,
		List<MetricResult> metrics) {

		static TripResult from(TripScoreView view) {
			List<MetricResult> metrics = view.metrics().stream()
				.map(m -> new MetricResult(m.metric(), m.value(), m.score(), m.weight()))
				.toList();
			return new TripResult(view.tripId(), view.status(), view.score(), view.grade(), metrics);
		}
	}

	public record MetricResult(ScoreItem metric, double value, int score, double weight) {
	}
}
