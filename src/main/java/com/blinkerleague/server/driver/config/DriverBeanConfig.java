package com.blinkerleague.server.driver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.auth.application.port.out.AccessTokenIssuer;
import com.blinkerleague.server.auth.application.service.RefreshTokenService;
import com.blinkerleague.server.driver.adapter.out.auth.AuthDriverTokensAdapter;
import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort;
import com.blinkerleague.server.driver.application.port.out.LoadDriverPort;
import com.blinkerleague.server.driver.application.port.out.SaveDriverPort;
import com.blinkerleague.server.driver.application.service.ChangeLeagueVisibilityService;
import com.blinkerleague.server.driver.application.service.RegisterDriverService;

/** driver 유스케이스에 포트 구현(어댑터)을 연결한다. application 계층은 Spring에 의존하지 않는다. */
@Configuration
public class DriverBeanConfig {

	@Bean
	public IssueDriverTokensPort issueDriverTokensPort(AccessTokenIssuer accessTokenIssuer,
		RefreshTokenService refreshTokenService) {
		return new AuthDriverTokensAdapter(accessTokenIssuer, refreshTokenService);
	}

	@Bean
	public ChangeLeagueVisibilityService changeLeagueVisibilityService(LoadDriverPort loadDriverPort,
		SaveDriverPort saveDriverPort) {
		return new ChangeLeagueVisibilityService(loadDriverPort, saveDriverPort);
	}

	@Bean
	public RegisterDriverService registerDriverService(LoadDriverPort loadDriverPort, SaveDriverPort saveDriverPort,
		IssueDriverTokensPort issueDriverTokensPort) {
		return new RegisterDriverService(loadDriverPort, saveDriverPort, issueDriverTokensPort);
	}
}
