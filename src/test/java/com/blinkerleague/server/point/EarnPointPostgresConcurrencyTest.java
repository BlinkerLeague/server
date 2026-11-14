package com.blinkerleague.server.point;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * {@link EarnPointConcurrencyTest}를 운영 · local과 같은 PostgreSQL에서 돌린다(docs/deployment.md 4.5).
 * 운전자 행 {@code SELECT ... FOR UPDATE} 잠금과 유니크 제약이 PostgreSQL에서도 일일 상한을 지키는지 확인한다.
 * 스키마는 Flyway V1으로 만든다. Docker가 필요하다.
 */
@Testcontainers
@TestPropertySource(properties = {
	"spring.jpa.hibernate.ddl-auto=validate",
	"spring.flyway.enabled=true"
})
class EarnPointPostgresConcurrencyTest extends EarnPointConcurrencyTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}
}
