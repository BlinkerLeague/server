package com.blinkerleague.server.threshold.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.threshold.application.port.out.LoadThresholdsPort;
import com.blinkerleague.server.threshold.application.port.out.SaveThresholdsPort;
import com.blinkerleague.server.threshold.application.service.ThresholdService;

/** threshold 유스케이스에 포트 구현(어댑터)을 연결한다. ThresholdService는 ThresholdProvider로도 주입된다. */
@Configuration
public class ThresholdBeanConfig {

	@Bean
	public ThresholdService thresholdService(LoadThresholdsPort loadThresholdsPort,
		SaveThresholdsPort saveThresholdsPort) {
		return new ThresholdService(loadThresholdsPort, saveThresholdsPort);
	}
}
