package com.orai.dahta.navigation;

/**
 * 호출을 잡은 뒤 탑승 지점까지 가는 동안의 거리와 방향을 알려 주는 일.
 * 어디서 값을 받는지(Pleos NaviHelper 등)는 구현이 정하고, 섬은 이 인터페이스만 안다.
 */
public interface PickupTracker {

  /** 탑승 지점. */
  final class Point {
    public final double latitude;
    public final double longitude;
    public final String name;
    public final String address;

    public Point(double latitude, double longitude, String name, String address) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.name = name;
      this.address = address;
    }
  }

  /** 거리와 방향을 받는 쪽. 호출 스레드는 정해져 있지 않다. */
  interface Listener {
    /**
     * @param meters         탑승 지점까지 남은 거리(m)
     * @param bearingDegrees 진행 방향 기준 탑승 지점의 방향(도). 0 이 앞, 시계 방향이 +.
     */
    void onUpdated(int meters, float bearingDegrees);

    /** 탑승 지점에 도착했다. */
    void onArrived();
  }

  /** 탑승 지점으로 길 안내를 시작하고, 거리를 알려 주기 시작한다. */
  void start(Point point, Listener listener);

  /** 거리 알림을 멈춘다. */
  void stop();
}
