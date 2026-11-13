package com.orai.dahta.navigation;

/**
 * 내비게이션 앱을 앞으로 띄우는 일. 어떤 방식으로 띄우는지(패키지 실행, 딥링크, Pleos SDK 등)는
 * 구현체가 정하고, 호출하는 쪽은 결과를 {@link Listener} 로 받는다.
 */
public interface NavigationLauncher {

    /** 내비를 띄우고 결과를 listener 로 알린다. */
    void open(Listener listener);

    /** 내비 실행 결과. 셋 중 정확히 하나가 호출된다. */
    interface Listener {
        /** 내비 실행을 요청했다. */
        void onOpened();

        /** 내비 앱이 설치돼 있지 않거나 실행할 수 없다. */
        void onNotInstalled();

        /** 실행 요청 중 오류가 났다. */
        void onFailed(RuntimeException error);
    }
}
