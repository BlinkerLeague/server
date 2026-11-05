package com.blinkerleague.server.trip.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.blinkerleague.server.support.TestTokens;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void 운행을_업로드하면_201과_tripId_점수를_매긴_상태를_돌려준다() throws Exception {
		String tripId = UUID.randomUUID().toString();

		upload(1L, tripBody(tripId, 12.3))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tripId").value(tripId))
			.andExpect(jsonPath("$.status").value("SCORED"));
	}

	@Test
	void 업로드_응답에_총점_등급_항목별_점수를_담는다() throws Exception {
		upload(1L, tripBody(UUID.randomUUID().toString(), 12.3))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.score").value(100))
			.andExpect(jsonPath("$.grade").value("S"))
			.andExpect(jsonPath("$.metrics.length()").value(1))
			.andExpect(jsonPath("$.metrics[0].metric").value("TURN_SIGNAL"))
			.andExpect(jsonPath("$.metrics[0].value").value(1.0))
			.andExpect(jsonPath("$.metrics[0].score").value(100))
			.andExpect(jsonPath("$.metrics[0].weight").value(1.0));
	}

	@Test
	void 업로드_응답에_회전_수_경기_요약_시각순_경기_기록을_담고_미션은_아직_null이다() throws Exception {
		String body = """
			{
			  "tripId": "%s",
			  "startedAt": "2026-11-03T08:00:00+09:00",
			  "endedAt": "2026-11-03T08:40:00+09:00",
			  "distanceKm": 12.3,
			  "supported": ["TURN_SIGNAL"],
			  "events": [
			    { "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:15:02+09:00",
			      "payload": { "direction": "RIGHT", "signalOn": false } },
			    { "eventId": "%s", "type": "LANE_CHANGE", "occurredAt": "2026-11-03T08:14:00+09:00", "payload": {} },
			    { "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:12:30+09:00",
			      "payload": { "direction": "LEFT", "signalOn": true } }
			  ]
			}
			""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		upload(1L, body)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.turns.total").value(2))
			.andExpect(jsonPath("$.turns.success").value(1))
			.andExpect(jsonPath("$.summary.goals").value(1))
			.andExpect(jsonPath("$.summary.conceded").value(1))
			.andExpect(jsonPath("$.summary.fouls").value(0))
			.andExpect(jsonPath("$.plays.length()").value(3))
			.andExpect(jsonPath("$.plays[0].type").value("TURN"))
			.andExpect(jsonPath("$.plays[0].result").value("GOAL"))
			.andExpect(jsonPath("$.plays[0].occurredAt").value("2026-11-03T08:12:30+09:00"))
			.andExpect(jsonPath("$.plays[1].type").value("LANE_CHANGE"))
			.andExpect(jsonPath("$.plays[1].result").hasJsonPath())
			.andExpect(jsonPath("$.plays[1].result").value(nullValue()))
			.andExpect(jsonPath("$.plays[2].result").value("CONCEDED"))
			.andExpect(jsonPath("$.mission").hasJsonPath())
			.andExpect(jsonPath("$.mission").value(nullValue()));
	}

	@Test
	void 내_운행의_점수를_업로드_응답과_같은_구조로_조회한다() throws Exception {
		String tripId = UUID.randomUUID().toString();
		upload(1L, tripBody(tripId, 12.3)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/trips/{tripId}/score", tripId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tripId").value(tripId))
			.andExpect(jsonPath("$.status").value("SCORED"))
			.andExpect(jsonPath("$.score").value(100))
			.andExpect(jsonPath("$.grade").value("S"))
			.andExpect(jsonPath("$.metrics[0].metric").value("TURN_SIGNAL"))
			.andExpect(jsonPath("$.turns.total").value(1))
			.andExpect(jsonPath("$.summary.goals").value(1))
			.andExpect(jsonPath("$.plays[0].result").value("GOAL"))
			.andExpect(jsonPath("$.mission").hasJsonPath());
	}

	@Test
	void 없는_운행의_점수를_조회하면_404_NOT_FOUND를_반환한다() throws Exception {
		getScore(1L, UUID.randomUUID().toString())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 남의_운행_점수를_조회하면_있는지_알리지_않고_404_NOT_FOUND를_반환한다() throws Exception {
		String tripId = UUID.randomUUID().toString();
		upload(1L, tripBody(tripId, 12.3)).andExpect(status().isCreated());

		getScore(2L, tripId)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 최근_경기_목록은_내_운행만_종료_시각_최신순으로_돌려준다() throws Exception {
		Long me = 1001L;
		String older = UUID.randomUUID().toString();
		String newest = UUID.randomUUID().toString();
		String shortTrip = UUID.randomUUID().toString();
		upload(me, tripBodyEndingAt(older, 12.3, "2026-11-03T08:40:00+09:00")).andExpect(status().isCreated());
		upload(me, tripBodyEndingAt(newest, 12.3, "2026-11-03T18:40:00+09:00")).andExpect(status().isCreated());
		upload(me, tripBodyEndingAt(shortTrip, 2.0, "2026-11-03T12:40:00+09:00")).andExpect(status().isCreated());
		upload(1002L, tripBodyEndingAt(UUID.randomUUID().toString(), 12.3, "2026-11-03T20:40:00+09:00"))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/trips").header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].tripId").value(newest))
			.andExpect(jsonPath("$[0].endedAt").value("2026-11-03T18:40:00+09:00"))
			.andExpect(jsonPath("$[0].distanceKm").value(12.3))
			.andExpect(jsonPath("$[0].status").value("SCORED"))
			.andExpect(jsonPath("$[0].score").value(100))
			.andExpect(jsonPath("$[0].grade").value("S"))
			.andExpect(jsonPath("$[1].tripId").value(shortTrip))
			.andExpect(jsonPath("$[1].status").value("EXCLUDED_SHORT"))
			.andExpect(jsonPath("$[1].score").value(nullValue()))
			.andExpect(jsonPath("$[1].grade").value(nullValue()))
			.andExpect(jsonPath("$[2].tripId").value(older));
	}

	@Test
	void 같은_운행을_다시_올리면_200과_처음_저장한_결과를_돌려준다() throws Exception {
		String tripId = UUID.randomUUID().toString();
		upload(1L, tripBody(tripId, 12.3)).andExpect(status().isCreated());

		upload(1L, tripBody(tripId, 1.0))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tripId").value(tripId))
			.andExpect(jsonPath("$.status").value("SCORED"));
	}

	@Test
	void 기본_기준값의_최소_거리_3km_미만_운행은_EXCLUDED_SHORT로_저장한다() throws Exception {
		upload(1L, tripBody(UUID.randomUUID().toString(), 2.9))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("EXCLUDED_SHORT"));
	}

	@Test
	void 차선_변경_이벤트도_받아서_저장한다() throws Exception {
		String tripId = UUID.randomUUID().toString();
		ObjectNode body = (ObjectNode)objectMapper.readTree(tripBody(tripId, 12.3));
		((ObjectNode)body.at("/events/0")).put("type", "LANE_CHANGE");

		upload(1L, body.toString()).andExpect(status().isCreated());

		assertThat(jdbcTemplate.queryForList("select type from trip_event where trip_id = ?", String.class, tripId))
			.containsExactly("LANE_CHANGE");
	}

	@Test
	void 다른_운전자가_올린_tripId로_업로드하면_409_CONFLICT를_돌려준다() throws Exception {
		String tripId = UUID.randomUUID().toString();
		upload(1L, tripBody(tripId, 12.3)).andExpect(status().isCreated());

		upload(2L, tripBody(tripId, 12.3))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	@Test
	void 한_요청_안에_같은_eventId가_두_번_있으면_400_INVALID_INPUT을_돌려준다() throws Exception {
		ObjectNode body = (ObjectNode)objectMapper.readTree(tripBody(UUID.randomUUID().toString(), 12.3));
		ArrayNode events = (ArrayNode)body.get("events");
		events.add(events.get(0).deepCopy());

		upload(1L, body.toString())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
		"종료 시각이 시작 시각과 같음 | /endedAt           | \"2026-11-03T08:00:00+09:00\"",
		"이벤트가 운행 종료 뒤       | /events/0/occurredAt | \"2026-11-03T08:40:01+09:00\""})
	void 운행_시각_규칙을_어기면_400_INVALID_INPUT을_돌려준다(String caseName, String pointer, String value)
		throws Exception {
		upload(1L, tripBodyWith(pointer, value))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
		"tripId 빈 문자열           | /tripId              | \"\"",
		"tripId 36자 초과            | /tripId              | \"0123456789-0123456789-0123456789-0123\"",
		"startedAt 누락              | /startedAt           | null",
		"distanceKm 누락             | /distanceKm          | null",
		"distanceKm 음수             | /distanceKm          | -0.1",
		"supported 누락              | /supported           | null",
		"supported 안에 null         | /supported           | [\"TURN_SIGNAL\", null]",
		"events 누락                 | /events              | null",
		"이벤트 eventId 누락         | /events/0/eventId    | null",
		"이벤트 occurredAt 누락      | /events/0/occurredAt | null",
		"이벤트 type 알 수 없는 값   | /events/0/type       | \"UNKNOWN\""})
	void 요청_값이_빠지거나_잘못되면_400_INVALID_INPUT을_돌려준다(String caseName, String pointer, String value)
		throws Exception {
		upload(1L, tripBodyWith(pointer, value))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	// 기본 요청 본문에서 pointer 위치의 값만 JSON value로 바꾼다.
	private String tripBodyWith(String pointer, String value) throws Exception {
		ObjectNode body = (ObjectNode)objectMapper.readTree(tripBody(UUID.randomUUID().toString(), 12.3));
		JsonPointer field = JsonPointer.compile(pointer);
		ObjectNode parent = (ObjectNode)body.at(field.head());
		parent.set(field.last().getMatchingProperty(), objectMapper.readTree(value));
		return body.toString();
	}

	private ResultActions getScore(Long driverId, String tripId) throws Exception {
		return mockMvc.perform(get("/api/trips/{tripId}/score", tripId)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(driverId)));
	}

	private ResultActions upload(Long driverId, String body) throws Exception {
		return mockMvc.perform(post("/api/trips")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(driverId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String tripBodyEndingAt(String tripId, double distanceKm, String endedAt) {
		return tripBody(tripId, distanceKm).replace("2026-11-03T08:40:00+09:00", endedAt);
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
