package com.orai.dahta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.orai.dahta.constant.IslandConstants;
import com.orai.dahta.island.CallIslandPage;
import com.orai.dahta.island.CompactIslandPage;
import com.orai.dahta.island.GuideIslandPage;
import com.orai.dahta.island.IslandPage;
import com.orai.dahta.island.IslandPhase;
import com.orai.dahta.island.PickupIslandPage;
import com.orai.dahta.navigation.NaviHelperPickupTracker;
import com.orai.dahta.navigation.PickupTracker;
import com.orai.dahta.res.R;
import com.orai.dahta.ui.Motion;
import com.orai.dahta.vehicle.RearDoorWatcher;

/**
 * 운행 중 내비 위에 떠 있는 다이나믹 아일랜드(오버레이).
 *
 * <p>이 서비스는 오버레이 창을 만들고 위치를 잡는 일, 그리고 지금 단계에 맞는 "페이지"를 갈아 끼우는
 * 일만 한다. 각 페이지의 화면과 문구는 {@code com.orai.dahta.island} 의 페이지 클래스가 맡는다.
 * <ul>
 *   <li>{@link CompactIslandPage}: 평소 알약</li>
 *   <li>{@link CallIslandPage}: 호출 도착 카드</li>
 *   <li>{@link GuideIslandPage}: 안내 순간 / 도착 확장</li>
 * </ul>
 * 오버레이를 그리려면 '다른 앱 위에 표시' 권한이 필요하다.
 *
 * <p>섬은 앱 창이 놓여 있던 영역(차량 UI의 앱 영역, 내비가 같은 영역을 쓴다)의 가운데 위쪽에 놓인다.
 * 영역 정보는 시작 Intent 의 EXTRA_AREA_* 로 받고, 없으면 화면 전체를 기준으로 한다.
 *
 * <p>흐름: 호출 대기 → 호출 도착(모시러 가기 / 나중에) → 모시는 중 → 안내 순간 → 도착.
 * 시작하면 '호출 대기 중' 알약만 뜨고 자동으로 바뀌지 않는다. 알약을 누르면 호출 카드가 뜬다.
 * 실제 이벤트(호출 수신, 문 열림, 내비 도착)가 연동되면 {@link #showPhase(IslandPhase, boolean)}
 * 를 호출해 단계를 바꾼다.
 */
