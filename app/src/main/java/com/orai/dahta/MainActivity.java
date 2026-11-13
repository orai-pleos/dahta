package com.orai.dahta;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.orai.dahta.constant.IslandConstants;
import com.orai.dahta.constant.SessionPrefs;
import com.orai.dahta.navigation.NavigationLauncher;
import com.orai.dahta.navigation.PackageNavigationLauncher;
import com.orai.dahta.res.R;

public class MainActivity extends AppCompatActivity {
  private SharedPreferences preferences;
  private NavigationLauncher navigationLauncher;

  /**
   * '다른 앱 위에 표시' 권한 설정 화면에서 돌아오면, 허용됐을 때 섬을 띄운다.
   */
  private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
      registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (Settings.canDrawOverlays(this)) {
          launchIsland();
        } else {
          Toast.makeText(this, R.string.island_permission_denied, Toast.LENGTH_LONG).show();
        }
      });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    preferences = getSharedPreferences(SessionPrefs.NAME, MODE_PRIVATE);
    navigationLauncher = new PackageNavigationLauncher(this);
    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        if (preferences.getBoolean(SessionPrefs.KEY_RUNNING, false)) {
          // Leaving the app does not silently end an active session.
          moveTaskToBack(true);
        } else if (preferences.getBoolean(SessionPrefs.KEY_SUMMARY_PENDING, false)) {
          dismissSummary();
        } else {
          finish();
        }
      }
    });
    renderSession();
  }

  private void renderSession() {
    boolean running = preferences.getBoolean(SessionPrefs.KEY_RUNNING, false);
    if (running && !IslandOverlayService.isActive()) {

      preferences.edit().putBoolean(SessionPrefs.KEY_RUNNING, false)
          .remove(SessionPrefs.KEY_STARTED_AT).apply();
      running = false;
    }
    if (running) {
      showWaiting();
    } else {
      showMain();
    }
  }

  private void setScreen(int layout) {
    setContentView(layout);
    View canvas = findViewById(R.id.main_canvas);
    ViewCompat.setOnApplyWindowInsetsListener(canvas, (view, windowInsets) -> {
      Insets safeArea = windowInsets.getInsets(
          WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      view.setPadding(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom);
      return windowInsets;
    });
    ViewCompat.requestApplyInsets(canvas);
  }

  private void showMain() {
    setScreen(R.layout.activity_main);
    CompoundButton careSwitch = findViewById(R.id.switch_care_mode);
    careSwitch.setChecked(preferences.getBoolean(SessionPrefs.KEY_CARE_MODE, true));
    careSwitch.setOnCheckedChangeListener((button, checked) ->
        preferences.edit().putBoolean(SessionPrefs.KEY_CARE_MODE, checked).apply());
    findViewById(R.id.btn_start).setOnClickListener(view -> startSession());
    boolean pending = preferences.getBoolean(SessionPrefs.KEY_SUMMARY_PENDING, false);
    findViewById(R.id.summary_overlay).setVisibility(pending ? View.VISIBLE : View.GONE);
    if (pending) {
      long minutes = preferences.getLong(SessionPrefs.KEY_DURATION_MS, 0) / 60000;
      TextView duration = findViewById(R.id.summary_duration);
      duration.setText(minutes >= 60
          ? getString(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
          : getString(R.string.duration_minutes, minutes));
      // Keep accessibility focus on the modal while it covers the main screen.
      findViewById(R.id.main_content).setImportantForAccessibility(
          View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
      findViewById(R.id.btn_summary_confirm).setOnClickListener(view -> dismissSummary());
      findViewById(R.id.btn_summary_confirm).requestFocus();
    }
  }

  private void startSession() {
    preferences.edit().putBoolean(SessionPrefs.KEY_RUNNING, true)
        .putBoolean(SessionPrefs.KEY_SUMMARY_PENDING, false)
        .putLong(SessionPrefs.KEY_STARTED_AT, System.currentTimeMillis()).apply();
    showWaiting();
    startIsland();
  }

  /**
   * 운행 시작: 권한이 있으면 바로, 없으면 설정 화면을 거쳐 섬을 띄우고 내비를 앞으로 띄운다.
   */
  private void startIsland() {
    if (Settings.canDrawOverlays(this)) {
      launchIsland();
      return;
    }
    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:" + getPackageName()));
    try {
      overlayPermissionLauncher.launch(intent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, R.string.island_permission_denied, Toast.LENGTH_LONG).show();
    }
  }

  private void launchIsland() {
    ContextCompat.startForegroundService(this, islandIntent());
    openNavigation();
  }

  /**
   * 내비게이션 앱을 앞으로 띄운다. 닿다는 그 뒤로 내려가서 섬으로만 동작한다.
   */
  private void openNavigation() {
    navigationLauncher.open(new NavigationLauncher.Listener() {
      @Override
      public void onOpened() {
        // 내비가 앞으로 오면서 닿다 화면은 자연스럽게 뒤로 내려간다.
      }

      @Override
      public void onNotInstalled() {
        // 내비 앱이 없으면 닿다 화면만 내린다.
        moveTaskToBack(true);
      }

      @Override
      public void onFailed(RuntimeException error) {
        Log.e("MainActivity", "내비게이션 실행에 실패했어요", error);
      }
    });
  }

  /**
   * 섬을 놓을 영역을 담은 Intent. 앱 창이 놓인 영역(차량 UI의 앱 영역) 가운데에 섬이 온다.
   */
  private Intent islandIntent() {
    View window = getWindow().getDecorView();
    int[] location = new int[2];
    window.getLocationOnScreen(location);
    return new Intent(this, IslandOverlayService.class)
        .putExtra(IslandConstants.EXTRA_AREA_LEFT, location[0])
        .putExtra(IslandConstants.EXTRA_AREA_TOP, location[1])
        .putExtra(IslandConstants.EXTRA_AREA_WIDTH, window.getWidth());
  }

  private void showWaiting() {
    setScreen(R.layout.activity_waiting);
    boolean careMode = preferences.getBoolean(SessionPrefs.KEY_CARE_MODE, true);
    ((TextView) findViewById(R.id.care_status_text)).setText(
        careMode ? R.string.care_status_on : R.string.care_status_off);
    findViewById(R.id.care_status).setBackgroundResource(
        careMode ? R.drawable.bg_care_enabled : R.drawable.bg_care_disabled);
    findViewById(R.id.care_status_dot).setAlpha(careMode ? 1f : 0.3f);
  }

  private void dismissSummary() {
    preferences.edit().putBoolean(SessionPrefs.KEY_SUMMARY_PENDING, false).apply();
    showMain();
  }
}
