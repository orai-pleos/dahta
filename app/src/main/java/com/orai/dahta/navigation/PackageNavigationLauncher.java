package com.orai.dahta.navigation;

import android.content.Context;
import android.content.Intent;

import com.orai.dahta.constant.NavigationConstants;

/** 내비 앱의 패키지 이름으로 실행 Intent 를 만들어 띄운다. 이미 떠 있으면 그 화면을 앞으로 가져온다. */
public final class PackageNavigationLauncher implements NavigationLauncher {
    private final Context context;

    public PackageNavigationLauncher(Context context) {
        this.context = context;
    }

    @Override
    public void open(Listener listener) {
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(NavigationConstants.PACKAGE);
        if (intent == null) {
            listener.onNotInstalled();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (RuntimeException error) {
            listener.onFailed(error);
            return;
        }
        listener.onOpened();
    }
}
