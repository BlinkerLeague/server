package com.blinkerleague.server.auth.application.port.out;

/** 운전자 Access 토큰 발급. 운영 구현은 JWT 어댑터다. */
public interface AccessTokenIssuer {

	String issueAccessToken(Long driverId);
}
