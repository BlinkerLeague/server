package com.blinkerleague.server.threshold.adapter.in.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "설정", description = "판정 기준값 · 점수 가중치 · 등급")
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ThresholdController {

	private final ThresholdProvider thresholdProvider;

	/** 앱이 시작할 때 내려받는다. 앱은 version이 바뀐 경우에만 갱신한다. */
	@Operation(summary = "판정 기준값 조회",
		description = "회전 · 급가감속 판정 기준, 점수 가중치, 포인트 규칙, 등급 기준, 항목별 팁 문구. 앱은 시작할 때 내려받고 version이 바뀐 경우에만 갱신한다.")
	@SecurityRequirements // 공개 API: 인증 없음
	@ApiResponse(responseCode = "200", description = "현재 기준값과 version")
	@GetMapping("/thresholds")
	public ThresholdsResponse thresholds() {
		return ThresholdsResponse.from(thresholdProvider.current());
	}

	public record ThresholdsResponse(
		long version,
		Thresholds.Trip trip,
		Thresholds.Turn turn,
		Thresholds.Harsh harsh,
		Thresholds.SafetyWarning safetyWarning,
		Thresholds.SignalCancel signalCancel,
		Thresholds.SeatBelt seatBelt,
		Thresholds.Rain rain,
		Map<ScoreItem, Double> weights,
		Map<ScoreItem, Integer> minSamples,
		Thresholds.Point point,
		List<Thresholds.GradeRule> grades,
		Map<ScoreItem, String> tips) {

		static ThresholdsResponse from(VersionedThresholds current) {
			Thresholds t = current.thresholds();
			return new ThresholdsResponse(current.version(), t.trip(), t.turn(), t.harsh(), t.safetyWarning(),
				t.signalCancel(), t.seatBelt(), t.rain(), t.weights(), t.minSamples(), t.point(), t.grades(), t.tips());
		}
	}
}
