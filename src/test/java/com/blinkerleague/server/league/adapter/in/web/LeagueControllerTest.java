package com.blinkerleague.server.league.adapter.in.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.web.servlet.ResultActions;

import com.blinkerleague.server.support.MutableClock;
import com.blinkerleague.server.support.MutableClockConfig;
import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MutableClockConfig.class)
class LeagueControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbcTemplate;

	// 다른 테스트와 같은 H2를 쓰므로 다른 테스트가 쓰지 않는 부산(26xxx) 리그로 확인한다.
	@Test
	void 내_리그의_순위_내_순위_지역_안전지수와_시즌_기간을_돌려준다() throws Exception {
		List<Long> drivers = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			drivers.add(registerDriver("26440"));
		}
		Long me = drivers.get(0);
		// 9명은 깜빡이를 모두 켜고(리그 점수 100) 1명은 켜지 않는다(0). 모두 60km라 순위 자격(D13)이 있다.
		for (Long driver : drivers) {
			upload(driver, !driver.equals(drivers.get(9))).andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/leagues/current").header(HttpHeaders.AUTHORIZATION, bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.season.seasonId").value("2026-11"))
			.andExpect(jsonPath("$.season.startsAt").value("2026-11-01T00:00:00+09:00"))
			.andExpect(jsonPath("$.season.endsAt").value("2026-12-01T00:00:00+09:00"))
			.andExpect(jsonPath("$.league.leagueCode").value("BUSAN"))
			.andExpect(jsonPath("$.league.name").value("부산"))
			.andExpect(jsonPath("$.league.participants").value(10))
			.andExpect(jsonPath("$.league.rankingPublished").value(true))
			// (100 × 60 × 9 + 0 × 60) ÷ 600 = 90.0
			.andExpect(jsonPath("$.league.safetyIndex").value(90.0))
			.andExpect(jsonPath("$.league.rankings.length()").value(10))
			.andExpect(jsonPath("$.league.rankings[0].rank").value(1))
			.andExpect(jsonPath("$.league.rankings[0].score").value(100.0))
			.andExpect(jsonPath("$.league.rankings[9].rank").value(10))
			.andExpect(jsonPath("$.league.rankings[9].score").value(0.0))
			.andExpect(jsonPath("$.me.visible").value(true))
			.andExpect(jsonPath("$.me.eligible").value(true))
			.andExpect(jsonPath("$.me.score").value(100.0))
			.andExpect(jsonPath("$.me.rank").value(1))
			.andExpect(jsonPath("$.me.trips").value(1))
			.andExpect(jsonPath("$.me.distanceKm").value(60.0))
			.andExpect(jsonPath("$.regions[?(@.leagueCode == 'BUSAN')].safetyIndex").value(90.0))
			.andExpect(jsonPath("$.regions[?(@.leagueCode == 'JEJU')].safetyIndex").value(contains(nullValue())));
	}

	private ResultActions upload(Long driverId, boolean signalOn) throws Exception {
		String body = """
			{ "tripId": "%s", "startedAt": "2026-11-03T08:00:00+09:00", "endedAt": "2026-11-03T09:00:00+09:00",
			  "distanceKm": 60.0, "supported": ["TURN_SIGNAL"],
			  "events": [ { "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:30:00+09:00",
			                "payload": { "direction": "LEFT", "signalOn": %s } } ] }
			""".formatted(UUID.randomUUID(), UUID.randomUUID(), signalOn);
		return mockMvc.perform(post("/api/trips").header(HttpHeaders.AUTHORIZATION, bearer(driverId))
			.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Long registerDriver(String regionCode) {
		String nickname = "l" + UUID.randomUUID().toString().substring(0, 8);
		jdbcTemplate.update("insert into driver (nickname, region_code, created_at, updated_at) "
			+ "values (?, ?, current_timestamp, current_timestamp)", nickname, regionCode);
		return jdbcTemplate.queryForObject("select driver_id from driver where nickname = ?", Long.class, nickname);
	}

	private String bearer(Long driverId) {
		return "Bearer " + TestTokens.accessToken(TestTokens.JWT, driverId, clock);
	}
}
