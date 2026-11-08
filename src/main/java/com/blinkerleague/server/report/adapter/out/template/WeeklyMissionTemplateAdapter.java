package com.blinkerleague.server.report.adapter.out.template;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.report.application.port.out.LoadWeeklyMissionTemplatesPort;
import com.blinkerleague.server.report.domain.WeeklyMissionTemplate;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 항목별 주간 맞춤 미션 틀을 `reports/weekly-missions.json`에서 읽는다. */
@Component
public class WeeklyMissionTemplateAdapter implements LoadWeeklyMissionTemplatesPort {

	static final String PATH = "reports/weekly-missions.json";

	public WeeklyMissionTemplateAdapter(ObjectMapper objectMapper) {
	}

	@Override
	public Optional<WeeklyMissionTemplate> templateFor(ScoreItem metric) {
		throw new UnsupportedOperationException();
	}
}
