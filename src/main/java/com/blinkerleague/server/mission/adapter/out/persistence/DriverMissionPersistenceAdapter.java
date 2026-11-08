package com.blinkerleague.server.mission.adapter.out.persistence;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.application.port.out.DriverMissionStore.DriverMissionAlreadyExistsException;
import com.blinkerleague.server.mission.domain.DriverMission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverMissionPersistenceAdapter implements DriverMissionStore {

	private final DriverMissionJpaRepository repository;

	@Override
	public Optional<DriverMission> find(Long driverId, Long missionId, String periodKey) {
		return repository.findByDriverIdAndMissionIdAndPeriodKey(driverId, missionId, periodKey);
	}

	@Override
	public Optional<DriverMission> findLatestBefore(Long driverId, String periodKey) {
		return repository.findFirstByDriverIdAndPeriodKeyLessThanOrderByPeriodKeyDesc(driverId, periodKey);
	}

	@Override
	public void save(DriverMission driverMission) {
		try {
			// 즉시 flush해 같은 날 동시 업로드로 생긴 유니크 충돌을 여기서 잡는다.
			repository.saveAndFlush(driverMission);
		} catch (DataIntegrityViolationException e) {
			if (isUniqueViolation(e)) {
				throw new DriverMissionAlreadyExistsException(driverMission.getPeriodKey(), e);
			}
			throw e;
		}
	}

	private static boolean isUniqueViolation(DataIntegrityViolationException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation.getKind() == ConstraintKind.UNIQUE;
			}
		}
		return false;
	}
}
