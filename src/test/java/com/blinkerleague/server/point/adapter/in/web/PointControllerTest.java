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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.point.application.service.EarnPointService;
import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

	private Long registerDriver() {
		String nickname = "p" + UUID.randomUUID().toString().substring(0, 8);
		jdbcTemplate.update("insert into driver (nickname, region_code, created_at, updated_at) "
			+ "values (?, '11680', current_timestamp, current_timestamp)", nickname);
		return jdbcTemplate.queryForObject("select driver_id from driver where nickname = ?", Long.class, nickname);
	}
}
