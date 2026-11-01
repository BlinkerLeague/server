package com.blinkerleague.server.security.jwt;

import java.time.Duration;

/** JWT 서명 키와 토큰 유효기간. secret은 HS256에 쓰므로 32바이트(256bit) 이상이어야 한다. */
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {
}
