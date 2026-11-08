package com.blinkerleague.server.report.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

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
@Import(WeeklyReportControllerTest.FixedClock.class)
class WeeklyReportControllerTest {

	// 한국 시간 2026-11-09(월) 09:00 → 지난주는 2026-W45(11-02 ~ 11-08)
	static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-09T00:00:00Z"), ZoneId.of("Asia/Seoul"));

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
	void 지난주_리포트에_항목_비교_가장_약한_항목_다음_주_맞춤_미션을_담고_끝나지_않은_주는_400이다() throws Exception {
		Long driverId = 7301L;
		// 지난주(W45) 운행: 깜빡이 100점, 경고등 1회(20km → 100km당 5회 → 0점)
		trip(driverId, "2026-11-03", 20.0, true, 1).andExpect(status().isCreated());
		// 지지난주(W44) 운행: 깜빡이 0점
		trip(driverId, "2026-10-27", 20.0, false, 0).andExpect(status().isCreated());

		mockMvc.perform(get("/api/reports/weekly").header(HttpHeaders.AUTHORIZATION, bearer(driverId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.week").value("2026-W45"))
			.andExpect(jsonPath("$.trips").value(1))
			.andExpect(jsonPath("$.distanceKm").value(20.0))
			.andExpect(jsonPath("$.metrics[0].metric").value("TURN_SIGNAL"))
			.andExpect(jsonPath("$.metrics[0].thisWeek").value(100))
			.andExpect(jsonPath("$.metrics[0].lastWeek").value(0))
			.andExpect(jsonPath("$.metrics[0].change").value(100))
			.andExpect(jsonPath("$.weakestMetric").value("SAFETY_WARNING"))
			.andExpect(jsonPath("$.nextMission.week").value("2026-W46"))
			.andExpect(jsonPath("$.nextMission.title").value("안전 경고 없는 주"))
			.andExpect(jsonPath("$.nextMission.status").value("ASSIGNED"));

		mockMvc.perform(get("/api/reports/weekly").param("week", "2026-W46")
				.header(HttpHeaders.AUTHORIZATION, bearer(driverId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	private ResultActions trip(Long driverId, String date, double distanceKm, boolean signalOn, int warnings)
		throws Exception {
		StringBuilder events = new StringBuilder("""
			{ "eventId": "%s", "type": "TURN", "occurredAt": "%sT08:10:00+09:00",
			  "payload": { "direction": "LEFT", "signalOn": %s } }""".formatted(UUID.randomUUID(), date, signalOn));
		for (int i = 0; i < warnings; i++) {
			events.append(", { \"eventId\": \"%s\", \"type\": \"SAFETY_WARNING\", \"occurredAt\": \"%sT08:2%d:00+09:00\" }"
				.formatted(UUID.randomUUID(), date, i));
		}
		String body = """
			{ "tripId": "%s", "startedAt": "%sT08:00:00+09:00", "endedAt": "%sT08:40:00+09:00",
			  "distanceKm": %s, "supported": ["TURN_SIGNAL", "SAFETY_WARNING"], "events": [%s] }
			""".formatted(UUID.randomUUID(), date, date, distanceKm, events);
		return mockMvc.perform(post("/api/trips").header(HttpHeaders.AUTHORIZATION, bearer(driverId))
			.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String bearer(Long driverId) {
		return "Bearer " + TestTokens.accessToken(TestTokens.JWT, driverId, CLOCK);
	}
}
