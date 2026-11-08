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
import java.util.stream.Stream;

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
@Import(MissionControllerTest.FixedClock.class)
class MissionControllerTest {

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
	void 오늘_올린_운행의_회전으로_기본_미션_진행도를_돌려준다() throws Exception {
		Long driverId = 7001L;
		upload(driverId, true, true).andExpect(status().isCreated());

		today(driverId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.missionId").isNumber())
			.andExpect(jsonPath("$.title").value("회전 깜빡이 완벽"))
			.andExpect(jsonPath("$.periodKey").value("2026-11-03"))
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.rewardPoint").value(20))
			.andExpect(jsonPath("$.progress.turns").value(2))
			.andExpect(jsonPath("$.progress.successTurns").value(2))
			.andExpect(jsonPath("$.progress.requiredTurns").value(3))
			.andExpect(jsonPath("$.conditions[0].type").value("TURN_COUNT"))
			.andExpect(jsonPath("$.conditions[0].current").value(2))
			.andExpect(jsonPath("$.conditions[0].target").value(3))
			.andExpect(jsonPath("$.conditions[1].type").value("SIGNAL_RATE"))
			.andExpect(jsonPath("$.conditions[1].current").value(100))
			.andExpect(jsonPath("$.conditions[1].target").value(100));
	}

	private ResultActions today(Long driverId) throws Exception {
		return mockMvc.perform(get("/api/missions/today").header(HttpHeaders.AUTHORIZATION, bearer(driverId)));
	}

	// 2026-11-03 08:00~08:40 KST 운행. signals 하나가 TURN 이벤트 하나(깜빡이 켬 여부)다.
	private ResultActions upload(Long driverId, boolean... signals) throws Exception {
		String events = Stream.iterate(0, i -> i + 1).limit(signals.length)
			.map(i -> """
				{ "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:1%d:00+09:00",
				  "payload": { "direction": "LEFT", "signalOn": %s } }""".formatted(UUID.randomUUID(), i, signals[i]))
			.collect(Collectors.joining(","));
		String body = """
			{
			  "tripId": "%s",
			  "startedAt": "2026-11-03T08:00:00+09:00",
			  "endedAt": "2026-11-03T08:40:00+09:00",
			  "distanceKm": 12.3,
			  "supported": ["TURN_SIGNAL"],
			  "events": [%s]
			}
			""".formatted(UUID.randomUUID(), events);
		return mockMvc.perform(post("/api/trips")
			.header(HttpHeaders.AUTHORIZATION, bearer(driverId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String bearer(Long driverId) {
		return "Bearer " + TestTokens.accessToken(TestTokens.JWT, driverId, CLOCK);
	}
}
