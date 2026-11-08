package com.blinkerleague.server.threshold;

import java.util.List;
import java.util.Map;

import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.Thresholds.GradeRule;

/** 테스트용 기준값. plan.md 3.3 예시 값을 쓴다. */
public final class ThresholdsFixture {

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
			new Thresholds.Trip(3.0, 3),
			new Thresholds.Turn(5, 10, 60, 150, 5),
			new Thresholds.Harsh(10),
			new Thresholds.SafetyWarning(10),
			Map.of(ScoreItem.TURN_SIGNAL, 0.4, ScoreItem.SMOOTH_DRIVING, 0.3,
				ScoreItem.SAFETY_WARNING, 0.2, ScoreItem.NIGHT_HEADLIGHT, 0.1),
			new Thresholds.Point(50, 150, 7, 50),
			grades,
			Map.of(ScoreItem.TURN_SIGNAL, "회전 5초 전에 깜빡이를 켜 보세요.",
				ScoreItem.SMOOTH_DRIVING, "천천히 출발하고 미리 감속해 보세요.",
				ScoreItem.SAFETY_WARNING, "앞차와 거리를 넉넉히 두세요.",
				ScoreItem.NIGHT_HEADLIGHT, "해가 지면 전조등을 켜 주세요."));
	}

	/** 같은 구조에서 운행 최소 거리만 바꾼 값. 변경 테스트용. */
	public static Thresholds withMinDistanceKm(double minDistanceKm) {
		Thresholds base = planExample();
		return new Thresholds(new Thresholds.Trip(minDistanceKm, base.trip().parkEndMinutes()), base.turn(),
			base.harsh(), base.safetyWarning(), base.weights(), base.point(), base.grades(), base.tips());
	}
}
