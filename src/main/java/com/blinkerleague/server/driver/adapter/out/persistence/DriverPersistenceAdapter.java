package com.blinkerleague.server.driver.adapter.out.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.driver.application.port.out.LoadDriverPort;
import com.blinkerleague.server.driver.application.port.out.SaveDriverPort;
import com.blinkerleague.server.driver.domain.Driver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverPersistenceAdapter implements SaveDriverPort, LoadDriverPort {

	private final DriverJpaRepository repository;

	@Override
	public Driver save(Driver driver) {
		try {
			// 즉시 flush해 동시 등록으로 생긴 닉네임 유니크 제약 위반을 여기서 잡는다.
			return repository.saveAndFlush(driver);
		} catch (DataIntegrityViolationException e) {
			if (isUniqueViolation(e)) {
				throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다.");
			}
			throw e;
		}
	}

	@Override
	public boolean existsByNickname(String nickname) {
		return repository.existsByNickname(nickname);
	}

	// driver 테이블의 유니크 제약은 닉네임 하나뿐이다.
	private static boolean isUniqueViolation(DataIntegrityViolationException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation.getKind() == ConstraintKind.UNIQUE;
			}
		}
		return false;
	}
}
