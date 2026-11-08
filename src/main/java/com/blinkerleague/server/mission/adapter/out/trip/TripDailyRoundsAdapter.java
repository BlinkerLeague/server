package com.blinkerleague.server.mission.adapter.out.trip;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.LoadDailyRoundsPort;
import com.blinkerleague.server.mission.domain.RoundTurns;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** 저장된 운행(라운드)마다 TURN 이벤트(payload.signalOn)로 회전 결과를 센다. */
@Component
@RequiredArgsConstructor
public class TripDailyRoundsAdapter implements LoadDailyRoundsPort {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	// 회전이 없는 운행도 라운드이므로 trip 기준 left join이다.
	private static final String ROUND_TURNS = """
		select t.trip_id, e.event_id, e.payload
		  from trip t
		  left join trip_event e on e.trip_id = t.trip_id and e.type = 'TURN'
		 where t.driver_id = ?
		   and t.ended_at >= ? and t.ended_at < ?
		   and t.status <> 'EXCLUDED_SHORT'
		 order by t.ended_at, t.trip_id
		""";

	private final JdbcTemplate jdbcTemplate;

	@Override
	public List<RoundTurns> load(Long driverId, Instant from, Instant to) {
		Map<String, int[]> rounds = new LinkedHashMap<>();
		jdbcTemplate.query(ROUND_TURNS, (RowCallbackHandler)row -> {
			int[] counts = rounds.computeIfAbsent(row.getString("trip_id"), id -> new int[2]);
			if (row.getString("event_id") != null) {
				counts[0]++;
				if (signalOn(row.getString("payload"))) {
					counts[1]++;
				}
			}
		}, driverId, Timestamp.from(from), Timestamp.from(to));
		return rounds.values().stream().map(counts -> new RoundTurns(counts[0], counts[1])).toList();
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
