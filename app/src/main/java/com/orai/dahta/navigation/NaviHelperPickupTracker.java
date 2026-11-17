package com.orai.dahta.navigation;

import ai.pleos.playground.navi.data.CurrentLocationInfo;
import ai.pleos.playground.navi.data.DestinationArrivedInfo;
import ai.pleos.playground.navi.data.DestinationInfo;
import ai.pleos.playground.navi.data.DrivingInfo;
import ai.pleos.playground.navi.data.RouteInfo;
import ai.pleos.playground.navi.helper.NaviHelper;
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

/**
 * Pleos NaviHelper 로 탑승 지점까지 길 안내를 요청하고, 남은 거리와 방향을 읽는다.
 *
 * <p>거리: 내비가 주는 목적지까지 남은 거리({@code DestinationInfo.remainDistance},
 * 안내 중에는 {@code DrivingInfo}). 방향: 현재 위치에서 탑승 지점이 보이는 각도에서 차의 진행 방향을 뺀 값.
 * 이벤트가 드문 경우를 대비해 안내 중에는 주기적으로 현재 위치와 목적지 정보를 다시 요청한다.
 */
public final class NaviHelperPickupTracker implements PickupTracker {
  private static final long POLL_INTERVAL_MS = 2000;

  private final NaviHelper naviHelper;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable poll = this::pollOnce;
  private final NaviHelperEventListener eventListener = new EventListener();
  private Listener listener;
  private Point point;
  private float lastBearing;
  private int lastMeters = Integer.MAX_VALUE;
  /** 내비가 준 길 기준 남은 거리. 길 안내가 없으면 0 이하. */
  private int routeMeters;
  /** 현재 위치에서 탑승 지점까지 직선 거리. 위치를 받기 전에는 0 이하. */
  private int straightMeters;
  private boolean arrived;

  public NaviHelperPickupTracker(Context context) {
    this.naviHelper = new NaviHelper(context.getApplicationContext());
  }

  @Override
  public void start(Point newPoint, Listener newListener) {
    point = newPoint;
    listener = newListener;
    lastBearing = 0f;
    lastMeters = Integer.MAX_VALUE;
    routeMeters = 0;
    straightMeters = 0;
    arrived = false;
    naviHelper.initialize(() -> {
      naviHelper.addListener(eventListener);
      naviHelper.requestRoute(new RouteInfo(point.latitude, point.longitude, null, null,
          point.name, "", point.address, "", ai.pleos.playground.navi.constants.RouteOption.RECOMMENDED,
          "", false));
      handler.post(poll);
      return kotlin.Unit.INSTANCE;
    }, error -> kotlin.Unit.INSTANCE);
  }

  @Override
  public void stop() {
    handler.removeCallbacks(poll);
    naviHelper.removeListener(eventListener);
    naviHelper.release();
    listener = null;
    point = null;
  }

  private void pollOnce() {
    naviHelper.getCurrentLocationInfo();
    naviHelper.getDestinationInfo();
    handler.postDelayed(poll, POLL_INTERVAL_MS);
  }

  /**
   * 거리는 내비가 길을 찾아 준 값을 우선한다. 길 안내가 없거나(경로 탐색 실패 등) 아직 오지 않았으면
   * 현재 위치와 탑승 지점 사이의 직선 거리로 대신한다.
   */
  private void updateMeters() {
    if (routeMeters > 0) {
      lastMeters = routeMeters;
    } else if (straightMeters > 0) {
      lastMeters = straightMeters;
    }
  }

  private void notifyUpdated() {
    if (listener != null && lastMeters != Integer.MAX_VALUE) {
      listener.onUpdated(lastMeters, lastBearing);
    }
  }

  private final class EventListener implements NaviHelperEventListener {
    @Override
    public void onCurrentLocationInfo(CurrentLocationInfo info) {
      if (point == null) {
        return;
      }
      float[] result = new float[2];
      Location.distanceBetween(info.getLatitude(), info.getLongitude(),
          point.latitude, point.longitude, result);
      float toPoint = result[1];
      float relative = (float) (toPoint - info.getHeading());
      // -180..180 도로 맞춘다.
      lastBearing = ((relative + 540f) % 360f) - 180f;
      straightMeters = Math.max(1, Math.round(result[0]));
      updateMeters();
      notifyUpdated();
    }

    @Override
    public void onDestinationInfo(DestinationInfo info) {
      // 길 안내가 없으면 남은 거리가 0 으로 온다. 도착이 아니므로 무시한다.
      if (info.getRemainDistance() <= 0) {
        return;
      }
      routeMeters = info.getRemainDistance();
      updateMeters();
      notifyUpdated();
    }

    @Override
    public void onDrivingInfo(DrivingInfo info) {
      if (info.getDestination().getDistance() <= 0) {
        return;
      }
      routeMeters = info.getDestination().getDistance();
      updateMeters();
      notifyUpdated();
    }

    @Override
    public void onDestinationArrived(DestinationArrivedInfo info) {
      if (arrived) {
        return;
      }
      arrived = true;
      handler.removeCallbacks(poll);
      if (listener != null) {
        listener.onArrived();
      }
    }
  }
}
