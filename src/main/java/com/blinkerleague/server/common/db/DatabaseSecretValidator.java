package com.blinkerleague.server.common.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * PostgreSQL 접속 비밀번호를 기동 전에 검증한다(CLAUDE.md 보안 규칙: 비밀값은 기본값 없이 fail-fast).
 * 비밀번호가 없으면 DB 연결 오류 대신 설정할 환경변수 이름을 알려 주고 멈춘다. H2(테스트)는 검사하지 않는다.
 * {@code META-INF/spring.factories}에 등록되어 설정 파일(.env 포함)을 읽은 뒤에 실행된다.
 */
public class DatabaseSecretValidator implements EnvironmentPostProcessor {

	static final String ENV_NAME = "DB_PASSWORD";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String url = environment.getProperty("spring.datasource.url", "");
		if (!url.startsWith("jdbc:postgresql:")) {
			return;
		}
		String password = environment.getProperty("spring.datasource.password");
		if (password == null || password.isBlank()) {
			throw new IllegalStateException("DB 비밀번호가 없습니다. 환경변수 " + ENV_NAME + "를 설정하세요.");
		}
	}
}
