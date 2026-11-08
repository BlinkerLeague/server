package com.blinkerleague.server.point.adapter.out.persistence;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.blinkerleague.server.point.application.port.out.DriverPointLockPort;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * 운전자 행을 SELECT ... FOR UPDATE로 잠근 트랜잭션 안에서 적립을 실행한다.
 * 같은 운전자의 다른 적립은 커밋될 때까지 기다리므로, 오늘 합계 조회 · 저장이 한 번에 하나씩 일어난다.
 */
@Component
@RequiredArgsConstructor
public class DriverPointLockAdapter implements DriverPointLockPort {

	private final TransactionTemplate transactionTemplate;
	private final EntityManager entityManager;

	@Override
	public <T> T withDriverLock(Long driverId, Supplier<T> work) {
		return transactionTemplate.execute(status -> {
			entityManager.createNativeQuery("select driver_id from driver where driver_id = :driverId for update")
				.setParameter("driverId", driverId)
				.getResultList();
			return work.get();
		});
	}
}
