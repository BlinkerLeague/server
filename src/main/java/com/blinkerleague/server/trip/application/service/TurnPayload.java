package com.blinkerleague.server.trip.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** TURN 이벤트 payload에서 깜빡이를 켰는지(signalOn) 읽는다. */
final class TurnPayload {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TurnPayload() {
	}

	static boolean signalOn(String payload) {
		try {
			return MAPPER.readTree(payload).path("signalOn").asBoolean();
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
