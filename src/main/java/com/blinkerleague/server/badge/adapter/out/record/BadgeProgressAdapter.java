package com.blinkerleague.server.badge.adapter.out.record;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.badge.application.port.out.BadgeProgressPort;
import com.blinkerleague.server.badge.domain.BadgeProgress;
import com.blinkerleague.server.badge.domain.LongestStreak;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** 저장된 운행(trip · trip_event)과 미션 기록(driver_mission)을 읽어 배지 판정용 누적 기록을 만든다. */
@Component
@RequiredArgsConstructor
public class BadgeProgressAdapter implements BadgeProgressPort {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String SCORED_TRIPS =
		"select count(*) from trip where driver_id = ? and status = 'SCORED'";

	// 최소 거리 미만 운행은 점수 · 미션과 같이 세지 않는다.
	private static final String TURN_PAYLOADS = """
		select e.payload
		  from trip_event e
		  join trip t on t.trip_id = e.trip_id
		 where t.driver_id = ? and e.type = 'TURN' and t.status <> 'EXCLUDED_SHORT'
		""";

	private static final String ACHIEVED_DAYS =
		"select period_key from driver_mission where driver_id = ? and status = 'ACHIEVED'";

	private final JdbcTemplate jdbcTemplate;

	@Override
	public BadgeProgress progress(Long driverId) {
		Integer scoredTrips = jdbcTemplate.queryForObject(SCORED_TRIPS, Integer.class, driverId);
		int signalOnTurns = (int) jdbcTemplate.queryForList(TURN_PAYLOADS, String.class, driverId).stream()
			.filter(BadgeProgressAdapter::signalOn)
			.count();
		Set<LocalDate> achieved = jdbcTemplate.queryForList(ACHIEVED_DAYS, String.class, driverId).stream()
			.map(LocalDate::parse)
			.collect(Collectors.toSet());
		return new BadgeProgress(scoredTrips == null ? 0 : scoredTrips, LongestStreak.of(achieved), signalOnTurns);
	}

	// 점수 엔진과 같은 기준(D23): signalOn이 boolean true일 때만 켠 회전이다.
	private static boolean signalOn(String payload) {
		if (payload == null) {
			return false;
		}
		try {
			JsonNode signalOn = MAPPER.readTree(payload).path("signalOn");
			return signalOn.isBoolean() && signalOn.booleanValue();
		} catch (JsonProcessingException e) {
			return false;
		}
	}
}
