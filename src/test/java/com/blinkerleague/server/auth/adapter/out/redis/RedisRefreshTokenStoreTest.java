package com.blinkerleague.server.auth.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Redis(Testcontainers)에서 저장소 동작을 확인한다. Docker가 필요하다. */
@Testcontainers
class RedisRefreshTokenStoreTest {

	private static final Duration TTL = Duration.ofDays(30);

	@Container
	private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	private static LettuceConnectionFactory connectionFactory;
	private static StringRedisTemplate redis;

	private RedisRefreshTokenStore store;

	@BeforeAll
	static void connect() {
		connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
		connectionFactory.afterPropertiesSet();
		connectionFactory.start();
		redis = new StringRedisTemplate(connectionFactory);
	}

	@AfterAll
	static void disconnect() {
		connectionFactory.destroy();
	}

	@BeforeEach
	void setUp() {
		redis.getConnectionFactory().getConnection().serverCommands().flushAll();
		store = new RedisRefreshTokenStore(redis);
	}

	@Test
	void 저장한_토큰_해시를_꺼내면_driverId를_돌려준다() {
		store.save("hash-1", 7L, TTL);

		assertThat(store.consume("hash-1")).contains(7L);
	}
}
