package com.blinkerleague.server.report.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.report.domain.WeeklyMissionTemplate;
import com.blinkerleague.server.threshold.domain.ScoreItem;

public interface LoadWeeklyMissionTemplatesPort {

	Optional<WeeklyMissionTemplate> templateFor(ScoreItem metric);
}
