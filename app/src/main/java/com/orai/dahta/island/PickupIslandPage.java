package com.orai.dahta.island;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.orai.dahta.res.R;
import com.orai.dahta.ui.PulseRingsView;

/**
 * 손님 마중 알약. 호출을 잡고 탑승 지점으로 가는 동안, 내비 거리에 맞춰 보여 준다.
 * <ul>
 *   <li>멀리: ↑ 1.2km, 고리 옅게</li>
 *   <li>가까이: 탑승 지점 방향 화살표 + 120m, 고리 짙게</li>
 *   <li>도착: 핀 + "도착", 고리 3겹</li>
 * </ul>
 * 거리와 방향은 {@link #update(int, float)} 로 받는다.
 */
public final class PickupIslandPage extends IslandPage {
    /** 이 거리(m) 이하면 "가까이". */
    public static final int NEAR_METERS = 300;
    /** 이 거리(m) 이하면 "도착". */
    public static final int ARRIVED_METERS = 30;

    private final PulseRingsView rings = find(R.id.island_pickup_rings);
    private final ImageView icon = find(R.id.island_pickup_icon);
    private final TextView text = find(R.id.island_pickup_text);
    private int meters = 1200;
    private float bearingDegrees = 0f;

    public PickupIslandPage(Context context, ViewGroup parent) {
        super(context, parent, R.layout.island_page_pickup);
    }

    /**
     * @param newMeters  탑승 지점까지 남은 거리(m)
     * @param newBearing 진행 방향 기준 탑승 지점의 방향(도). 0 이 앞, 시계 방향이 +.
     */
    public void update(int newMeters, float newBearing) {
        meters = newMeters;
        bearingDegrees = newBearing;
    }

    @Override
    public void bind(IslandPhase phase) {
        Context context = view().getContext();
        if (meters <= ARRIVED_METERS) {
            rings.setLevel(2);
            icon.setImageResource(R.drawable.ic_nav_pin);
            icon.setRotation(0f);
            text.setText(R.string.island_pickup_arrived);
            return;
        }
        rings.setLevel(meters <= NEAR_METERS ? 1 : 0);
        icon.setImageResource(R.drawable.ic_nav_arrow);
        icon.setRotation(bearingDegrees);
        text.setText(meters >= 1000
                ? context.getString(R.string.island_pickup_km, meters / 1000f)
                : context.getString(R.string.island_pickup_m, meters));
    }
}
