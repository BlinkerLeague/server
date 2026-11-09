package com.blinkerleague.server.league.application.port.out;

import java.util.List;

import com.blinkerleague.server.league.domain.LeagueSeason;
import com.blinkerleague.server.league.domain.MemberRecord;

/** 시즌 소속 운전자들의 시즌 리그 기록. */
public interface LoadLeagueRecordsPort {

	List<MemberRecord> records(LeagueSeason season);
}
