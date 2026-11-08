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
		upload(driverId, 8, true, true).andExpect(status().isCreated());

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

	@Test
	void 라운드_하나라도_달성하면_뒤에_실패한_라운드가_있어도_그날_미션은_ACHIEVED다() throws Exception {
		Long driverId = 7002L;
		upload(driverId, 7, false).andExpect(status().isCreated());
		upload(driverId, 8, true, true, true).andExpect(status().isCreated());
		upload(driverId, 9, true, false).andExpect(status().isCreated());

		today(driverId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACHIEVED"))
			.andExpect(jsonPath("$.progress.turns").value(3))
			.andExpect(jsonPath("$.progress.successTurns").value(3));
	}

	@Test
	void 오늘_첫_운행_전에는_어제의_최종_상태를_보여_주고_오늘_운행을_올리면_오늘_미션으로_바뀐다() throws Exception {
		Long driverId = 7003L;
		upload(driverId, "2026-11-02", 8, true, true).andExpect(status().isCreated());

		today(driverId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.periodKey").value("2026-11-02"))
			.andExpect(jsonPath("$.status").value("FAILED"))
			.andExpect(jsonPath("$.progress.turns").value(2));

		upload(driverId, 9, true).andExpect(status().isCreated());

		today(driverId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.periodKey").value("2026-11-03"))
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.progress.turns").value(1));
	}

	private ResultActions today(Long driverId) throws Exception {
		return mockMvc.perform(get("/api/missions/today").header(HttpHeaders.AUTHORIZATION, bearer(driverId)));
	}

	// 2026-11-03 hour:00~hour:40 KST 운행(라운드) 한 건. signals 하나가 TURN 이벤트 하나(깜빡이 켬 여부)다.
	private ResultActions upload(Long driverId, int hour, boolean... signals) throws Exception {
		return upload(driverId, "2026-11-03", hour, signals);
	}

	private ResultActions upload(Long driverId, String date, int hour, boolean... signals) throws Exception {
		String events = Stream.iterate(0, i -> i + 1).limit(signals.length)
			.map(i -> """
				{ "eventId": "%s", "type": "TURN", "occurredAt": "%sT%02d:1%d:00+09:00",
				  "payload": { "direction": "LEFT", "signalOn": %s } }""".formatted(UUID.randomUUID(), date, hour, i, signals[i]))
			.collect(Collectors.joining(","));
		String body = """
			{
			  "tripId": "%s",
			  "startedAt": "%sT%02d:00:00+09:00",
			  "endedAt": "%sT%02d:40:00+09:00",
			  "distanceKm": 12.3,
			  "supported": ["TURN_SIGNAL"],
			  "events": [%s]
			}
			""".formatted(UUID.randomUUID(), date, hour, date, hour, events);
		return mockMvc.perform(post("/api/trips")
			.header(HttpHeaders.AUTHORIZATION, bearer(driverId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String bearer(Long driverId) {
		return "Bearer " + TestTokens.accessToken(TestTokens.JWT, driverId, CLOCK);
	}
}
