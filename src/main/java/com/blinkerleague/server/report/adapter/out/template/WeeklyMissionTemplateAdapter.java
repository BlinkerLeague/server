package com.blinkerleague.server.report.adapter.out.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.report.application.port.out.LoadWeeklyMissionTemplatesPort;
import com.blinkerleague.server.report.domain.WeeklyMissionTemplate;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 항목별 주간 맞춤 미션 틀을 `reports/weekly-missions.json`에서 읽는다. */
@Component
public class WeeklyMissionTemplateAdapter implements LoadWeeklyMissionTemplatesPort {

	static final String PATH = "reports/weekly-missions.json";

	private final Map<ScoreItem, WeeklyMissionTemplate> templates;

	public WeeklyMissionTemplateAdapter(ObjectMapper objectMapper) {
		try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
			this.templates = new EnumMap<>(ScoreItem.class);
			for (WeeklyMissionTemplate template : objectMapper.readValue(in, WeeklyMissionTemplate[].class)) {
				templates.put(template.metric(), template);
			}
		} catch (IOException e) {
			throw new IllegalStateException("주간 미션 틀 파일을 읽지 못했습니다: " + PATH, e);
		}
	}

	@Override
	public Optional<WeeklyMissionTemplate> templateFor(ScoreItem metric) {
		return Optional.ofNullable(templates.get(metric));
	}
}
