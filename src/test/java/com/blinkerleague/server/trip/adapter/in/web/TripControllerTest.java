package com.blinkerleague.server.trip.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 운행을_업로드하면_201과_tripId_UPLOADED_상태를_돌려준다() throws Exception {
		String tripId = UUID.randomUUID().toString();

		upload(1L, tripBody(tripId, 12.3))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tripId").value(tripId))
			.andExpect(jsonPath("$.status").value("UPLOADED"));
	}

	@Test
	void 같은_운행을_다시_올리면_200과_처음_저장한_결과를_돌려준다() throws Exception {
		String tripId = UUID.randomUUID().toString();
		upload(1L, tripBody(tripId, 12.3)).andExpect(status().isCreated());

		upload(1L, tripBody(tripId, 1.0))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tripId").value(tripId))
			.andExpect(jsonPath("$.status").value("UPLOADED"));
	}

	private ResultActions upload(Long driverId, String body) throws Exception {
		return mockMvc.perform(post("/api/trips")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(driverId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String tripBody(String tripId, double distanceKm) {
		return """
			{
			  "tripId": "%s",
			  "startedAt": "2026-11-03T08:00:00+09:00",
			  "endedAt": "2026-11-03T08:40:00+09:00",
			  "distanceKm": %s,
			  "supported": ["TURN_SIGNAL"],
			  "events": [
			    { "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:12:30+09:00",
			      "payload": { "direction": "LEFT", "signalOn": true } }
			  ]
			}
			""".formatted(tripId, distanceKm, UUID.randomUUID());
	}
}
