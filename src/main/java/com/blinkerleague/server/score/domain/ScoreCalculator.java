package com.blinkerleague.server.score.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		Map<ScoreItem, Outcome> outcomes = new EnumMap<>(ScoreItem.class);
		for (ScoreItem item : ScoreItem.values()) {
			outcomes.put(item, facts.supported().contains(item)
				? measure(item, facts, thresholds)
				: Outcome.excluded(MetricStatus.UNSUPPORTED, "SIGNAL_NOT_SUPPORTED"));
		}
		List<ScoreItem> measured = outcomes.entrySet().stream()
			.filter(e -> e.getValue().measured()).map(Map.Entry::getKey).toList();
		if (measured.isEmpty()) {
			return new ScoreResult(null, metrics(outcomes, Map.of()));
		}
		Map<ScoreItem, Double> applied = WeightRedistribution.redistribute(thresholds.weights(), Set.copyOf(measured));
		List<MetricScore> metrics = metrics(outcomes, applied);
		double total = metrics.stream().filter(m -> m.status() == MetricStatus.SCORED)
			.mapToDouble(m -> m.value() * 100 * m.weight()).sum();
		return new ScoreResult((int) Math.round(total), metrics);
	}

	private static Outcome measure(ScoreItem item, TripFacts facts, Thresholds thresholds) {
		return switch (item) {
			case TURN_SIGNAL -> turnSignal(facts);
			case SMOOTH_DRIVING -> smoothDriving(facts, thresholds.harsh());
			case SAFETY_WARNING -> safetyWarning(facts, thresholds.safetyWarning());
			default -> Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_DATA");
		};
	}

	private static List<MetricScore> metrics(Map<ScoreItem, Outcome> outcomes, Map<ScoreItem, Double> applied) {
		List<MetricScore> metrics = new ArrayList<>();
		outcomes.forEach((item, o) -> metrics.add(o.measured()
			? MetricScore.scored(item, o.value(), toScore(o.value()), applied.get(item), o.detail())
			: MetricScore.excluded(item, o.status(), o.reason())));
		return metrics;
	}

	// 깜빡이를 켠 회전 비율. 회전이 없으면 이 항목만 빼고 나머지로 평점한다(plan.md 4.1).
	private static Outcome turnSignal(TripFacts facts) {
		List<DrivingEvent> turns = ofType(facts, TripEventType.TURN);
		if (turns.isEmpty()) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_TURN");
		}
		long signalOn = turns.stream().filter(DrivingEvent::signalOn).count();
		return Outcome.of((double) signalOn / turns.size(), Map.of());
	}

	// max(0, 100 − penaltyPer10Km × 10km당 (급가속 + 급감속)) (plan.md 4.1)
	private static Outcome smoothDriving(TripFacts facts, Thresholds.Harsh rule) {
		int count = ofType(facts, TripEventType.HARSH_ACCEL).size() + ofType(facts, TripEventType.HARSH_BRAKE).size();
		double per10Km = count / facts.distanceKm() * 10;
		double value = Math.max(0, 1 - rule.penaltyPer10Km() * per10Km / 100);
		return Outcome.of(value, Map.of("count", count, "per10Km", oneDecimal(per10Km)));
	}

	// 같은 경고가 mergeWindowSec 안에 다시 오면 1회로 센다. max(0, 100 − penaltyPer100Km × 100km당 횟수) (plan.md 4.1)
	private static Outcome safetyWarning(TripFacts facts, Thresholds.SafetyWarning rule) {
		List<Instant> times = ofType(facts, TripEventType.SAFETY_WARNING).stream()
			.map(DrivingEvent::occurredAt).sorted().toList();
		int count = 0;
		Instant lastCounted = null;
		for (Instant at : times) {
			if (lastCounted == null || Duration.between(lastCounted, at).getSeconds() > rule.mergeWindowSec()) {
				count++;
				lastCounted = at;
			}
		}
		double per100Km = count / facts.distanceKm() * 100;
		double value = Math.max(0, 1 - rule.penaltyPer100Km() * per100Km / 100);
		return Outcome.of(value, Map.of("count", count, "per100Km", oneDecimal(per100Km)));
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

	/** 항목 하나의 측정 결과. 측정했으면 value · detail, 제외면 status · reason. */
	private record Outcome(Double value, Map<String, Number> detail, MetricStatus status, String reason) {

		static Outcome of(double value, Map<String, Number> detail) {
			return new Outcome(value, detail, MetricStatus.SCORED, null);
		}

		static Outcome excluded(MetricStatus status, String reason) {
			return new Outcome(null, Map.of(), status, reason);
		}

		boolean measured() {
			return status == MetricStatus.SCORED;
		}
	}
}
