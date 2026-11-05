package com.blinkerleague.server.score.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.domain.TripEventType;

/**
 * 운행 점수 엔진. DB · 스프링에 의존하지 않는 순수 계산이다(plan.md 3.5).
 * 측정한 항목만 가중치를 재분배해 가중합을 총점으로 한다. P0은 깜빡이 항목만 측정한다.
 */
public class ScoreCalculator {

	public ScoreResult calculate(Set<ScoreItem> supported, List<DrivingEvent> events, Map<ScoreItem, Double> weights) {
		List<DrivingEvent> turns = events.stream().filter(event -> event.type() == TripEventType.TURN).toList();
		Map<ScoreItem, Double> values = new EnumMap<>(ScoreItem.class);
		if (supported.contains(ScoreItem.TURN_SIGNAL) && !turns.isEmpty()) {
			long signalOn = turns.stream().filter(DrivingEvent::signalOn).count();
			values.put(ScoreItem.TURN_SIGNAL, (double) signalOn / turns.size());
		}
		if (values.isEmpty()) {
			return new ScoreResult(null, List.of());
		}

		Map<ScoreItem, Double> applied = WeightRedistribution.redistribute(weights, values.keySet());
		List<MetricScore> metrics = values.entrySet().stream()
			.map(e -> new MetricScore(e.getKey(), e.getValue(), toScore(e.getValue()), applied.get(e.getKey())))
			.toList();
		double total = metrics.stream().mapToDouble(m -> m.value() * 100 * m.weight()).sum();
		return new ScoreResult((int) Math.round(total), metrics);
	}

	private static int toScore(double value) {
		return (int) Math.round(value * 100);
	}
}
