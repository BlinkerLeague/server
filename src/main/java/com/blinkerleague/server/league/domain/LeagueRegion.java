package com.blinkerleague.server.league.domain;

/** 리그 권역(D11). 운전자의 시군구 코드를 매핑표로 묶은 단위다. 예) SEOUL_NORTHWEST "서울 서북권". */
public record LeagueRegion(String code, String name) {
}
