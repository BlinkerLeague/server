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
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.score.domain.PlayResult;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.application.port.out.TripMissionPort;
import com.blinkerleague.server.trip.application.service.GetTripScoreService;
import com.blinkerleague.server.trip.application.service.ListRecentTripsService;
import com.blinkerleague.server.trip.application.service.TripScoreView;
import com.blinkerleague.server.trip.application.service.UploadTripService;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.TripEventType;
import com.blinkerleague.server.trip.domain.TripStatus;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Tag(name = "운행", description = "운행(라운드) · 이벤트 업로드")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

	private final UploadTripService uploadTripService;
	private final GetTripScoreService getTripScoreService;
	private final ListRecentTripsService listRecentTripsService;

	/** 새로 저장하면 201, 이미 업로드된 운행을 다시 받으면 저장하지 않고 처음 결과를 200으로 돌려준다. */
	@Operation(summary = "운행 · 이벤트 업로드",
		description = "시동을 켜고 끄기까지의 운행 1건과 운행 중 이벤트를 올린다. tripId · eventId는 앱이 만든 UUID이며, 같은 tripId를 다시 보내도 한 번만 저장한다(재전송 안전). 원시 위치 좌표는 받지 않는다. payload는 해석하지 않고 JSON 그대로 저장한다.")
	@ApiResponse(responseCode = "201", description = "새로 저장하고 점수를 매김. 최소 거리 미만이면 status=EXCLUDED_SHORT(점수 없음), 회전이 없으면 NO_TURN(score=null), 아니면 SCORED. 응답은 점수 조회와 같은 TripResult")
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
			.body(TripResult.from(result.trip()));
	}

	/** 최근 경기 목록(SCR-01). 종료 시각 최신순. */
	@Operation(summary = "최근 경기 목록",
		description = "내 운행을 종료 시각 최신순으로 돌려준다(SCR-01). limit은 기본 10, 최대 50이며 50을 넘으면 50건으로 자른다(D24). 점수가 없는 운행은 score · grade가 null이다.")
	@ApiResponse(responseCode = "200", description = "내 운행 목록(최신순)")
	@ApiResponse(responseCode = "400", description = "INVALID_INPUT: limit이 1 미만이거나 숫자가 아님",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public List<RecentTripResult> recent(@CurrentDriver Long driverId,
		@RequestParam(required = false) Integer limit) {
		return listRecentTripsService.list(driverId, limit).stream().map(RecentTripResult::from).toList();
	}

	@Operation(summary = "운행 점수 조회",
		description = "업로드 응답과 같은 TripResult(총점 · 등급 · 항목별 점수 · 회전 수 · 경기 요약 · 시각순 경기 기록). mission은 오늘의 미션 연결 전까지 null이다.")
	@ApiResponse(responseCode = "200", description = "운행 결과")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "NOT_FOUND: 없는 운행이거나 다른 운전자의 운행",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
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
		@Schema(implementation = Object.class, description = "이벤트 타입별 추가 정보(선택). 서버는 해석하지 않고 JSON 그대로 저장한다. "
			+ "TURN: { \"direction\": \"LEFT\" | \"RIGHT\", \"signalOn\": true | false } "
			+ "(깜빡이 점수는 signalOn으로 계산한다). 다른 타입의 형식은 아직 정해지지 않아 저장만 한다.",
			example = "{\"direction\": \"LEFT\", \"signalOn\": true}")
		JsonNode payload) {

		RecordedEvent toRecordedEvent() {
			return new RecordedEvent(eventId, type, occurredAt.toInstant(), payload == null ? null : payload.toString());
		}
	}

	/** 업로드 응답과 점수 조회가 같은 구조(plan.md 3.5). mission은 그날 운영 중인 미션이 없으면 null이다. */
	public record TripResult(String tripId, TripStatus status, Integer score, String grade,
		List<MetricResult> metrics, TurnsResult turns, SummaryResult summary, List<PlayItem> plays,
		MissionResult mission) {

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
				plays, MissionResult.from(view.mission()));
		}
	}

	/** 이 운행을 반영한 그날 미션 판정. status는 IN_PROGRESS 또는 ACHIEVED. */
	public record MissionResult(Long missionId, String status) {

		static MissionResult from(TripMissionPort.TripMission mission) {
			return mission == null ? null : new MissionResult(mission.missionId(), mission.status());
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
