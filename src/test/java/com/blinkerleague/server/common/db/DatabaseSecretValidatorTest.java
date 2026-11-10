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

	private static StandardEnvironment environment(MockPropertySource properties) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(properties);
		return environment;
	}
}
