package com.orai.dahta.constant;

/** 안내 설정(SQLite)에 쓰는 이름, 키, 기본값. */
public final class SettingsConstants {
    /** 데이터베이스 이름과 버전. */
    public static final String DB_NAME = "dahta.db";
    public static final int DB_VERSION = 1;

    /** 설정을 키-값으로 저장하는 테이블. 같은 키에 저장하면 마지막 값으로 덮어쓴다. */
    public static final String TABLE = "settings";
    public static final String COLUMN_KEY = "setting_key";
    public static final String COLUMN_VALUE = "setting_value";

    public static final String KEY_STT_LANGUAGE = "stt_language";
    public static final String KEY_TTS_LANGUAGE = "tts_language";
    public static final String KEY_SPEECH_SPEED_STEP = "speech_speed_step";
    public static final String KEY_CARE_MODE = "care_mode";

    /** 지원 언어 코드. 설정 화면의 구분 막대 순서(한국어, 영어, 프랑스어, 스페인어)와 같다. */
    public static final String[] LANGUAGES = {"ko", "en", "fr", "es"};
    public static final String DEFAULT_LANGUAGE = "ko";

    /** 말하기 속도: 0.5x ~ 1.5x 를 0.05 단위로 20칸. 가운데(10)가 1.0x. */
    public static final float SPEED_MIN = 0.5f;
    public static final float SPEED_UNIT = 0.05f;
    public static final int SPEED_STEP_MAX = 20;
    public static final int SPEED_STEP_DEFAULT = 10;

    public static final boolean DEFAULT_CARE_MODE = true;

    private SettingsConstants() {
    }
}
