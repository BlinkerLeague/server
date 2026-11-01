package com.blinkerleague.server.auth.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.blinkerleague.server.auth.adapter.out.jwt.JwtAccessTokenIssuer;
import com.blinkerleague.server.auth.adapter.out.redis.RedisRefreshTokenStore;
import com.blinkerleague.server.auth.application.port.out.AccessTokenIssuer;
import com.blinkerleague.server.auth.application.port.out.RefreshTokenStore;
import com.blinkerleague.server.auth.application.service.RefreshTokenService;
import com.blinkerleague.server.auth.application.service.TokenReissueService;
import com.blinkerleague.server.security.jwt.JwtProperties;

/** auth 유스케이스에 포트 구현(어댑터)을 연결한다. application 계층은 Spring에 의존하지 않는다. */
@Configuration
public class AuthBeanConfig {

	@Bean
	public RefreshTokenStore refreshTokenStore(StringRedisTemplate redis) {
		return new RedisRefreshTokenStore(redis);
	}

	@Bean
	public AccessTokenIssuer accessTokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		return new JwtAccessTokenIssuer(jwtEncoder, properties, clock);
	}

	@Bean
	public RefreshTokenService refreshTokenService(RefreshTokenStore store, JwtProperties properties) {
		return new RefreshTokenService(store, properties.refreshTtl());
	}

	@Bean
	public TokenReissueService tokenReissueService(RefreshTokenService refreshTokenService,
		AccessTokenIssuer accessTokenIssuer) {
		return new TokenReissueService(refreshTokenService, accessTokenIssuer);
	}
}
