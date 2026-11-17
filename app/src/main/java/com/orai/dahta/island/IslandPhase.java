package com.orai.dahta.island;

/** 운행 중 island 보여주는 단계. */
public enum IslandPhase {
    WAITING, /** 호출을 기다리는 중. */
    CALL,    /** 새 호출이 들어옴. */
    PICKUP,  /** 호출을 잡고 손님을 태우러 가는 중(손님은 아직 안 탐). */
    DRIVING, /** 손님이 탄 뒤, 모시는 중. */
    GUIDING, /** 안내 순간. */
    ARRIVED  /** 목적지 도착. */
}
