package com.blinkerleague.server.threshold.adapter.in.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ThresholdController {

	private final ThresholdProvider thresholdProvider;

	/** 앱이 시작할 때 내려받는다. 앱은 version이 바뀐 경우에만 갱신한다. */
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
		Map<ScoreItem, Double> weights,
		Thresholds.Point point,
		List<Thresholds.GradeRule> grades,
		Map<ScoreItem, String> tips) {

		static ThresholdsResponse from(VersionedThresholds current) {
			Thresholds t = current.thresholds();
			return new ThresholdsResponse(current.version(), t.trip(), t.turn(), t.harsh(), t.safetyWarning(),
				t.weights(), t.point(), t.grades(), t.tips());
		}
	}
}
