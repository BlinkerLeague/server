package com.blinkerleague.server.mission.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 미션 조건을 `{"requiredTurns":3,"requiredSignalRate":100}` 같은 JSON 한 컬럼으로 저장한다. */
@Converter
public class TurnSignalConditionConverter implements AttributeConverter<TurnSignalCondition, String> {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(TurnSignalCondition condition) {
		try {
			return MAPPER.writeValueAsString(condition);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("미션 조건을 JSON으로 바꾸지 못했습니다.", e);
		}
	}

	@Override
	public TurnSignalCondition convertToEntityAttribute(String column) {
		try {
			return MAPPER.readValue(column, TurnSignalCondition.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("저장된 미션 조건 JSON이 올바르지 않습니다.", e);
		}
	}
}
