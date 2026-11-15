package com.orai.dahta.settings;

/**
 * 안내 설정(듣기/말하기 언어, 말하기 속도, 배려 모드)의 저장소.
 * 항상 마지막으로 저장한 값을 돌려주며, 저장된 값이 없으면 기본값을 돌려준다.
 * 어디에 저장하는지(SQLite 등)는 구현체가 정한다.
 */
public interface SettingsStore {

    /** 듣기(STT) 언어 코드. 예: ko, en, fr, es */
    String sttLanguage();

    void setSttLanguage(String languageCode);

    /** 말하기(TTS) 언어 코드. */
    String ttsLanguage();

    void setTtsLanguage(String languageCode);

    /** 말하기 속도 칸(0~20). 10이 1.0x. */
    int speechSpeedStep();

    void setSpeechSpeedStep(int step);

    /** 배려 모드 사용 여부. */
    boolean careMode();

    void setCareMode(boolean enabled);

    /** 저장소를 닫는다. 화면이 사라질 때 호출한다. */
    void close();
}
