package com.blinkerleague.server.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * 실제 Redis가 필요한 통합 테스트의 상위 클래스. Docker가 필요하다.
 * 컨테이너 하나를 테스트 JVM 전체에서 공유하고(종료 시 Ryuk가 정리), 같은 접속 설정이라 Spring 컨텍스트도 재사용된다.
 */
public abstract class RedisContainerTest {

	private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	static {
		REDIS.start();
	}

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}
}
