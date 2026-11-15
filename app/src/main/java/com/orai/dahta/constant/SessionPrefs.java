package com.orai.dahta.constant;

/** 운행 상태를 저장하는 SharedPreferences 의 파일 이름과 키. */
public final class SessionPrefs {
    public static final String NAME = "drive_session";

    /** 운행 중인지. */
    public static final String KEY_RUNNING = "running";
    /** 운행을 마치고 요약 화면 확인을 기다리는 중인지. */
    public static final String KEY_SUMMARY_PENDING = "summary_pending";
    /** 운행을 시작한 시각(ms). */
    public static final String KEY_STARTED_AT = "started_at";
    /** 마지막 운행 시간(ms). */
    public static final String KEY_DURATION_MS = "duration_ms";

    private SessionPrefs() {
    }
}
