package com.blinkerleague.server.trip.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Persistable;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.common.jpa.BaseTimeEntity;
import com.blinkerleague.server.threshold.domain.ScoreItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 운행 한 건. id는 앱이 만든 UUID라 재전송해도 같은 값이다(멱등 처리 기준). 원시 위치 좌표는 두지 않는다.
 * id를 직접 정하므로 Persistable로 새 엔티티임을 알려, 저장 시 merge(덮어쓰기)가 아니라 INSERT로 PK 충돌이 드러나게 한다.
 */
@Getter
@Entity
@Table(name = "trip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity implements Persistable<String> {

	@Id
	@Column(name = "trip_id", length = 36)
	private String id;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "ended_at", nullable = false)
	private Instant endedAt;

	@Column(name = "distance_km", nullable = false)
	private double distanceKm;

	@Convert(converter = ScoreItemSetConverter.class)
	@Column(name = "supported_signals", nullable = false)
	private Set<ScoreItem> supportedSignals = EnumSet.noneOf(ScoreItem.class);

	private Integer score;

	/** 공통 항목만으로 매긴 리그 점수(D19). 공통 항목을 측정하지 않았으면 null이다. */
	@Column(name = "league_score")
	private Integer leagueScore;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TripStatus status;

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("occurredAt")
	private List<TripEvent> events = new ArrayList<>();

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TripMetric> metrics = new ArrayList<>();

	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	/** 업로드된 운행을 만든다. 최소 거리는 기준값 설정에서 받는다(D10). */
	public static Trip upload(String tripId, Long driverId, Instant startedAt, Instant endedAt, double distanceKm,
		Set<ScoreItem> supportedSignals, List<RecordedEvent> events, double minDistanceKm) {
		validateTimes(startedAt, endedAt, events);
		validateUniqueEventIds(events);
		Trip trip = new Trip();
		trip.id = tripId;
		trip.driverId = driverId;
		trip.startedAt = startedAt;
		trip.endedAt = endedAt;
		trip.distanceKm = distanceKm;
		trip.supportedSignals.addAll(supportedSignals);
		trip.status = distanceKm < minDistanceKm ? TripStatus.EXCLUDED_SHORT : TripStatus.UPLOADED;
		events.forEach(event -> trip.events.add(new TripEvent(trip, event)));
		return trip;
	}

	/** 점수 계산 결과를 반영한다. 측정할 항목이 없어 점수가 없으면 NO_TURN이다(plan.md 3.5). */
	public void applyScore(Integer score, List<TripMetric> metrics) {
		this.score = score;
		metrics.forEach(metric -> metric.assignTo(this));
		this.metrics.addAll(metrics);
		this.status = score == null ? TripStatus.NO_TURN : TripStatus.SCORED;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		newEntity = false;
	}

	// 이벤트는 시작 · 종료 시각을 포함한 운행 구간 안에 있어야 한다.
	private static void validateTimes(Instant startedAt, Instant endedAt, List<RecordedEvent> events) {
		if (!endedAt.isAfter(startedAt)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "종료 시각은 시작 시각보다 뒤여야 합니다.");
		}
		boolean outOfTrip = events.stream().map(RecordedEvent::occurredAt)
			.anyMatch(at -> at.isBefore(startedAt) || at.isAfter(endedAt));
		if (outOfTrip) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "이벤트 시각은 운행 구간 안이어야 합니다.");
		}
	}

	// 같은 eventId가 두 번 오면 저장 시 PK 충돌로 500이 되므로 미리 거부한다.
	private static void validateUniqueEventIds(List<RecordedEvent> events) {
		long distinct = events.stream().map(RecordedEvent::eventId).distinct().count();
		if (distinct != events.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "한 운행 안에서 eventId가 중복되었습니다.");
		}
	}
}
