package com.blinkerleague.server.trip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.trip.application.port.out.LoadRecentTripsPort;
import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.application.port.out.SaveTripPort;
import com.blinkerleague.server.trip.application.port.out.TripMissionPort;
import com.blinkerleague.server.trip.application.service.GetTripScoreService;
import com.blinkerleague.server.trip.application.service.ListRecentTripsService;
import com.blinkerleague.server.trip.application.service.UploadTripService;

/** trip 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class TripBeanConfig {

	@Bean
	public UploadTripService uploadTripService(LoadTripPort loadTripPort, SaveTripPort saveTripPort,
		ThresholdProvider thresholdProvider, TripMissionPort tripMissionPort) {
		return new UploadTripService(loadTripPort, saveTripPort, thresholdProvider, tripMissionPort);
	}

	@Bean
	public GetTripScoreService getTripScoreService(LoadTripPort loadTripPort, ThresholdProvider thresholdProvider) {
		return new GetTripScoreService(loadTripPort, thresholdProvider);
	}

	@Bean
	public ListRecentTripsService listRecentTripsService(LoadRecentTripsPort loadRecentTripsPort,
		ThresholdProvider thresholdProvider) {
		return new ListRecentTripsService(loadRecentTripsPort, thresholdProvider);
	}
}
