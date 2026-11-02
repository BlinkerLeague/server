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
			.andExpect(jsonPath("$.turn.signalLeadSec").value(5))
			.andExpect(jsonPath("$.turn.uTurnAngleDeg").value(150))
			.andExpect(jsonPath("$.harsh.deltaKmhPerSec").value(10))
			.andExpect(jsonPath("$.safetyWarning.mergeWindowSec").value(10))
			.andExpect(jsonPath("$.weights.TURN_SIGNAL").value(0.4))
			.andExpect(jsonPath("$.weights.NIGHT_HEADLIGHT").value(0.1))
			.andExpect(jsonPath("$.point.dailyCap").value(150))
			.andExpect(jsonPath("$.grades[0].grade").value("S"))
			.andExpect(jsonPath("$.grades[0].minScore").value(95))
			.andExpect(jsonPath("$.grades[3].grade").value("C"))
			.andExpect(jsonPath("$.grades[3].minScore").value(50));
	}
}
