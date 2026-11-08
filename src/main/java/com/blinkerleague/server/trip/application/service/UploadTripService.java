package com.blinkerleague.server.trip.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.score.domain.ScoreCalculator;
import com.blinkerleague.server.score.domain.ScoreResult;
import com.blinkerleague.server.score.domain.TripFacts;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort;
import com.blinkerleague.server.trip.application.port.out.TripBadgePort;
import com.blinkerleague.server.trip.application.port.out.TripMissionPort;
import com.blinkerleague.server.trip.application.port.out.TripPointPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort.TripAlreadyExistsException;
import com.blinkerleague.server.trip.domain.RecordedEvent;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripMetric;
import com.blinkerleague.server.trip.domain.TripStatus;

import lombok.RequiredArgsConstructor;

/** 운행 · 이벤트 업로드(plan.md 3.4). 최소 거리 이상 운행은 업로드하면서 점수를 매긴다(3.5). */
@RequiredArgsConstructor
public class UploadTripService {

	private final LoadTripPort loadTripPort;
	private final SaveTripPort saveTripPort;
	private final ThresholdProvider thresholdProvider;
	private final TripMissionPort tripMissionPort;
	private final TripPointPort tripPointPort;
	private final TripBadgePort tripBadgePort;
	private final ScoreCalculator scoreCalculator = new ScoreCalculator();

	public Result upload(Long driverId, Command command) {
		Thresholds thresholds = thresholdProvider.current().thresholds();
		Optional<Trip> existing = loadTripPort.findById(command.tripId());
		if (existing.isPresent()) {
			return alreadyUploaded(existing.get(), driverId, thresholds);
		}
		Trip trip = Trip.upload(command.tripId(), driverId, command.startedAt(), command.endedAt(),
			command.distanceKm(), command.supportedSignals(), command.events(), thresholds.trip().minDistanceKm());
		if (trip.getStatus() == TripStatus.UPLOADED) {
			ScoreResult score = scoreCalculator.calculate(new TripFacts(trip.getSupportedSignals(), trip.getDistanceKm(),
				DrivingEvents.fromRecorded(command.events())), thresholds);
			trip.applyScore(score.score(), score.metrics().stream()
				.map(m -> new TripMetric(m.metric(), m.status().name(), m.reason(), m.value(), m.score(), m.weight(),
					MetricDetails.toJson(m.detail())))
				.toList());
		}
		try {
			saveTripPort.saveNew(trip);
		} catch (TripAlreadyExistsException e) {
			// 확인과 저장 사이에 같은 tripId가 먼저 저장된 동시 업로드. 먼저 저장된 운행이 기준이다.
			return alreadyUploaded(loadTripPort.findById(command.tripId()).orElseThrow(() -> e), driverId, thresholds);
		}
		return new Result(afterUpload(trip, thresholds), true);
	}

	// 운행 적립 → 그날 미션 판정 · 보상 → 배지 판정 순서다(배지는 연속 달성 · 누적 기록을 본다).
	private TripScoreView afterUpload(Trip trip, Thresholds thresholds) {
		earnPoints(trip);
		TripScoreView view = withMission(TripScoreView.of(trip, thresholds), trip);
		tripBadgePort.awardEarned(trip.getDriverId());
		return view;
	}

	// 점수가 난 운행만 적립한다(EXCLUDED_SHORT · NO_TURN은 점수가 없다).
	private void earnPoints(Trip trip) {
		if (trip.getScore() != null) {
			tripPointPort.earnForTrip(trip.getDriverId(), trip.getId(), trip.getDistanceKm(), trip.getScore());
		}
	}

	// 운행을 반영한 그날 미션 판정을 기록하고 결과에 담는다(plan.md 3.5 · 3.6).
	private TripScoreView withMission(TripScoreView view, Trip trip) {
		return view.withMission(tripMissionPort.recordAfterUpload(trip.getDriverId(), trip.getEndedAt()).orElse(null));
	}

	// 재전송은 처음 저장한 결과를 돌려주고, 남의 운행이면 결과를 알려주지 않고 거부한다.
	private Result alreadyUploaded(Trip existing, Long driverId, Thresholds thresholds) {
		if (!existing.getDriverId().equals(driverId)) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 다른 운전자가 사용한 운행 ID입니다.");
		}
		// 적립은 한 번만 되고 판정 기록은 달성한 날을 되돌리지 않으므로 다시 불러도 안전하다.
		// 처음 업로드에서 적립 · 기록이 실패했어도 재전송으로 복구된다.
		return new Result(afterUpload(existing, thresholds), false);
	}

	public record Command(String tripId, Instant startedAt, Instant endedAt, double distanceKm,
		Set<ScoreItem> supportedSignals, List<RecordedEvent> events) {
	}

	/** created가 false면 이미 업로드된 운행을 다시 받은 것이다. */
	public record Result(TripScoreView trip, boolean created) {

		public String tripId() {
			return trip.tripId();
		}

		public TripStatus status() {
			return trip.status();
		}
	}
}
