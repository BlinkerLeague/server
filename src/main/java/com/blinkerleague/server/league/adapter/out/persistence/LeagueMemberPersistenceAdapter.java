package com.blinkerleague.server.league.adapter.out.persistence;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.out.LeagueMemberStore;
import com.blinkerleague.server.league.domain.LeagueMember;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LeagueMemberPersistenceAdapter implements LeagueMemberStore {

	private final LeagueMemberJpaRepository repository;

	@Override
	public Optional<LeagueMember> find(String seasonId, Long driverId) {
		return repository.findBySeasonIdAndDriverId(seasonId, driverId);
	}

	@Override
	public void saveIfAbsent(LeagueMember member) {
		if (find(member.getSeasonId(), member.getDriverId()).isPresent()) {
			return;
		}
		try {
			repository.saveAndFlush(member);
		} catch (DataIntegrityViolationException e) {
			// 같은 운전자 · 시즌의 첫 운행이 동시에 올라와 먼저 저장됐다. 그 소속이 기준이다(D12).
			if (!isUniqueViolation(e)) {
				throw e;
			}
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
