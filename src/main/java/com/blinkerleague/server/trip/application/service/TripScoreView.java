package com.blinkerleague.server.trip.application.service;

import java.util.List;
import java.util.Map;

import com.blinkerleague.server.score.domain.MatchReport;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.trip.application.port.out.TripMissionPort;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripStatus;

/** 운행 결과(plan.md 3.5 TripResult). 업로드 응답과 점수 조회가 같은 구조를 쓴다. */
public record TripScoreView(String tripId, TripStatus status, Integer score, String grade, List<Metric> metrics,
	Turns turns, MatchReport.Summary summary, List<MatchReport.Play> plays, TripMissionPort.TripMission mission) {

	static TripScoreView of(Trip trip, Thresholds thresholds) {
		String grade = gradeOf(trip.getScore(), thresholds);
		List<Metric> metrics = trip.getMetrics().stream()
			.map(m -> new Metric(m.getMetric(), m.getStatus(), m.getReason(), m.getValue(), m.getScore(), m.getWeight(),
				MetricDetails.fromJson(m.getDetail())))
			.toList();
		MatchReport report = MatchReport.of(DrivingEvents.of(trip.getEvents()));
		Turns turns = new Turns(report.summary().goals() + report.summary().conceded(), report.summary().goals());
		return new TripScoreView(trip.getId(), trip.getStatus(), trip.getScore(), grade, metrics, turns,
			report.summary(), report.plays(), null);
	}

	TripScoreView withMission(TripMissionPort.TripMission mission) {
		return new TripScoreView(tripId, status, score, grade, metrics, turns, summary, plays, mission);
	}

	/** 점수가 없거나 최저 등급 기준 미만이면 등급이 없다. */
	static String gradeOf(Integer score, Thresholds thresholds) {
		return score == null ? null : thresholds.gradeOf(score).orElse(null);
	}

	/** 측정하지 못한 항목은 value · score · weight가 null이고 status · reason을 담는다. */
	public record Metric(ScoreItem metric, String status, String reason, Double value, Integer score, Double weight,
		Map<String, Object> detail) {
	}

	public record Turns(int total, int success) {
	}
}
