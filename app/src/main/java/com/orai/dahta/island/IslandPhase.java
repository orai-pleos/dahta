package com.orai.dahta.island;

/** 운행 중 island 보여주는 단계. */
public enum IslandPhase {
    WAITING, /** 호출을 기다리는 중. */
    CALL,    /** 새 호출이 들어옴. */
    DRIVING, /** 호출을 수락하고 손님을 모시러 가는 중. */
    GUIDING, /** 손님 탑승 후 안내 순간. */
    ARRIVED  /** 목적지 도착. */
}
