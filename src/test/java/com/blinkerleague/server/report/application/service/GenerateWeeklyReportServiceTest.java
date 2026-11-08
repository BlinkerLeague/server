package com.blinkerleague.server.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.report.application.service.WeeklyReportView.MissionView;
import com.blinkerleague.server.report.domain.MetricChange;
import com.blinkerleague.server.report.domain.ReportWeek;
import com.blinkerleague.server.report.domain.TripMetricRecord;
import com.blinkerleague.server.report.domain.WeekDriving;
import com.blinkerleague.server.report.domain.WeeklyMissionStatus;
import com.blinkerleague.server.report.domain.WeeklyMissionTemplate;
import com.blinkerleague.server.threshold.domain.ScoreItem;

class GenerateWeeklyReportServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final ReportWeek W45 = ReportWeek.parse("2026-W45");

	// W45(이번 주)와 W44(지난주) 운행 기록
	private final Map<Instant, WeekDriving> byWeekStart = Map.of(
		W45.start(SEOUL), new WeekDriving(3, 60.0, List.of(
			new TripMetricRecord("a", ScoreItem.TURN_SIGNAL, 90, 40.0),
			new TripMetricRecord("b", ScoreItem.TURN_SIGNAL, 60, 20.0),
			new TripMetricRecord("a", ScoreItem.SAFETY_WARNING, 60, 40.0),
			new TripMetricRecord("b", ScoreItem.SAFETY_WARNING, 60, 20.0))),
		W45.previous().start(SEOUL), new WeekDriving(1, 20.0, List.of(
			new TripMetricRecord("c", ScoreItem.TURN_SIGNAL, 75, 20.0))));

	private final InMemoryWeeklyStores stores = new InMemoryWeeklyStores();

	private final GenerateWeeklyReportService service = new GenerateWeeklyReportService(
		(driverId, from, to) -> byWeekStart.getOrDefault(from, new WeekDriving(0, 0, List.of())),
		stores, stores,
		metric -> Optional.of(new WeeklyMissionTemplate(metric, metric + " 미션", 90, 50.0)),
		SEOUL);

	@Test
	void 항목을_지난주와_비교하고_가장_약한_항목으로_다음_주_맞춤_미션을_자동_등록한다() {
		WeeklyReportView report = service.generate(1L, W45);

		assertThat(report.week()).isEqualTo("2026-W45");
		assertThat(report.trips()).isEqualTo(3);
		assertThat(report.distanceKm()).isEqualTo(60.0);
		// 깜빡이 (90 × 40 + 60 × 20) ÷ 60 = 80
		assertThat(report.metrics()).containsExactly(
			new MetricChange(ScoreItem.TURN_SIGNAL, 80, 75, 5),
			new MetricChange(ScoreItem.SAFETY_WARNING, 60, null, null));
		assertThat(report.weakestMetric()).isEqualTo(ScoreItem.SAFETY_WARNING);
		assertThat(report.nextMission()).isEqualTo(new MissionView("2026-W46", ScoreItem.SAFETY_WARNING,
			"SAFETY_WARNING 미션", 90, 50.0, WeeklyMissionStatus.ASSIGNED));
		assertThat(stores.find(1L, "2026-W46")).hasValueSatisfying(
			m -> assertThat(m.getStatus()).isEqualTo(WeeklyMissionStatus.ASSIGNED));
		assertThat(stores.findReport(1L, "2026-W45")).isPresent();
	}
}
