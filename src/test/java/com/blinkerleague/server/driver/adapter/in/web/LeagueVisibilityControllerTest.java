package com.blinkerleague.server.driver.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.support.MutableClock;
import com.blinkerleague.server.support.MutableClockConfig;
import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MutableClockConfig.class)
class LeagueVisibilityControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void 리그_순위_공개를_끄면_200과_바뀐_값을_돌려주고_리그_조회에_반영된다() throws Exception {
		Long driverId = registerDriver();

		mockMvc.perform(put("/api/drivers/me/league-visibility").header(HttpHeaders.AUTHORIZATION, bearer(driverId))
				.contentType(MediaType.APPLICATION_JSON).content("{ \"visible\": false }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.visible").value(false));

		mockMvc.perform(get("/api/leagues/current").header(HttpHeaders.AUTHORIZATION, bearer(driverId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.me.visible").value(false));
	}

	private Long registerDriver() {
		String nickname = "v" + UUID.randomUUID().toString().substring(0, 8);
		jdbcTemplate.update("insert into driver (nickname, region_code, created_at, updated_at) "
			+ "values (?, '27110', current_timestamp, current_timestamp)", nickname);
		return jdbcTemplate.queryForObject("select driver_id from driver where nickname = ?", Long.class, nickname);
	}

	private String bearer(Long driverId) {
		return "Bearer " + TestTokens.accessToken(TestTokens.JWT, driverId, clock);
	}
}
