package com.blinkerleague.server.trip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.trip.application.port.out.LoadRecentTripsPort;
import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort;
import com.blinkerleague.server.trip.application.port.out.TripBadgePort;
import com.blinkerleague.server.trip.application.port.out.TripMissionPort;
import com.blinkerleague.server.trip.application.port.out.TripPointPort;
import com.blinkerleague.server.trip.application.service.GetTripScoreService;
import com.blinkerleague.server.trip.application.service.ListRecentTripsService;
import com.blinkerleague.server.trip.application.service.UploadTripService;

/** trip 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class TripBeanConfig {

	@Bean
	public UploadTripService uploadTripService(LoadTripPort loadTripPort, SaveTripPort saveTripPort,
		ThresholdProvider thresholdProvider, TripMissionPort tripMissionPort, TripPointPort tripPointPort,
		TripBadgePort tripBadgePort) {
		return new UploadTripService(loadTripPort, saveTripPort, thresholdProvider, tripMissionPort, tripPointPort,
			tripBadgePort);
	}

	@Bean
	public GetTripScoreService getTripScoreService(LoadTripPort loadTripPort, ThresholdProvider thresholdProvider,
		TripMissionPort tripMissionPort) {
		return new GetTripScoreService(loadTripPort, thresholdProvider, tripMissionPort);
	}

	@Bean
	public ListRecentTripsService listRecentTripsService(LoadRecentTripsPort loadRecentTripsPort,
		ThresholdProvider thresholdProvider) {
		return new ListRecentTripsService(loadRecentTripsPort, thresholdProvider);
	}
}
