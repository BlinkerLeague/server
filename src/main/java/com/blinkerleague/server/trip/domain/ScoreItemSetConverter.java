package com.blinkerleague.server.trip.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.blinkerleague.server.threshold.domain.ScoreItem;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 지원 신호 집합을 `SMOOTH_DRIVING,TURN_SIGNAL` 같은 문자열 한 컬럼으로 저장한다. */
@Converter
public class ScoreItemSetConverter implements AttributeConverter<Set<ScoreItem>, String> {

	@Override
	public String convertToDatabaseColumn(Set<ScoreItem> items) {
		return items.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
	}

	@Override
	public Set<ScoreItem> convertToEntityAttribute(String column) {
		Set<ScoreItem> items = EnumSet.noneOf(ScoreItem.class);
		if (column != null && !column.isBlank()) {
			Arrays.stream(column.split(",")).map(ScoreItem::valueOf).forEach(items::add);
		}
		return items;
	}
}
