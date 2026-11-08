package com.blinkerleague.server.mission.adapter.out.trip;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.LoadDailyTurnsPort;
import com.blinkerleague.server.mission.domain.RoundTurns;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** 저장된 운행의 TURN 이벤트(payload.signalOn)로 그날 회전 결과를 센다. */
@Component
@RequiredArgsConstructor
public class TripDailyTurnsAdapter implements LoadDailyTurnsPort {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String TURN_PAYLOADS = """
		select e.payload
		  from trip_event e
		  join trip t on t.trip_id = e.trip_id
		 where t.driver_id = ?
		   and t.ended_at >= ? and t.ended_at < ?
		   and t.status <> 'EXCLUDED_SHORT'
		   and e.type = 'TURN'
		""";

	private final JdbcTemplate jdbcTemplate;

	@Override
	public RoundTurns load(Long driverId, Instant from, Instant to) {
		List<String> payloads = jdbcTemplate.queryForList(TURN_PAYLOADS, String.class,
			driverId, Timestamp.from(from), Timestamp.from(to));
		int success = (int)payloads.stream().filter(TripDailyTurnsAdapter::signalOn).count();
		return new RoundTurns(payloads.size(), success);
	}

	@Override
	public List<RoundTurns> loadRounds(Long driverId, Instant from, Instant to) {
		throw new UnsupportedOperationException();
	}

	// payload가 없거나 읽을 수 없으면 깜빡이를 안 켠 회전으로 본다.
	private static boolean signalOn(String payload) {
		if (payload == null) {
			return false;
		}
		try {
			return MAPPER.readTree(payload).path("signalOn").asBoolean(false);
		} catch (JsonProcessingException e) {
			return false;
		}
	}
}
