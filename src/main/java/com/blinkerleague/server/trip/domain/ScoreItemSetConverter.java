package com.blinkerleague.server.trip.domain;

import java.util.Set;

import com.blinkerleague.server.threshold.domain.ScoreItem;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 지원 신호 집합을 `TURN_SIGNAL,SMOOTH_DRIVING` 같은 문자열 한 컬럼으로 저장한다. */
@Converter
public class ScoreItemSetConverter implements AttributeConverter<Set<ScoreItem>, String> {

	@Override
	public String convertToDatabaseColumn(Set<ScoreItem> items) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<ScoreItem> convertToEntityAttribute(String column) {
		throw new UnsupportedOperationException();
	}
}
