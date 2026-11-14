package com.blinkerleague.server.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.point.application.service.EarnPointService;

/** 같은 운전자의 동시 적립에도 일일 상한(기본 150P)을 지키는지 실제 DB로 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
class EarnPointConcurrencyTest {

	private static final int THREADS = 8;

	@Autowired
	EarnPointService earnPointService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private Long driverId;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("delete from point_ledger where driver_id = ?", driverId);
		jdbcTemplate.update("delete from driver where driver_id = ?", driverId);
	}

	@Test
	void 같은_운전자의_여러_운행을_동시에_적립해도_일일_상한을_넘지_않는다() throws Exception {
		driverId = registerDriver();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		List<Future<Integer>> results = new ArrayList<>();
		for (int i = 0; i < THREADS; i++) {
			String tripId = UUID.randomUUID().toString();
			Callable<Integer> earn = () -> {
				start.await();
				return earnPointService.earnForTrip(driverId, tripId, 50.0, 100);
			};
			results.add(pool.submit(earn));
		}

		start.countDown();
		int earned = 0;
		for (Future<Integer> result : results) {
			earned += result.get();
		}
		pool.shutdown();

		Integer stored = jdbcTemplate.queryForObject(
			"select coalesce(sum(amount), 0) from point_ledger where driver_id = ?", Integer.class, driverId);
		assertThat(earned).isEqualTo(150);
		assertThat(stored).isEqualTo(150);
	}

	private Long registerDriver() {
		String nickname = "p" + UUID.randomUUID().toString().substring(0, 8);
		jdbcTemplate.update("insert into driver (nickname, region_code, created_at, updated_at) "
			+ "values (?, '11680', current_timestamp, current_timestamp)", nickname);
		return jdbcTemplate.queryForObject("select driver_id from driver where nickname = ?", Long.class, nickname);
	}
}
