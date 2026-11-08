package com.blinkerleague.server.point.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.point.application.port.out.DriverPointLockPort;
import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.application.service.EarnPointService;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;

/** point 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class PointBeanConfig {

	@Bean
	public EarnPointService earnPointService(LoadPointLedgerPort loadPointLedgerPort,
		SavePointEntryPort savePointEntryPort, ThresholdProvider thresholdProvider,
		DriverPointLockPort driverPointLockPort, Clock clock) {
		return new EarnPointService(loadPointLedgerPort, savePointEntryPort, thresholdProvider, driverPointLockPort,
			clock);
	}
}
