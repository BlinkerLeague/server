package com.blinkerleague.server.league.adapter.out.record;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.out.LoadLeagueDriverPort;
import com.blinkerleague.server.league.application.port.out.LoadLeagueRecordsPort;
import com.blinkerleague.server.league.domain.LeagueSeason;
import com.blinkerleague.server.league.domain.MemberRecord;

import lombok.RequiredArgsConstructor;

/** 리그 집계용 읽기. trip · driver · league_member를 JDBC로 읽는다. */
@Component
@RequiredArgsConstructor
public class LeagueRecordsAdapter implements LoadLeagueRecordsPort, LoadLeagueDriverPort {

	// 시즌 소속 운전자마다 시즌 안에 끝난 참여 운행(최소 거리 이상)의 수 · 거리(D13)와,
	// 리그 점수가 있는 운행의 거리 합 · (리그 점수 × 거리) 합(D19 · D5)
	private static final String RECORDS = """
		select m.driver_id, d.nickname, m.league_code, d.league_visible,
		       count(t.trip_id) as trips,
		       coalesce(sum(t.distance_km), 0) as distance,
		       coalesce(sum(case when t.league_score is not null then t.distance_km end), 0) as scored_distance,
		       coalesce(sum(case when t.league_score is not null then t.league_score * t.distance_km end), 0) as weighted
		  from league_member m
		  join driver d on d.driver_id = m.driver_id
		  left join trip t on t.driver_id = m.driver_id
		                  and t.ended_at >= ? and t.ended_at < ?
		                  and t.status <> 'EXCLUDED_SHORT'
		 where m.season_id = ?
		 group by m.driver_id, d.nickname, m.league_code, d.league_visible
		 order by m.driver_id
		""";

	private final JdbcTemplate jdbcTemplate;

	@Override
	public List<MemberRecord> records(LeagueSeason season) {
		return jdbcTemplate.query(RECORDS, (row, n) -> new MemberRecord(row.getLong("driver_id"),
				row.getString("nickname"), row.getString("league_code"), row.getBoolean("league_visible"),
				row.getInt("trips"), row.getDouble("distance"), row.getDouble("scored_distance"),
				row.getDouble("weighted")),
			Timestamp.from(season.startsAt()), Timestamp.from(season.endsAt()), season.id());
	}

	@Override
	public Optional<String> regionCodeOf(Long driverId) {
		return jdbcTemplate.query("select region_code from driver where driver_id = ?",
			(row, n) -> row.getString("region_code"), driverId).stream().findFirst();
	}
}
