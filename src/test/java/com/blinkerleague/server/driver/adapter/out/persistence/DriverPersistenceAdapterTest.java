package com.blinkerleague.server.driver.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.driver.domain.Driver;

@DataJpaTest
@ActiveProfiles("test")
@Import({DriverPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class DriverPersistenceAdapterTest {

	@Autowired
	DriverPersistenceAdapter adapter;

	@Test
	void 저장하면_id가_채워지고_닉네임으로_존재_여부를_확인할_수_있다() {
		Driver saved = adapter.save(Driver.register("깜빡왕", "11680"));

		assertThat(saved.getId()).isNotNull();
		assertThat(adapter.existsByNickname("깜빡왕")).isTrue();
		assertThat(adapter.existsByNickname("없는닉네임")).isFalse();
	}

	@Test
	void 같은_닉네임이_중복_확인을_통과해_저장되려_하면_DB_제약으로_막고_CONFLICT로_알린다() {
		adapter.save(Driver.register("깜빡왕", "11680"));

		// 동시 요청 두 건이 모두 existsByNickname을 통과한 상황
		assertThatThrownBy(() -> adapter.save(Driver.register("깜빡왕", "26350")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode())
			.isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void 유니크_제약이_아닌_무결성_오류는_CONFLICT로_바꾸지_않고_그대로_올린다() {
		// API 검증을 거치지 않아 컬럼 길이(12자)를 넘긴 경우
		Driver tooLong = Driver.register("가가가가가가가가가가가가가", "11680");

		assertThatThrownBy(() -> adapter.save(tooLong)).isInstanceOf(DataIntegrityViolationException.class);
	}
}
