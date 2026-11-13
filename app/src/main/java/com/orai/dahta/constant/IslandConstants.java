package com.orai.dahta.constant;

/** 다이나믹 아일랜드(오버레이)와 그 서비스 관련 상수. */
public final class IslandConstants {
    /** 가운데 정렬할 영역(화면 좌표, px). 앱 창의 왼쪽/위쪽/폭. 서비스 시작 Intent 의 extra 키. */
    public static final String EXTRA_AREA_LEFT = "area_left";
    public static final String EXTRA_AREA_TOP = "area_top";
    public static final String EXTRA_AREA_WIDTH = "area_width";

    /** 포그라운드 서비스 알림 채널과 알림 id. */
    public static final String NOTIFICATION_CHANNEL_ID = "drive_island";
    public static final int NOTIFICATION_ID = 1001;

    public static final int TOP_MARGIN_DP = 38;

    private IslandConstants() {
    }
}
