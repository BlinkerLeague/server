package com.blinkerleague.server.league.adapter.out.record;

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

	private final JdbcTemplate jdbcTemplate;

	@Override
	public List<MemberRecord> records(LeagueSeason season) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> regionCodeOf(Long driverId) {
		throw new UnsupportedOperationException();
	}
}
