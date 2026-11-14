package com.blinkerleague.server.point.adapter.in.web;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.point.application.service.EarnPointService;
import com.blinkerleague.server.support.MutableClockConfig;
import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MutableClockConfig.class)
class PointControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	EarnPointService earnPointService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private Long driverId;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("delete from point_ledger where driver_id = ?", driverId);
		jdbcTemplate.update("delete from driver where driver_id = ?", driverId);
	}

	@Test
	void 내_포인트를_조회하면_잔액_오늘_적립_일일_상한과_적립_시점_설명이_담긴_원장을_돌려준다() throws Exception {
		driverId = registerDriver();
		String tripId = UUID.randomUUID().toString();
		earnPointService.earnForTrip(driverId, tripId, 12.3, 86);

		mockMvc.perform(get("/api/points")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(driverId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(10))
			.andExpect(jsonPath("$.todayEarned").value(10))
			.andExpect(jsonPath("$.dailyCap").value(150))
			.andExpect(jsonPath("$.entries.length()").value(1))
			.andExpect(jsonPath("$.entries[0].amount").value(10))
			.andExpect(jsonPath("$.entries[0].reason").value("TRIP"))
			.andExpect(jsonPath("$.entries[0].description.distanceKm").value(12.3))
			.andExpect(jsonPath("$.entries[0].description.score").value(86))
			.andExpect(jsonPath("$.entries[0].createdAt").value(matchesPattern(".+\\+09:00$")));
	}

	@Test
	void 점수가_난_운행으로_미션을_달성하면_운행_포인트와_미션_보상이_원장에_쌓인다() throws Exception {
		driverId = registerDriver();
		String turns = java.util.stream.IntStream.range(0, 3).mapToObj(i -> """
			{ "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:1%d:00+09:00",
			  "payload": { "direction": "LEFT", "signalOn": true } }""".formatted(UUID.randomUUID(), i))
			.collect(java.util.stream.Collectors.joining(","));
		String trip = """
			{ "tripId": "%s", "startedAt": "2026-11-03T08:00:00+09:00", "endedAt": "2026-11-03T08:40:00+09:00",
			  "distanceKm": 12.3, "supported": ["TURN_SIGNAL"], "events": [%s] }
			""".formatted(UUID.randomUUID(), turns);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/trips")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(driverId))
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.content(trip))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.mission.status").value("ACHIEVED"));

		// 운행 12.3km × 100점 → 12P, 미션 달성 20P
		mockMvc.perform(get("/api/points")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(driverId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(32))
			.andExpect(jsonPath("$.entries.length()").value(2))
			.andExpect(jsonPath("$.entries[?(@.reason == 'TRIP')].amount").value(org.hamcrest.Matchers.contains(12)))
			.andExpect(jsonPath("$.entries[?(@.reason == 'DAILY_MISSION')].amount").value(org.hamcrest.Matchers.contains(20)))
			.andExpect(jsonPath("$.entries[?(@.reason == 'DAILY_MISSION')].description").value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
	}

	private Long registerDriver() {
		String nickname = "p" + UUID.randomUUID().toString().substring(0, 8);
		jdbcTemplate.update("insert into driver (nickname, region_code, created_at, updated_at) "
			+ "values (?, '11680', current_timestamp, current_timestamp)", nickname);
		return jdbcTemplate.queryForObject("select driver_id from driver where nickname = ?", Long.class, nickname);
	}
}
