package com.blinkerleague.server.report.adapter.out.record;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.report.application.port.out.LoadWeekDriversPort;
import com.blinkerleague.server.report.application.port.out.LoadWeekDrivingPort;
import com.blinkerleague.server.report.domain.TripMetricRecord;
import com.blinkerleague.server.report.domain.WeekDriving;
import com.blinkerleague.server.threshold.domain.ScoreItem;

import lombok.RequiredArgsConstructor;

/** 저장된 운행(trip)과 항목별 점수(trip_metric)를 읽어 한 주 기록을 만든다. */
@Component
@RequiredArgsConstructor
public class WeekDrivingAdapter implements LoadWeekDrivingPort, LoadWeekDriversPort {

	// 최소 거리 미만 운행은 점수 · 미션과 같이 세지 않는다.
	private static final String TOTALS = """
		select count(*) as trips, coalesce(sum(distance_km), 0) as distance
		  from trip
		 where driver_id = ? and ended_at >= ? and ended_at < ? and status <> 'EXCLUDED_SHORT'
		""";

	private static final String SCORED_METRICS = """
		select t.trip_id, m.metric, m.score, t.distance_km
		  from trip_metric m
		  join trip t on t.trip_id = m.trip_id
		 where t.driver_id = ? and t.ended_at >= ? and t.ended_at < ? and m.status = 'SCORED'
		 order by t.ended_at, t.trip_id, m.metric
		""";

	private final JdbcTemplate jdbcTemplate;

	@Override
	public WeekDriving load(Long driverId, Instant from, Instant to) {
		Timestamp start = Timestamp.from(from);
		Timestamp end = Timestamp.from(to);
		WeekDriving totals = jdbcTemplate.queryForObject(TOTALS,
			(row, n) -> new WeekDriving(row.getInt("trips"), row.getDouble("distance"), List.of()),
			driverId, start, end);
		List<TripMetricRecord> metrics = jdbcTemplate.query(SCORED_METRICS,
			(row, n) -> new TripMetricRecord(row.getString("trip_id"), ScoreItem.valueOf(row.getString("metric")),
				row.getInt("score"), row.getDouble("distance_km")),
			driverId, start, end);
		return new WeekDriving(totals.trips(), totals.distanceKm(), metrics);
	}

	@Override
	public List<Long> driversWithTrips(Instant from, Instant to) {
		return jdbcTemplate.queryForList("""
			select distinct driver_id
			  from trip
			 where ended_at >= ? and ended_at < ? and status <> 'EXCLUDED_SHORT'
			 order by driver_id
			""", Long.class, Timestamp.from(from), Timestamp.from(to));
	}
}
