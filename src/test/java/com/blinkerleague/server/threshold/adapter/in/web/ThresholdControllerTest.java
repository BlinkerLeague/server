package com.blinkerleague.server.threshold.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ThresholdControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 서버_시작_시_시드된_기본_기준값을_version_1로_인증없이_조회한다() throws Exception {
		mockMvc.perform(get("/api/config/thresholds"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(1))
			.andExpect(jsonPath("$.trip.minDistanceKm").value(1.0))
			.andExpect(jsonPath("$.trip.parkEndMinutes").value(3))
			.andExpect(jsonPath("$.trip.maxEvents").value(2000))
			.andExpect(jsonPath("$.turn.signalLeadSec").value(5))
			.andExpect(jsonPath("$.turn.uTurnAngleDeg").value(150))
			.andExpect(jsonPath("$.harsh.deltaKmhPerSec").value(10))
			.andExpect(jsonPath("$.safetyWarning.mergeWindowSec").value(10))
			.andExpect(jsonPath("$.weights.TURN_SIGNAL").value(0.25))
			.andExpect(jsonPath("$.weights.SIGNAL_CANCEL").value(0.05))
			.andExpect(jsonPath("$.weights.TURN_SPEED").value(0.05))
			.andExpect(jsonPath("$.weights.SMOOTH_DRIVING").value(0.15))
			.andExpect(jsonPath("$.weights.SEAT_BELT").value(0.05))
			.andExpect(jsonPath("$.weights.SAFETY_WARNING").value(0.05))
			.andExpect(jsonPath("$.weights.NIGHT_HEADLIGHT").value(0.05))
			.andExpect(jsonPath("$.weights.RAIN_DRIVING").value(0.05))
			.andExpect(jsonPath("$.minSamples.TURN_SIGNAL").value(1))
			.andExpect(jsonPath("$.turn.signalLeadDistanceM").value(30))
			.andExpect(jsonPath("$.turn.steeringEnterDeg").value(90))
			.andExpect(jsonPath("$.turn.steeringExitDeg").value(20))
			.andExpect(jsonPath("$.turn.mergeGapSec").value(3))
			.andExpect(jsonPath("$.turn.steeringRightPositive").value(false))
			.andExpect(jsonPath("$.turn.lateSignalScore").value(0.5))
			.andExpect(jsonPath("$.turn.turnSpeedKmh").value(30))
			.andExpect(jsonPath("$.harsh.penaltyPer10Km").value(10.0))
			.andExpect(jsonPath("$.safetyWarning.penaltyPer100Km").value(20.0))
			.andExpect(jsonPath("$.signalCancel.forgetSec").value(20))
			.andExpect(jsonPath("$.signalCancel.minSpeedKmh").value(30))
			.andExpect(jsonPath("$.signalCancel.penalty").value(25))
			.andExpect(jsonPath("$.seatBelt.departurePenalty").value(50))
			.andExpect(jsonPath("$.seatBelt.perMinutePenalty").value(10))
			.andExpect(jsonPath("$.rain.minWiperSec").value(30))
			.andExpect(jsonPath("$.rain.speedRatio").value(0.8))
			.andExpect(jsonPath("$.point.dailyCap").value(150))
			.andExpect(jsonPath("$.point.streakRewardDays").value(7))
			.andExpect(jsonPath("$.point.streakRewardPoint").value(50))
			.andExpect(jsonPath("$.grades[0].grade").value("S"))
			.andExpect(jsonPath("$.grades[0].minScore").value(95))
			.andExpect(jsonPath("$.grades[3].grade").value("C"))
			.andExpect(jsonPath("$.grades[3].minScore").value(50));
	}

	@Test
	void 기준값에_점수_항목별_개선_팁_문구가_모두_포함된다() throws Exception {
		mockMvc.perform(get("/api/config/thresholds"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tips.TURN_SIGNAL").value("회전 30m 전에 깜빡이를 켜 보세요."))
			.andExpect(jsonPath("$.tips.SMOOTH_DRIVING").isNotEmpty())
			.andExpect(jsonPath("$.tips.SAFETY_WARNING").isNotEmpty())
			.andExpect(jsonPath("$.tips.NIGHT_HEADLIGHT").isNotEmpty())
			.andExpect(jsonPath("$.tips.SIGNAL_CANCEL").isNotEmpty())
			.andExpect(jsonPath("$.tips.TURN_SPEED").isNotEmpty())
			.andExpect(jsonPath("$.tips.SEAT_BELT").isNotEmpty())
			.andExpect(jsonPath("$.tips.RAIN_DRIVING").isNotEmpty());
	}
}
