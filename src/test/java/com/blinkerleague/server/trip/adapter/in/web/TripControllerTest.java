package com.blinkerleague.server.trip.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
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
			.andExpect(jsonPath("$.metrics[?(@.status == 'SCORED')].metric").value(org.hamcrest.Matchers.contains("TURN_SIGNAL")))
			.andExpect(jsonPath("$.metrics[?(@.metric == 'TURN_SIGNAL')].value").value(org.hamcrest.Matchers.contains(1.0)))
			.andExpect(jsonPath("$.metrics[?(@.metric == 'TURN_SIGNAL')].score").value(org.hamcrest.Matchers.contains(100)))
			.andExpect(jsonPath("$.metrics[?(@.metric == 'TURN_SIGNAL')].weight").value(org.hamcrest.Matchers.contains(1.0)));
	}

	@Test
	void 업로드와_점수_조회는_측정_못_한_항목도_상태_사유와_근거_수치를_담는다() throws Exception {
		String tripId = UUID.randomUUID().toString();
		String body = """
			{ "tripId": "%s", "startedAt": "2026-11-03T08:00:00+09:00", "endedAt": "2026-11-03T08:40:00+09:00",
			  "distanceKm": 10.0, "supported": ["TURN_SIGNAL", "SMOOTH_DRIVING", "NIGHT_HEADLIGHT"],
			  "events": [
			    { "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T08:10:00+09:00",
			      "payload": { "direction": "LEFT", "signalOn": true, "signalTiming": "LATE" } },
			    { "eventId": "%s", "type": "HARSH_BRAKE", "occurredAt": "2026-11-03T08:20:00+09:00", "payload": {} } ] }
			""".formatted(tripId, UUID.randomUUID(), UUID.randomUUID());

		for (ResultActions response : List.of(upload(1L, body), getScore(1L, tripId))) {
			response
				.andExpect(jsonPath("$.metrics[?(@.metric == 'TURN_SIGNAL')].status").value(org.hamcrest.Matchers.contains("SCORED")))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'TURN_SIGNAL')].score").value(org.hamcrest.Matchers.contains(50)))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'TURN_SIGNAL')].detail.late").value(org.hamcrest.Matchers.contains(1)))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'SMOOTH_DRIVING')].detail.per10Km").value(org.hamcrest.Matchers.contains(1.0)))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'NIGHT_HEADLIGHT')].status").value(org.hamcrest.Matchers.contains("NOT_APPLICABLE")))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'NIGHT_HEADLIGHT')].reason").value(org.hamcrest.Matchers.contains("NO_NIGHT")))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'NIGHT_HEADLIGHT')].score").value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'SEAT_BELT')].status").value(org.hamcrest.Matchers.contains("UNSUPPORTED")))
				.andExpect(jsonPath("$.metrics[?(@.metric == 'SEAT_BELT')].reason").value(org.hamcrest.Matchers.contains("SIGNAL_NOT_SUPPORTED")));
		}
	}

	@Test
	void 새_이벤트와_항목을_받아_D18_가중치로_평점한다() throws Exception {
		String body = """
			{ "tripId": "%s", "startedAt": "2026-11-03T20:00:00+09:00", "endedAt": "2026-11-03T20:40:00+09:00",
			  "distanceKm": 20.0,
			  "supported": ["TURN_SIGNAL", "SIGNAL_CANCEL", "TURN_SPEED", "SMOOTH_DRIVING", "SEAT_BELT",
			                "SAFETY_WARNING", "NIGHT_HEADLIGHT", "RAIN_DRIVING"],
			  "events": [
			    { "eventId": "%s", "type": "TURN", "occurredAt": "2026-11-03T20:10:00+09:00",
			      "payload": { "direction": "LEFT", "signalOn": true, "signalTiming": "ON_TIME", "maxSpeedKmh": 25 } },
			    { "eventId": "%s", "type": "SIGNAL_LEFT_ON", "occurredAt": "2026-11-03T20:15:00+09:00", "payload": { "durationSec": 40 } },
			    { "eventId": "%s", "type": "SEAT_BELT", "occurredAt": "2026-11-03T20:00:00+09:00",
			      "payload": { "buckledAtDeparture": false, "unbuckledMovingSec": 0 } },
			    { "eventId": "%s", "type": "NIGHT_SEGMENT", "occurredAt": "2026-11-03T20:00:00+09:00",
			      "payload": { "durationSec": 2400, "headlightOffSec": 0 } },
			    { "eventId": "%s", "type": "RAIN_SEGMENT", "occurredAt": "2026-11-03T20:20:00+09:00",
			      "payload": { "durationSec": 600, "headlightOffSec": 0, "knownLimitSec": 0, "overRainLimitSec": 0 } } ] }
			""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID());

		// 깜빡이 100 · 끄기 75 · 회전 속도 100 · 급가감속 100 · 벨트 50 · 경고등 100 · 전조등 100 · 우천 100
		// 가중치 0.25 · 0.05 · 0.05 · 0.15 · 0.05 · 0.05 · 0.05 · 0.05 (합 0.7로 나눔) → (70 − 1.25 − 2.5) ÷ 0.7 = 94.6
		upload(1L, body)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("SCORED"))
			.andExpect(jsonPath("$.score").value(95))
			.andExpect(jsonPath("$.metrics[?(@.status == 'SCORED')]").value(org.hamcrest.Matchers.hasSize(8)))
			.andExpect(jsonPath("$.metrics[?(@.metric == 'SIGNAL_CANCEL')].score").value(org.hamcrest.Matchers.contains(75)))
			.andExpect(jsonPath("$.metrics[?(@.metric == 'SEAT_BELT')].score").value(org.hamcrest.Matchers.contains(50)));
	}

	@Test
	void 업로드_응답에_회전_수_경기_요약_시각순_경기_기록을_담는다() throws Exception {
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
			.andExpect(jsonPath("$.plays[0].direction").value("LEFT"))
			.andExpect(jsonPath("$.plays[1].direction").value(nullValue()))
			.andExpect(jsonPath("$.plays[2].direction").value("RIGHT"))
			.andExpect(jsonPath("$.plays[1].type").value("LANE_CHANGE"))
			.andExpect(jsonPath("$.plays[1].result").hasJsonPath())
			.andExpect(jsonPath("$.plays[1].result").value(nullValue()))
			.andExpect(jsonPath("$.plays[2].result").value("CONCEDED"));
	}

	@Test
	void 업로드_응답에_운행이_끝난_날의_미션_판정을_담는다() throws Exception {
		// 다른 테스트의 운행과 섞이지 않도록 새 운전자로 올린다. 기본 미션: 한 라운드에서 회전 3회 이상 · 깜빡이 100%
		Long driverId = 900_000L + new java.util.Random().nextInt(100_000);

		upload(driverId, threePerfectTurns(UUID.randomUUID().toString()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.mission.missionId").isNumber())
			.andExpect(jsonPath("$.mission.status").value("ACHIEVED"));
	}

	@Test
	void 운행_점수_조회에도_그날_미션_판정을_담는다() throws Exception {
		Long driverId = 900_000L + new java.util.Random().nextInt(100_000);
		String tripId = UUID.randomUUID().toString();
		upload(driverId, threePerfectTurns(tripId)).andExpect(status().isCreated());

		getScore(driverId, tripId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mission.missionId").isNumber())
			.andExpect(jsonPath("$.mission.status").value("ACHIEVED"));
	}

	private String threePerfectTurns(String tripId) throws Exception {
		ObjectNode body = (ObjectNode) objectMapper.readTree(tripBody(tripId, 12.3));
		ArrayNode events = body.putArray("events");
		for (int i = 0; i < 3; i++) {
			ObjectNode turn = events.addObject();
			turn.put("eventId", UUID.randomUUID().toString()).put("type", "TURN")
				.put("occurredAt", "2026-11-03T08:1" + i + ":00+09:00")
				.putObject("payload").put("direction", "LEFT").put("signalOn", true);
		}
		return objectMapper.writeValueAsString(body);
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
	void 최근_경기_목록은_limit만큼만_돌려준다() throws Exception {
		Long me = 1003L;
		for (int hour = 10; hour <= 12; hour++) {
			upload(me, tripBodyEndingAt(UUID.randomUUID().toString(), 12.3, "2026-11-03T%d:00:00+09:00".formatted(hour)))
				.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/trips").param("limit", "2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].endedAt").value("2026-11-03T12:00:00+09:00"));
	}

	@ParameterizedTest(name = "limit={0}")
	@CsvSource({"0", "-1", "abc"})
	void 최근_경기_목록_limit이_1_이상의_숫자가_아니면_400_INVALID_INPUT을_반환한다(String limit) throws Exception {
		mockMvc.perform(get("/api/trips").param("limit", limit)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(1L)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 회전_300회_운행도_업로드와_점수_계산을_3초_안에_끝낸다() throws Exception {
		ObjectNode body = (ObjectNode)objectMapper.readTree(tripBody(UUID.randomUUID().toString(), 120.0));
		ArrayNode events = body.putArray("events");
		for (int i = 0; i < 300; i++) {
			events.addObject()
				.put("eventId", UUID.randomUUID().toString())
				.put("type", "TURN")
				.put("occurredAt", "2026-11-03T08:%02d:%02d+09:00".formatted(1 + i / 60, i % 60))
				.putObject("payload").put("direction", "LEFT").put("signalOn", i % 4 != 0);
		}

		assertTimeoutPreemptively(Duration.ofSeconds(3), () -> upload(1L, body.toString())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.score").value(75)));
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
	void 기본_기준값에서_1km_이상_3km_미만_운행도_경기에_참여한다() throws Exception {
		upload(1L, tripBody(UUID.randomUUID().toString(), 2.0))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("SCORED"));
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
