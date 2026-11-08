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

import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.application.service.UploadTripService;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.TripEventType;
import com.blinkerleague.server.trip.domain.TripStatus;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
	@ApiResponse(responseCode = "201", description = "새로 저장함. 최소 거리 미만이면 status=EXCLUDED_SHORT, 아니면 UPLOADED")
	@ApiResponse(responseCode = "200", description = "같은 tripId 재전송. 저장하지 않고 처음 저장한 결과를 돌려준다")
	@ApiResponse(responseCode = "400", description = "INVALID_INPUT: 필수 값 누락, 음수 거리, ID 36자 초과, 알 수 없는 type, endedAt <= startedAt, 운행 구간 밖 이벤트, 중복 eventId, supported 안의 null",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "CONFLICT: 다른 운전자가 이미 사용한 tripId",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<TripResult> upload(@CurrentDriver Long driverId,
		@Valid @RequestBody UploadTripRequest request) {
		UploadTripService.Result result = uploadTripService.upload(driverId, request.toCommand());
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(new TripResult(result.tripId(), result.status()));
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

	/** 점수 응답(plan.md 3.5)과 같은 구조. 점수 · 미션 필드는 feat/trip-score, feat/daily-mission에서 채운다. */
	public record TripResult(String tripId, TripStatus status) {
	}
}
