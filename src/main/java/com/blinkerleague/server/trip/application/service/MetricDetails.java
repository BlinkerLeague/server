package com.blinkerleague.server.trip.application.service;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 항목별 근거 수치(detail)를 trip_metric에 JSON으로 저장하고 다시 읽는다. */
final class MetricDetails {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
	};

	private MetricDetails() {
	}

	static String toJson(Map<String, Number> detail) {
		try {
			return MAPPER.writeValueAsString(detail);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("근거 수치를 JSON으로 바꾸지 못했습니다.", e);
		}
	}

	static Map<String, Object> fromJson(String detail) {
		if (detail == null || detail.isBlank()) {
			return Map.of();
		}
		try {
			return MAPPER.readValue(detail, MAP);
		} catch (JsonProcessingException e) {
			return Map.of();
		}
	}
}
