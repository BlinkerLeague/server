package com.blinkerleague.server.badge.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.badge.application.port.out.BadgeAwardStore;
import com.blinkerleague.server.badge.application.port.out.BadgeProgressPort;
import com.blinkerleague.server.badge.application.port.out.LoadBadgeCatalogPort;
import com.blinkerleague.server.badge.application.port.out.SaveBadgeCatalogPort;
import com.blinkerleague.server.badge.application.service.AwardBadgesService;
import com.blinkerleague.server.badge.application.service.GetBadgesService;
import com.blinkerleague.server.badge.application.service.SeedBadgeService;

/** badge 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class BadgeBeanConfig {

	@Bean
	public AwardBadgesService awardBadgesService(LoadBadgeCatalogPort loadBadgeCatalogPort,
		BadgeProgressPort badgeProgressPort, BadgeAwardStore badgeAwardStore, Clock clock) {
		return new AwardBadgesService(loadBadgeCatalogPort, badgeProgressPort, badgeAwardStore, clock);
	}

	@Bean
	public GetBadgesService getBadgesService(LoadBadgeCatalogPort loadBadgeCatalogPort,
		BadgeAwardStore badgeAwardStore) {
		return new GetBadgesService(loadBadgeCatalogPort, badgeAwardStore);
	}

	@Bean
	public SeedBadgeService seedBadgeService(SaveBadgeCatalogPort saveBadgeCatalogPort) {
		return new SeedBadgeService(saveBadgeCatalogPort);
	}
}
