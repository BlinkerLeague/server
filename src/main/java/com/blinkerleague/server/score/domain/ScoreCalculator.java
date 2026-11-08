package com.blinkerleague.server.score.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.trip.domain.TripEventType;

/**
 * 운행 점수 엔진. DB · 스프링에 의존하지 않는 순수 계산이다(plan.md 3.5 · 4.1).
 * 항목마다 측정값(0~1)을 구하고, 측정한 항목만 가중치를 재분배해 가중합을 총점으로 한다.
 * 기준값(감점 폭 등)은 모두 {@link Thresholds}에서 읽는다.
 */
public class ScoreCalculator {

	public ScoreResult calculate(TripFacts facts, Thresholds thresholds) {
		Map<ScoreItem, Measured> measured = new EnumMap<>(ScoreItem.class);
		if (facts.supported().contains(ScoreItem.TURN_SIGNAL)) {
			turnSignal(facts).ifPresent(m -> measured.put(ScoreItem.TURN_SIGNAL, m));
		}
		if (facts.supported().contains(ScoreItem.SMOOTH_DRIVING)) {
			measured.put(ScoreItem.SMOOTH_DRIVING, smoothDriving(facts, thresholds.harsh()));
		}
		if (measured.isEmpty()) {
			return new ScoreResult(null, List.of());
		}

		Map<ScoreItem, Double> applied = WeightRedistribution.redistribute(thresholds.weights(), measured.keySet());
		List<MetricScore> metrics = new ArrayList<>();
		measured.forEach((item, m) -> metrics.add(
			MetricScore.scored(item, m.value(), toScore(m.value()), applied.get(item), m.detail())));
		double total = metrics.stream().mapToDouble(m -> m.value() * 100 * m.weight()).sum();
		return new ScoreResult((int) Math.round(total), metrics);
	}

	// 깜빡이를 켠 회전 비율. 회전이 없으면 측정하지 않는다.
	private static Optional<Measured> turnSignal(TripFacts facts) {
		List<DrivingEvent> turns = ofType(facts, TripEventType.TURN);
		if (turns.isEmpty()) {
			return Optional.empty();
		}
		long signalOn = turns.stream().filter(DrivingEvent::signalOn).count();
		return Optional.of(new Measured((double) signalOn / turns.size(), Map.of()));
	}

	// max(0, 100 − penaltyPer10Km × 10km당 (급가속 + 급감속)) (plan.md 4.1)
	private static Measured smoothDriving(TripFacts facts, Thresholds.Harsh rule) {
		int count = ofType(facts, TripEventType.HARSH_ACCEL).size() + ofType(facts, TripEventType.HARSH_BRAKE).size();
		double per10Km = count / facts.distanceKm() * 10;
		double value = Math.max(0, 1 - rule.penaltyPer10Km() * per10Km / 100);
		return new Measured(value, Map.of("count", count, "per10Km", oneDecimal(per10Km)));
	}

	private static List<DrivingEvent> ofType(TripFacts facts, TripEventType type) {
		return facts.events().stream().filter(event -> event.type() == type).toList();
	}

	private static double oneDecimal(double value) {
		return Math.round(value * 10) / 10.0;
	}

	private static int toScore(double value) {
		return (int) Math.round(value * 100);
	}

	private record Measured(double value, Map<String, Number> detail) {
	}
}
