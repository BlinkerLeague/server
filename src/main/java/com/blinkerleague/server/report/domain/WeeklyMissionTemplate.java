package com.blinkerleague.server.report.domain;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 가장 약한 항목별 다음 주 맞춤 미션 틀(reports/weekly-missions.json). 코드 수정 없이 늘린다. */
public record WeeklyMissionTemplate(ScoreItem metric, String title, int target, double minDistanceKm) {
}
