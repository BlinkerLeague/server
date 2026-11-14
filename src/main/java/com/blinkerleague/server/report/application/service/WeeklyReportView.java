package com.blinkerleague.server.report.application.service;

import java.util.List;

import com.blinkerleague.server.report.domain.MetricChange;
import com.blinkerleague.server.report.domain.WeeklyMissionStatus;
import com.blinkerleague.server.threshold.domain.ScoreItem;

/**
 * 주간 리포트(plan.md 4.5, SCR-07). metrics의 점수는 항목별 거리 가중 평균 점수(0~100)다.
 * lastMission은 이 주에 수행한 맞춤 미션의 판정, nextMission은 다음 주에 자동 등록한 맞춤 미션이다(D7 B).
 */
public record WeeklyReportView(String week, int trips, double distanceKm, List<MetricChange> metrics,
	ScoreItem weakestMetric, MissionView lastMission, MissionView nextMission) {

	public record MissionView(String week, ScoreItem metric, String title, int target, double minDistanceKm,
		WeeklyMissionStatus status) {
	}
}
