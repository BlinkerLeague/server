package com.blinkerleague.server.trip.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** TURN 이벤트 payload에서 깜빡이를 켰는지 읽는다. signalOn이 boolean true일 때만 켠 회전이다(plan.md D23). */
final class TurnPayload {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TurnPayload() {
	}

	static boolean signalOn(String payload) {
		if (payload == null) {
			return false;
		}
		try {
			JsonNode signalOn = MAPPER.readTree(payload).path("signalOn");
			return signalOn.isBoolean() && signalOn.booleanValue();
		} catch (JsonProcessingException e) {
			return false;
		}
	}
}
