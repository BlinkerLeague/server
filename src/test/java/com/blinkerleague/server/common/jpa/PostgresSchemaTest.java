package com.blinkerleague.server.common.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 운영 · local과 같은 PostgreSQL에서 Flyway 마이그레이션이 엔티티와 맞는지 확인한다(D3, D37).
 * 다른 테스트는 H2를 쓰므로, 스키마 어긋남은 이 테스트가 잡는다. Docker가 필요하다.
 */
@DataJpaTest(properties = {
	"spring.jpa.hibernate.ddl-auto=validate",
	"spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PostgresSchemaTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void Flyway_마이그레이션_뒤_엔티티_스키마_검증을_통과하고_Flyway_이력이_남는다() {
		// when
		List<String> applied = jdbcTemplate.queryForList(
			"select version from flyway_schema_history where success = true order by installed_rank", String.class);

		// then
		assertThat(applied).contains("1");
	}
}
