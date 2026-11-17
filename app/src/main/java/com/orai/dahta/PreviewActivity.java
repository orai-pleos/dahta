package com.orai.dahta;

import android.app.Activity;
import android.os.Bundle;

/**
 * 임시 확인용. 레이아웃 XML만 화면에 띄워 본다. (확인 후 삭제)
 * adb shell am start -n com.orai.dahta/.PreviewActivity --es layout activity_destination_ko
 */
public class PreviewActivity extends Activity {

    private static final String EXTRA_LAYOUT = "layout";
    private static final String DEFAULT_LAYOUT = "activity_destination_ko";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String name = getIntent().getStringExtra(EXTRA_LAYOUT);
        if (name == null) {
            name = DEFAULT_LAYOUT;
        }
        if ("destination".equals(name)) {
            // 임시: exported 가 아닌 DestinationActivity 를 adb 에서 띄우기 위한 통로.
            startActivity(new android.content.Intent(this, DestinationActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        if ("pickup".equals(name)) {
            // 임시: 마중 알약의 단계를 거리(--ei meters) / 방향(--ef bearing)으로 바꿔 보기.
            android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
            frame.setBackgroundColor(0xFFF4F4F6);
            com.orai.dahta.island.PickupIslandPage page =
                new com.orai.dahta.island.PickupIslandPage(this, frame);
            page.update(getIntent().getIntExtra("meters", 1200),
                getIntent().getFloatExtra("bearing", 0f));
            page.bind(com.orai.dahta.island.IslandPhase.PICKUP);
            android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER);
            frame.addView(page.view(), lp);
            setContentView(frame);
            return;
        }
        int layoutId = getResources().getIdentifier(name, "layout", getPackageName());
        setContentView(layoutId);
    }
}