public final class IslandOverlayService extends Service {
  private static volatile boolean active;
  private WindowManager windowManager;
  private WindowManager.LayoutParams params;
  /**
   * 오버레이 창에 붙는 틀. 이 안에 페이지 하나가 들어온다.
   */
  private ViewGroup container;
  private CompactIslandPage compactPage;
  private GuideIslandPage guidePage;
  private CallIslandPage callPage;
  private PickupIslandPage pickupPage;
  private IslandPage currentPage;
  /**
   * 이번 터치가 섬 본체에서 시작했는지. 그림자용 투명 여백을 누른 것은 섬을 누른 것으로 보지 않는다.
   */
  private boolean touchStartedOnPage;
  private int areaLeft;
  private int areaTop;
  private int areaWidth;
  private IslandPhase phase = IslandPhase.WAITING;
  private boolean expanded = false;
  private RearDoorWatcher rearDoorWatcher;
  private PickupTracker pickupTracker;
  private boolean trackingPickup;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  /**
   * 섬이 떠 있으면 true.
   */
  public static boolean isActive() {
    return active;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    startAsForeground();
    if (container == null) {
      readArea(intent);
      addIsland();
      startRearDoorWatcher();
      pickupTracker = new NaviHelperPickupTracker(this);
      active = true;
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    active = false;
    stopPickupTracking();
    if (rearDoorWatcher != null) {
      rearDoorWatcher.stop();
      rearDoorWatcher = null;
    }
    if (container != null) {
      windowManager.removeView(container);
      container = null;
    }
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void readArea(Intent intent) {
    int screenWidth = getResources().getDisplayMetrics().widthPixels;
    areaLeft = intent != null ? intent.getIntExtra(IslandConstants.EXTRA_AREA_LEFT, 0) : 0;
    areaTop = intent != null ? intent.getIntExtra(IslandConstants.EXTRA_AREA_TOP, 0) : 0;
    areaWidth = intent != null
        ? intent.getIntExtra(IslandConstants.EXTRA_AREA_WIDTH, screenWidth) : screenWidth;
    if (areaWidth <= 0) {
      areaWidth = screenWidth;
    }
  }

  private void startAsForeground() {
    NotificationManager manager = getSystemService(NotificationManager.class);
    manager.createNotificationChannel(new NotificationChannel(IslandConstants.NOTIFICATION_CHANNEL_ID,
        getString(R.string.island_notification_channel),
        NotificationManager.IMPORTANCE_LOW));
    Notification notification = new NotificationCompat.Builder(this, IslandConstants.NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.figma_car)
        .setContentTitle(getString(R.string.island_notification_title))
        .setContentText(getString(R.string.island_notification_text))
        .setOngoing(true)
        .build();
    ServiceCompat.startForeground(this, IslandConstants.NOTIFICATION_ID, notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
  }

  private void addIsland() {
    windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    Context themed = new ContextThemeWrapper(this, R.style.Theme_Dahta);
    container = (ViewGroup) LayoutInflater.from(themed).inflate(R.layout.view_island, null);
    // 한 번 누르면 단계에 맞는 동작(확장/축소, 설정 열기 등). 운행 종료는 닿다 화면의 [운행 종료하기]로 한다.
    GestureDetector gestureDetector = new GestureDetector(themed,
        new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onDown(MotionEvent event) {
            return true;
          }

          @Override
          public boolean onSingleTapUp(MotionEvent event) {
            onIslandClicked();
            return true;
          }
        });
    container.setOnTouchListener((view, event) -> {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        touchStartedOnPage = isOnPage(event);
      }
      return touchStartedOnPage && gestureDetector.onTouchEvent(event);
    });

    compactPage = new CompactIslandPage(themed, container);
    guidePage = new GuideIslandPage(themed, container);
    callPage = new CallIslandPage(themed, container, this::acceptCall, this::postponeCall);
    pickupPage = new PickupIslandPage(themed, container);

    params = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // LAYOUT_IN_SCREEN: 상태바 아래가 아니라 화면 전체를 좌표 기준으로 삼는다.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.TOP | Gravity.START;
    // 섬 둘레에 그림자용 여백이 있으므로, 위쪽 여백만큼 올려서 섬 본체가 같은 자리에 오게 한다.
    params.y = areaTop
        + Math.round(IslandConstants.TOP_MARGIN_DP * getResources().getDisplayMetrics().density)
        - getResources().getDimensionPixelSize(R.dimen.island_shadow_padding_top);
    showCurrentPage();
    params.x = centeredX();
    windowManager.addView(container, params);
  }

  // ---- 상태 전환 ---------------------------------------------------------------------------

  /**
   * 섬을 눌렀을 때.
   * <ul>
   *   <li>호출 대기 중: 호출 카드를 띄운다.</li>
   *   <li>호출 중(카드): 닿다 설정 화면으로 간다. 섬은 그대로 남는다.</li>
   *   <li>그 외(모시는 중 포함): 확장/축소를 번갈아 한다. 모시는 중 알약을 누르면
   *   "손님을 모시는 중이에요" 카드가 펼쳐진다.</li>
   * </ul>
   */
  private void onIslandClicked() {
    switch (phase) {
      case WAITING:
        showPhase(IslandPhase.CALL, true);
        break;
      case CALL:
      case PICKUP:
        openSettings();
        break;
      default:
        showPhase(phase, !expanded);
        break;
    }
  }

  /**
   * 뒷문이 열렸다 닫히면(손님이 탐) 목적지를 듣는 화면(STT)을 앞으로 띄운다.
   * 섬의 단계는 바꾸지 않으므로, 화면에서 나오면 섬은 그대로 있다.
   */
  private void startRearDoorWatcher() {
    rearDoorWatcher = new RearDoorWatcher(this, this::onRearDoorClosed);
    rearDoorWatcher.start();
  }

  /**
   * 뒷문이 열렸다 닫혔을 때(손님이 탄 순간).
   * <ul>
   *   <li>호출을 잡은 상태(마중 중): 목적지를 이미 알고 있으므로 STT 없이 '모시는 중'으로 바꾼다.</li>
   *   <li>호출이 없는 상태(대기 / 호출 도착 전): 대신 목적지를 듣는 STT 화면을 띄운다.</li>
   *   <li>그 외(이미 모시는 중 등): 아무것도 하지 않는다.</li>
   * </ul>
   */
  private void onRearDoorClosed() {
    switch (phase) {
      case PICKUP:
        showPhase(IslandPhase.DRIVING, false);
        break;
      case WAITING:
      case CALL:
        // 목적지 듣기(STT) 화면은 차가 멈췄을 때(기어 P 또는 N)만 띄운다. 주행 중에는 내비 그대로 둔다.
        if (rearDoorWatcher.isStopped()) {
          openDestination();
        }
        break;
      default:
        break;
    }
  }

  /**
   * 내비게이션이 알려 주는 탑승 지점까지의 거리와 방향을 섬에 반영한다. 마중 중일 때만 쓴다.
   * 실제 내비 이벤트가 연동되면 여기만 호출하면 된다.
   *
   * @param meters         탑승 지점까지 남은 거리(m)
   * @param bearingDegrees 진행 방향 기준 탑승 지점 방향(도). 0 이 앞, 시계 방향이 +.
   */
  private void updatePickup(int meters, float bearingDegrees) {
    pickupPage.update(meters, bearingDegrees);
    if (phase == IslandPhase.PICKUP) {
      render();
    }
  }

  private void openDestination() {
    // 닿다 설정 화면과 다른 작업(taskAffinity 가 비어 있다)에 띄운다. 그래서 화면을 닫으면
    // 닿다 기본 화면이 아니라 직전에 보던 내비로 돌아간다.
    startActivity(new Intent(this, DestinationActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
  }

  /**
   * 닿다 설정 화면을 앞으로 가져온다. 섬의 단계는 바꾸지 않으므로, 사용자가 내비를 눌러
   * 닿다가 다시 백그라운드로 가도 섬은 이전 모습 그대로 있다.
   */
  private void openSettings() {
    startActivity(new Intent(this, MainActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
  }

  /**
   * [모시러 가기]: 호출을 수락하고 손님을 태우러 간다. 손님이 타면 '모시는 중'으로 바뀐다.
   */
  private void acceptCall() {
    showPhase(IslandPhase.PICKUP, false);
    startPickupTracking();
  }

  /**
   * 탑승 지점으로 길 안내를 요청하고, 내비가 주는 남은 거리와 방향을 섬에 반영한다.
   * 탑승 지점은 아직 호출 데이터가 없어 화면 확인용 값(IslandConstants)을 쓴다.
   */
  private void startPickupTracking() {
    trackingPickup = true;
    pickupTracker.start(new PickupTracker.Point(
            IslandConstants.DEMO_PICKUP_LATITUDE, IslandConstants.DEMO_PICKUP_LONGITUDE,
            IslandConstants.DEMO_PICKUP_NAME, IslandConstants.DEMO_PICKUP_ADDRESS),
        new PickupTracker.Listener() {
          @Override
          public void onUpdated(int meters, float bearingDegrees) {
            mainHandler.post(() -> updatePickup(meters, bearingDegrees));
          }

          @Override
          public void onArrived() {
            mainHandler.post(() -> updatePickup(0, 0f));
          }
        });
  }

  private void stopPickupTracking() {
    if (trackingPickup) {
      trackingPickup = false;
      pickupTracker.stop();
    }
  }

  /**
   * [나중에]: 호출을 미루고 대기 상태로 돌아간다.
   */
  private void postponeCall() {
    showPhase(IslandPhase.WAITING, false);
  }

  /**
   * 섬의 단계와 확장 여부를 바꾼다. 실제 이벤트가 연동되면 여기만 호출하면 된다.
   */
  private void showPhase(IslandPhase newPhase, boolean newExpanded) {
    if (newPhase != IslandPhase.PICKUP) {
      stopPickupTracking();
    }
    phase = newPhase;
    expanded = newExpanded;
    render();
  }

  // ---- 화면 ------------------------------------------------------------------------------

  private void render() {
    if (container == null) {
      return;
    }
    showCurrentPage();
    // 창 폭이 바뀌므로 영역 가운데로 다시 맞춘다.
    params.x = centeredX();
    windowManager.updateViewLayout(container, params);
  }

  /**
   * 터치 위치가 지금 보이는 섬 본체 안인지.
   */
  private boolean isOnPage(MotionEvent event) {
    if (currentPage == null) {
      return false;
    }
    View page = currentPage.view();
    return event.getX() >= page.getLeft() && event.getX() <= page.getRight()
        && event.getY() >= page.getTop() && event.getY() <= page.getBottom();
  }

  /**
   * 지금 단계와 확장 여부에 맞는 페이지.
   */
  private IslandPage pageFor() {
    if (phase == IslandPhase.PICKUP) {
      return pickupPage;
    }
    if (expanded && phase == IslandPhase.CALL) {
      return callPage;
    }
    if (expanded && phase != IslandPhase.WAITING) {
      return guidePage;
    }
    return compactPage;
  }

  /**
   * 맞는 페이지로 갈아 끼우고 내용을 갱신한다.
   */
  private void showCurrentPage() {
    IslandPage page = pageFor();
    page.bind(phase);
    if (page != currentPage) {
      container.removeAllViews();
      container.addView(page.view());
      currentPage = page;
      // 페이지가 바뀔 때 딱 바뀌지 않고 마치 튀어나오듯 나타난다.
      Motion.popIn(page.view());
    }
  }

  /**
   * 섬을 영역 가운데에 놓기 위한 왼쪽 좌표. 섬 폭을 먼저 재서 계산한다.
   */
  private int centeredX() {
    container.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    return areaLeft + (areaWidth - container.getMeasuredWidth()) / 2;
  }
}
