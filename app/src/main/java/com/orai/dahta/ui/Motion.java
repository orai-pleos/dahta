package com.orai.dahta.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * 화면 곳곳에서 쓰는 부드러운 움직임과, 그림자가 잘리지 않게 하는 도우미.
 * 시간과 비율은 한곳에 모아 두어서 화면 전체의 느낌을 한 번에 조절할 수 있다.
 */
public final class Motion {
    /** 버튼을 누르는 동안의 크기와 오가는 시간. */
    private static final float PRESSED_SCALE = 0.96f;
    private static final long PRESS_DURATION = 90;
    private static final long RELEASE_DURATION = 260;
    /** 화면이 나타날 때 아래에서 올라오는 거리(dp)와 시간. */
    private static final float ENTER_RISE_DP = 20f;
    private static final long ENTER_DURATION = 320;
    /** 섬 페이지가 나타날 때 시작 크기와 시간. */
    private static final float POP_START_SCALE = 0.92f;
    private static final long POP_DURATION = 300;

    private Motion() {
    }

    /**
     * 누르면 살짝 작아졌다가, 떼면 통통 튀며 돌아오게 한다. 클릭 동작에는 영향이 없다.
     */
    public static void pressScale(View... views) {
        for (View view : views) {
            if (view == null) {
                continue;
            }
            view.setOnTouchListener((target, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        target.animate().scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE)
                                .setDuration(PRESS_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        target.animate().scaleX(1f).scaleY(1f)
                                .setDuration(RELEASE_DURATION)
                                .setInterpolator(new OvershootInterpolator(2f))
                                .start();
                        break;
                    default:
                        break;
                }
                // 클릭은 그대로 처리되도록 이벤트를 소비하지 않는다.
                return false;
            });
        }
    }

    /**
     * 화면이 처음 나타날 때, 투명한 상태에서 아래에서 위로 부드럽게 떠오르며 보이게 한다.
     */
    public static void enter(View view) {
        float rise = ENTER_RISE_DP * view.getResources().getDisplayMetrics().density;
        view.setAlpha(0f);
        view.setTranslationY(rise);
        view.animate().alpha(1f).translationY(0f)
                .setDuration(ENTER_DURATION)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    /**
     * 섬 페이지가 나타날 때, 위쪽 가운데를 기준으로 살짝 작은 크기에서 튀어나오듯 커지며 보이게 한다.
     * 아직 크기가 정해지지 않은 뷰도 레이아웃이 끝나는 대로 시작한다.
     */
    public static void popIn(View view) {
        view.setAlpha(0f);
        view.setScaleX(POP_START_SCALE);
        view.setScaleY(POP_START_SCALE);
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                v.setPivotX((right - left) / 2f);
                v.setPivotY(0f);
                v.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(POP_DURATION)
                        .setInterpolator(new OvershootInterpolator(0.9f))
                        .start();
            }
        });
        // 레이아웃 알림이 오지 않아도 보이지 않는 채로 남지 않게 안전장치를 둔다.
        view.postDelayed(() -> {
            view.setAlpha(1f);
            view.setScaleX(1f);
            view.setScaleY(1f);
        }, POP_DURATION + 400);
    }

    /**
     * elevation 그림자는 부모가 자식을 잘라내면 가장자리가 끊겨 보인다.
     * 이 뷰의 모든 부모에서 자르기(clipChildren, clipToPadding)를 꺼서 그림자가 온전히 보이게 한다.
     */
    public static void allowShadowOverflow(View view) {
        ViewParent parent = view.getParent();
        while (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            group.setClipChildren(false);
            group.setClipToPadding(false);
            parent = group.getParent();
        }
    }
}
