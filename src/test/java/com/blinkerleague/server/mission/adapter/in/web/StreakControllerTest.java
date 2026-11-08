package com.blinkerleague.server.mission.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StreakControllerTest.FixedClock.class)
class StreakControllerTest {

	// 2026-11-03 12:00 KST
	static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T03:00:00Z"), ZoneId.of("Asia/Seoul"));

	@TestConfiguration
	static class FixedClock {
		@Bean
		@Primary
		Clock fixedClock() {
			return CLOCK;
		}
	}

	@Autowired
	MockMvc mockMvc;

	// 다른 테스트와 같은 H2를 쓰므로 이 클래스만 쓰는 운전자 id를 쓴다.
	@Test
	void 어제와_오늘_미션을_달성하면_연속_2일이고_오늘_달성했다고_알려_준다() throws Exception {
		Long driverId = 7101L;
		perfectTrip(driverId, "2026-11-02").andExpect(status().isCreated());
		perfectTrip(driverId, "2026-11-03").andExpect(status().isCreated());

		mockMvc.perform(get("/api/streak").header(HttpHeaders.AUTHORIZATION, bearer(driverId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.streakDays").value(2))
			.andExpect(jsonPath("$.todayAchieved").value(true));
	}

	// date 08:00~08:40 KST, 깜빡이를 켠 회전 3회(기본 미션 달성)
	private ResultActions perfectTrip(Long driverId, String date) throws Exception {
		String turns = IntStream.range(0, 3).mapToObj(i -> """
			{ "eventId": "%s", "type": "TURN", "occurredAt": "%sT08:1%d:00+09:00",
			  "payload": { "direction": "LEFT", "signalOn": true } }""".formatted(UUID.randomUUID(), date, i))
			.collect(Collectors.joining(","));
		String body = """
			{ "tripId": "%s", "startedAt": "%sT08:00:00+09:00", "endedAt": "%sT08:40:00+09:00",
			  "distanceKm": 12.3, "supported": ["TURN_SIGNAL"], "events": [%s] }
			""".formatted(UUID.randomUUID(), date, date, turns);
		return mockMvc.perform(post("/api/trips").header(HttpHeaders.AUTHORIZATION, bearer(driverId))
			.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String bearer(Long driverId) {
		return "Bearer " + TestTokens.accessToken(TestTokens.JWT, driverId, CLOCK);
	}
}
