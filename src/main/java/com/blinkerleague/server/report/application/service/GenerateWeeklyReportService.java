package com.blinkerleague.server.report.application.service;

import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import com.blinkerleague.server.report.application.port.out.LoadWeekDrivingPort;
import com.blinkerleague.server.report.application.port.out.LoadWeeklyMissionTemplatesPort;
import com.blinkerleague.server.report.application.port.out.WeeklyMissionStore;
import com.blinkerleague.server.report.application.port.out.WeeklyReportStore;
import com.blinkerleague.server.report.application.service.WeeklyReportView.MissionView;
import com.blinkerleague.server.report.domain.ReportWeek;
import com.blinkerleague.server.report.domain.WeekDriving;
import com.blinkerleague.server.report.domain.WeeklyAverages;
import com.blinkerleague.server.report.domain.WeeklyComparison;
import com.blinkerleague.server.report.domain.WeeklyMission;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** 운전자의 한 주 리포트를 만들어 저장한다(plan.md 4.5). 이미 있으면 저장본을 돌려준다. */
@RequiredArgsConstructor
public class GenerateWeeklyReportService {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final LoadWeekDrivingPort loadWeekDrivingPort;
	private final WeeklyMissionStore weeklyMissionStore;
	private final WeeklyReportStore weeklyReportStore;
	private final LoadWeeklyMissionTemplatesPort templates;
	private final ZoneId zone;

	public WeeklyReportView generate(Long driverId, ReportWeek week) {
		Optional<String> stored = weeklyReportStore.findReport(driverId, week.key());
		if (stored.isPresent()) {
			return fromJson(stored.get());
		}
		WeekDriving thisWeek = load(driverId, week);
		Map<ScoreItem, Integer> averages = WeeklyAverages.of(thisWeek.metrics());
		WeeklyComparison comparison = WeeklyComparison.of(averages,
			WeeklyAverages.of(load(driverId, week.previous()).metrics()));
		MissionView last = weeklyMissionStore.find(driverId, week.key())
			.map(mission -> judge(mission, averages, thisWeek.distanceKm()))
			.orElse(null);
		ScoreItem weakest = comparison.weakest().orElse(null);
		MissionView next = weakest == null ? null : assignNext(driverId, week.next(), weakest);

		WeeklyReportView report = new WeeklyReportView(week.key(), thisWeek.trips(), thisWeek.distanceKm(),
			comparison.metrics(), weakest, last, next);
		weeklyReportStore.saveReport(driverId, week.key(), toJson(report));
		return report;
	}

	// 지난 리포트가 이 주에 등록한 미션을 이 주 기록으로 판정한다.
	private MissionView judge(WeeklyMission mission, Map<ScoreItem, Integer> averages, double distanceKm) {
		mission.judge(averages.get(mission.getMetric()), distanceKm);
		weeklyMissionStore.save(mission);
		return view(mission);
	}

	// 가장 약한 항목의 미션 틀로 다음 주 미션을 자동 등록한다(D7 B: 수락 단계 없음).
	private MissionView assignNext(Long driverId, ReportWeek nextWeek, ScoreItem weakest) {
		return templates.templateFor(weakest).map(template -> {
			WeeklyMission mission = new WeeklyMission(driverId, nextWeek.key(), template.metric(), template.title(),
				template.target(), template.minDistanceKm());
			weeklyMissionStore.save(mission);
			return view(mission);
		}).orElse(null);
	}

	private WeekDriving load(Long driverId, ReportWeek week) {
		return loadWeekDrivingPort.load(driverId, week.start(zone), week.end(zone));
	}

	private static MissionView view(WeeklyMission mission) {
		return new MissionView(mission.getWeekKey(), mission.getMetric(), mission.getTitle(), mission.getTarget(),
			mission.getMinDistanceKm(), mission.getStatus());
	}

	private static WeeklyReportView fromJson(String json) {
		try {
			return MAPPER.readValue(json, WeeklyReportView.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("저장된 주간 리포트를 읽지 못했습니다.", e);
		}
	}

	private static String toJson(WeeklyReportView report) {
		try {
			return MAPPER.writeValueAsString(report);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("주간 리포트를 JSON으로 바꾸지 못했습니다.", e);
		}
	}
}
