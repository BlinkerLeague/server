package com.blinkerleague.server.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;

class RefreshTokenServiceTest {

	private final InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
	private final RefreshTokenService service = new RefreshTokenService(store,
		new JwtProperties("blinkerleague-test-secret-key-32bytes-or-more!", Duration.ofHours(1), Duration.ofDays(30)));

	@Test
	void 발급한_리프레시_토큰은_원문이_아닌_SHA256_해시로_저장한다() {
		String token = service.issue(1L);

		assertThat(store.storedHashes()).containsExactly(sha256(token));
		assertThat(store.storedHashes()).doesNotContain(token);
	}

	@Test
	void 발급할_때마다_다른_토큰을_만든다() {
		assertThat(service.issue(1L)).isNotEqualTo(service.issue(2L));
	}

	@Test
	void 재발급하면_같은_운전자의_새_리프레시_토큰을_주고_이전_토큰은_지운다() {
		String token = service.issue(1L);

		RefreshTokenService.Rotation rotation = service.rotate(token);

		assertThat(rotation.driverId()).isEqualTo(1L);
		assertThat(rotation.refreshToken()).isNotEqualTo(token);
		assertThat(store.storedHashes()).containsExactly(sha256(rotation.refreshToken()));
	}

	@Test
	void 이미_사용한_토큰을_다시_쓰면_거부하고_그_운전자의_현재_토큰도_무효화한다() {
		String stolen = service.issue(1L);
		RefreshTokenService.Rotation rotation = service.rotate(stolen);

		assertInvalidToken(() -> service.rotate(stolen));
		assertInvalidToken(() -> service.rotate(rotation.refreshToken()));
	}

	@Test
	void 저장되지_않은_토큰은_거부한다() {
		assertInvalidToken(() -> service.rotate("unknown-token"));
	}

	@Test
	void 강제_로그아웃하면_리프레시_토큰으로_재발급할_수_없다() {
		String token = service.issue(1L);

		service.revoke(1L);

		assertInvalidToken(() -> service.rotate(token));
	}

	private static void assertInvalidToken(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	private static String sha256(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
