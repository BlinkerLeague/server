package com.blinkerleague.server.score.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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
			case TURN_SIGNAL -> turnSignal(facts, thresholds.turn(), minSamples(thresholds, ScoreItem.TURN_SIGNAL));
			case SMOOTH_DRIVING -> smoothDriving(facts, thresholds.harsh());
			case SAFETY_WARNING -> safetyWarning(facts, thresholds.safetyWarning());
			case NIGHT_HEADLIGHT -> nightHeadlight(facts);
			case SIGNAL_CANCEL -> signalCancel(facts, thresholds.signalCancel());
			case SEAT_BELT -> seatBelt(facts, thresholds.seatBelt());
			case RAIN_DRIVING -> rainDriving(facts);
			case TURN_SPEED -> turnSpeed(facts, thresholds.turn(), minSamples(thresholds, ScoreItem.TURN_SPEED));
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

	// 회전마다 제때 1 · 늦게 lateSignalScore · 반대 방향 · 안 켬 0의 평균(D18).
	// signalTiming이 없는 P0 payload는 signalOn으로 2단계(켬 1 · 안 켬 0) 판정한다.
	// 회전이 없으면 이 항목만 빼고 나머지로 평점한다(plan.md 4.1).
	private static Outcome turnSignal(TripFacts facts, Thresholds.Turn rule, int minSamples) {
		List<DrivingEvent> turns = ofType(facts, TripEventType.TURN);
		if (turns.isEmpty()) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_TURN");
		}
		if (turns.size() < minSamples) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "TOO_FEW_SAMPLES");
		}
		int onTime = 0;
		int late = 0;
		for (DrivingEvent turn : turns) {
			SignalTiming timing = timingOf(turn.payload());
			if (timing == SignalTiming.ON_TIME) {
				onTime++;
			} else if (timing == SignalTiming.LATE) {
				late++;
			}
		}
		double value = (onTime + late * rule.lateSignalScore()) / turns.size();
		return Outcome.of(value, Map.of("turns", turns.size(), "onTime", onTime, "late", late,
			"missed", turns.size() - onTime - late));
	}

	private static SignalTiming timingOf(EventPayload payload) {
		if (payload.signalTiming() != null) {
			return payload.signalTiming();
		}
		return payload.signalOn() ? SignalTiming.ON_TIME : SignalTiming.NONE;
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

	// 속도를 아는 회전 중 최고 속도가 turnSpeedKmh 이하인 비율(D18).
	private static Outcome turnSpeed(TripFacts facts, Thresholds.Turn rule, int minSamples) {
		List<DrivingEvent> turns = ofType(facts, TripEventType.TURN);
		if (turns.isEmpty()) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_TURN");
		}
		List<Double> speeds = turns.stream().map(t -> t.payload().maxSpeedKmh()).filter(Objects::nonNull).toList();
		if (speeds.isEmpty()) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_SPEED");
		}
		if (speeds.size() < minSamples) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "TOO_FEW_SAMPLES");
		}
		long within = speeds.stream().filter(speed -> speed <= rule.turnSpeedKmh()).count();
		return Outcome.of((double) within / speeds.size(), Map.of("turns", speeds.size(), "withinLimit", (int) within));
	}

	// 출발 시 미착용 −departurePenalty, 주행 중 미착용 1분마다 −perMinutePenalty, 최소 0(D18). 운행당 1건.
	private static Outcome seatBelt(TripFacts facts, Thresholds.SeatBelt rule) {
		List<DrivingEvent> records = ofType(facts, TripEventType.SEAT_BELT);
		if (records.isEmpty()) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_DATA");
		}
		EventPayload belt = records.get(records.size() - 1).payload();
		boolean buckled = !Boolean.FALSE.equals(belt.buckledAtDeparture());
		int unbuckledSec = belt.unbuckledMovingSec() == null ? 0 : belt.unbuckledMovingSec();
		int penalty = (buckled ? 0 : rule.departurePenalty()) + rule.perMinutePenalty() * (unbuckledSec / 60);
		return Outcome.of(Math.max(0, 100 - penalty) / 100.0,
			Map.of("buckledAtDeparture", buckled ? 1 : 0, "unbuckledMovingSec", unbuckledSec));
	}

	// max(0, 100 − penalty × 깜빡이 끄기를 잊은 횟수) (D18)
	private static Outcome signalCancel(TripFacts facts, Thresholds.SignalCancel rule) {
		int count = ofType(facts, TripEventType.SIGNAL_LEFT_ON).size();
		return Outcome.of(Math.max(0, 1 - rule.penalty() * count / 100.0), Map.of("count", count));
	}

	// (1 − 꺼짐 비율) × 100. 야간 구간이 없으면 측정하지 않는다. 오토라이트 규칙은 없다(D20).
	private static Outcome nightHeadlight(TripFacts facts) {
		int duration = sum(facts, TripEventType.NIGHT_SEGMENT, EventPayload::durationSec);
		int off = sum(facts, TripEventType.NIGHT_SEGMENT, EventPayload::headlightOffSec);
		if (duration <= 0) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_NIGHT");
		}
		return Outcome.of(1 - Math.min(1.0, (double) off / duration),
			Map.of("durationSec", duration, "headlightOffSec", off));
	}

	// 비 오는 구간에서 ① 점등 비율과 ② 제한속도의 rain.speedRatio 이하 준수 비율(앱 판정)의 평균. 제한속도를 모르면 ①만(D18).
	private static Outcome rainDriving(TripFacts facts) {
		int duration = sum(facts, TripEventType.RAIN_SEGMENT, EventPayload::durationSec);
		if (duration <= 0) {
			return Outcome.excluded(MetricStatus.NOT_APPLICABLE, "NO_RAIN");
		}
		int off = sum(facts, TripEventType.RAIN_SEGMENT, EventPayload::headlightOffSec);
		int known = sum(facts, TripEventType.RAIN_SEGMENT, EventPayload::knownLimitSec);
		int over = sum(facts, TripEventType.RAIN_SEGMENT, EventPayload::overRainLimitSec);
		double light = 1 - Math.min(1.0, (double) off / duration);
		double value = known > 0 ? (light + (1 - Math.min(1.0, (double) over / known))) / 2 : light;
		return Outcome.of(value, Map.of("durationSec", duration, "headlightOffSec", off,
			"knownLimitSec", known, "overRainLimitSec", over));
	}

	private static int sum(TripFacts facts, TripEventType type, Function<EventPayload, Integer> field) {
		return ofType(facts, type).stream()
			.map(event -> field.apply(event.payload()))
			.filter(Objects::nonNull)
			.mapToInt(Integer::intValue)
			.sum();
	}

	// 항목별 최소 표본(기준값 minSamples, 없으면 1). 1~2번의 우연으로 점수가 흔들리지 않게 한다(D18 · D27).
	private static int minSamples(Thresholds thresholds, ScoreItem item) {
		return thresholds.minSamples() == null ? 1 : thresholds.minSamples().getOrDefault(item, 1);
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
