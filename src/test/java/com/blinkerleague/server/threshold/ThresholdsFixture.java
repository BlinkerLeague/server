package com.blinkerleague.server.threshold;

import java.util.List;
import java.util.Map;

import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.Thresholds.GradeRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** 테스트용 기준값. plan.md 3.3 예시 값을 쓴다. */
public final class ThresholdsFixture {

	private static final ObjectMapper JSON = new ObjectMapper();

	public static final Thresholds.League LEAGUE = new Thresholds.League(
		List.of(ScoreItem.TURN_SIGNAL, ScoreItem.SMOOTH_DRIVING, ScoreItem.TURN_SPEED), 10, 50, 10, 10);

	private ThresholdsFixture() {
	}

	public static Thresholds planExample() {
		return withGrades(List.of(
			new GradeRule("S", 95),
			new GradeRule("A", 85),
			new GradeRule("B", 70),
			new GradeRule("C", 50)));
	}

	public static Thresholds withGrades(List<GradeRule> grades) {
		return new Thresholds(
			new Thresholds.Trip(3.0, 3, 2000, 5, 7),
			new Thresholds.Turn(5, 10, 60, 150, 5, 0.5, 30, 30, 90, 20, 3, false),
			new Thresholds.Harsh(10, 10),
			new Thresholds.SafetyWarning(10, 20),
			new Thresholds.SignalCancel(20, 30, 25),
			new Thresholds.SeatBelt(50, 10),
			new Thresholds.Rain(30, 0.8),
			Map.of(ScoreItem.TURN_SIGNAL, 0.4, ScoreItem.SMOOTH_DRIVING, 0.3,
				ScoreItem.SAFETY_WARNING, 0.2, ScoreItem.NIGHT_HEADLIGHT, 0.1),
			Map.of(ScoreItem.TURN_SIGNAL, 1),
			new Thresholds.Point(50, 150, 7, 50),
			LEAGUE,
			grades,
			Map.of(ScoreItem.TURN_SIGNAL, "회전 5초 전에 깜빡이를 켜 보세요.",
				ScoreItem.SMOOTH_DRIVING, "천천히 출발하고 미리 감속해 보세요.",
				ScoreItem.SAFETY_WARNING, "앞차와 거리를 넉넉히 두세요.",
				ScoreItem.NIGHT_HEADLIGHT, "해가 지면 전조등을 켜 주세요."));
	}

	/** 같은 구조에서 운행 최소 거리만 바꾼 값. 변경 테스트용. */
	public static Thresholds withMinDistanceKm(double minDistanceKm) {
		Thresholds base = planExample();
		return new Thresholds(new Thresholds.Trip(minDistanceKm, base.trip().parkEndMinutes(), base.trip().maxEvents(), base.trip().maxFutureMinutes(), base.trip().maxPastDays()), base.turn(),
			base.harsh(), base.safetyWarning(), base.signalCancel(), base.seatBelt(), base.rain(), base.weights(), base.minSamples(), base.point(), base.league(), base.grades(), base.tips());
	}

	/**
	 * plan.md 예시 값에서 점으로 구분한 경로(예: {@code "turn.lateSignalScore"}) 하나만 바꾼 값. 검증 테스트용.
	 * value가 null이면 그 값을 비운다(JSON에서 그룹이 빠진 경우).
	 */
	public static Thresholds planExampleWith(String path, Object value) {
		ObjectNode root = JSON.valueToTree(planExample());
		String[] keys = path.split("\\.");
		ObjectNode parent = root;
		for (int i = 0; i < keys.length - 1; i++) {
			parent = (ObjectNode) parent.get(keys[i]);
		}
		parent.set(keys[keys.length - 1], JSON.valueToTree(value));
		try {
			return JSON.treeToValue(root, Thresholds.class);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(path, e);
		}
	}

	/** 같은 구조에서 항목별 최소 표본만 바꾼 값. */
	public static Thresholds withMinSamples(Map<ScoreItem, Integer> minSamples) {
		Thresholds base = planExample();
		return new Thresholds(base.trip(), base.turn(), base.harsh(), base.safetyWarning(), base.signalCancel(), base.seatBelt(), base.rain(), base.weights(), minSamples,
			base.point(), base.league(), base.grades(), base.tips());
	}
}
