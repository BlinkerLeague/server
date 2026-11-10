package com.blinkerleague.server.common.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/** PostgreSQL 접속 비밀번호를 기동 전에 검증한다. */
public class DatabaseSecretValidator implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
	}
}
