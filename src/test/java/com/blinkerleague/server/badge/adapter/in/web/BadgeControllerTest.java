package com.blinkerleague.server.badge.adapter.in.web;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BadgeControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 점수가_난_첫_운행을_올리면_전체_배지_중_첫_경기_배지만_받은_상태로_보인다() throws Exception {
		Long driverId = 800_000L + new Random().nextInt(100_000);
		String body = """
			{ "tripId": "%s", "startedAt": "2026-11-03T08:00:00+09:00", "endedAt": "2026-11-03T08:40:00+09:00",
			  "distanceKm": 12.3, "supported": ["TURN_SIGNAL"],
			  "events": [{ "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:10:00+09:00",
			               "payload": { "direction": "LEFT", "signalOn": true } }] }
			""".formatted(UUID.randomUUID(), UUID.randomUUID());
		mockMvc.perform(post("/api/trips").header(HttpHeaders.AUTHORIZATION, bearer(driverId))
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/badges").header(HttpHeaders.AUTHORIZATION, bearer(driverId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].code").value("FIRST_TRIP"))
			.andExpect(jsonPath("$[0].earned").value(true))
			.andExpect(jsonPath("$[0].awardedAt").value(matchesPattern(".+\\+09:00$")))
			.andExpect(jsonPath("$[1].code").value("STREAK_7"))
			.andExpect(jsonPath("$[1].earned").value(false))
			.andExpect(jsonPath("$[1].awardedAt").value(nullValue()))
			.andExpect(jsonPath("$[2].code").value("SIGNAL_100"))
			.andExpect(jsonPath("$[2].name").isString())
			.andExpect(jsonPath("$[2].condition").isString());
	}

	private static String bearer(Long driverId) {
		return "Bearer " + TestTokens.accessToken(driverId);
	}
}
