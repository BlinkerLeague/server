package com.blinkerleague.server.common.db;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

class DatabaseSecretValidatorTest {

	private final DatabaseSecretValidator validator = new DatabaseSecretValidator();

	@Test
	void PostgreSQL_비밀번호가_없으면_DB_PASSWORD를_안내하며_기동을_멈춘다() {
		// given
		StandardEnvironment environment = environment(new MockPropertySource()
			.withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/blinkerleague"));

		// when & then
		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, new SpringApplication()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("DB_PASSWORD");
	}

	@Test
	void 환경변수가_치환되지_않은_비밀번호는_미설정으로_보고_DB_PASSWORD를_안내한다() {
		// given
		// .env에도 환경변수에도 DB_PASSWORD가 없어 "${DB_PASSWORD}"가 풀리지 않은 상태
		StandardEnvironment environment = environment(new MockPropertySource()
			.withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/blinkerleague")
			.withProperty("spring.datasource.password", "${DB_PASSWORD_NOT_PROVIDED_PLACEHOLDER}"));

		// when & then
		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, new SpringApplication()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("환경변수 DB_PASSWORD");
	}

	private static StandardEnvironment environment(MockPropertySource properties) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(properties);
		return environment;
	}
}
