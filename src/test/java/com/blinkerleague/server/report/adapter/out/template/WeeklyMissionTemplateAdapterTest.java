package com.blinkerleague.server.report.adapter.out.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.report.domain.WeeklyMissionTemplate;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.fasterxml.jackson.databind.ObjectMapper;

class WeeklyMissionTemplateAdapterTest {

	private final WeeklyMissionTemplateAdapter adapter = new WeeklyMissionTemplateAdapter(new ObjectMapper());

	@Test
	void 모든_점수_항목의_주간_맞춤_미션_틀을_파일에서_읽는다() {
		assertThat(adapter.templateFor(ScoreItem.SAFETY_WARNING))
			.contains(new WeeklyMissionTemplate(ScoreItem.SAFETY_WARNING, "안전 경고 없는 주", 100, 50.0));
		assertThat(Arrays.stream(ScoreItem.values()))
			.allSatisfy(item -> assertThat(adapter.templateFor(item)).isPresent());
	}
}
