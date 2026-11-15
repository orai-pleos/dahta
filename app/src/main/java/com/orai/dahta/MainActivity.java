package com.orai.dahta;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
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
import com.orai.dahta.constant.SettingsConstants;
import com.orai.dahta.navigation.NavigationLauncher;
import com.orai.dahta.navigation.PackageNavigationLauncher;
import com.orai.dahta.res.R;
import com.orai.dahta.settings.SettingsStore;
import com.orai.dahta.settings.SqliteSettingsStore;
import com.orai.dahta.ui.Motion;

import java.util.Locale;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
  private SharedPreferences preferences;
  private NavigationLauncher navigationLauncher;
  private SettingsStore settingsStore;

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
    settingsStore = new SqliteSettingsStore(this);
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

  @Override
  protected void onDestroy() {
    settingsStore.close();
    super.onDestroy();
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
    // 화면이 바뀔 때 딱 바뀌지 않고 부드럽게 떠오르며 나타난다.
    Motion.enter(canvas);
  }

  private void showMain() {
    setScreen(R.layout.activity_main);
    bindCareSwitch();
    View start = findViewById(R.id.btn_start);
    Motion.allowShadowOverflow(start);
    Motion.pressScale(start, findViewById(R.id.btn_summary_confirm));
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
    // 안내 설정: 화면을 열 때는 마지막으로 저장한 값을 보여주고, 바꿀 때마다 저장한다.
    bindCareSwitch();
    bindLanguageSegment(R.id.segment_stt, settingsStore.sttLanguage(),
        settingsStore::setSttLanguage);
    bindLanguageSegment(R.id.segment_tts, settingsStore.ttsLanguage(),
        settingsStore::setTtsLanguage);
    bindSpeechSpeed();
    View endDrive = findViewById(R.id.btn_end_drive);
    Motion.allowShadowOverflow(endDrive);
    // 손님을 기다리는 카드와 진행 단계 카드의 그림자가 잘리지 않게 한다.
    Motion.allowShadowOverflow(findViewById(R.id.waiting_status_card));
    Motion.allowShadowOverflow(findViewById(R.id.waiting_steps_card));
    Motion.pressScale(endDrive, findViewById(R.id.speech_speed_minus),
        findViewById(R.id.speech_speed_plus));
  }

  /**
   * 배려 모드 스위치. 메인 화면과 설정 화면이 같은 저장 값을 쓴다.
   */
  private void bindCareSwitch() {
    CompoundButton careSwitch = findViewById(R.id.switch_care_mode);
    careSwitch.setChecked(settingsStore.careMode());
    careSwitch.setOnCheckedChangeListener((button, checked) -> settingsStore.setCareMode(checked));
  }

  /**
   * 언어 구분 막대(한국어, 영어, 프랑스어, 스페인어). 저장된 언어를 선택해 보여주고,
   * 눌러서 바꾸면 바뀐 언어 코드를 onSelected 로 알린다.
   */
  private void bindLanguageSegment(int segmentId, String savedCode, Consumer<String> onSelected) {
    ViewGroup segment = findViewById(segmentId);
    for (int index = 0; index < segment.getChildCount(); index++) {
      final int selectedIndex = index;
      View option = segment.getChildAt(index);
      // 선택 상태가 바뀔 때 흰 알약이 부드럽게 나타나고 사라지도록 선택자 배경을 쓴다.
      option.setBackgroundResource(R.drawable.bg_segment_item);
      Motion.pressScale(option);
      option.setOnClickListener(view -> {
        showSelectedLanguage(segment, selectedIndex);
        onSelected.accept(SettingsConstants.LANGUAGES[selectedIndex]);
      });
    }
    int savedIndex = 0;
    for (int index = 0; index < SettingsConstants.LANGUAGES.length; index++) {
      if (SettingsConstants.LANGUAGES[index].equals(savedCode)) {
        savedIndex = index;
      }
    }
    showSelectedLanguage(segment, savedIndex);
  }

  private void showSelectedLanguage(ViewGroup segment, int selectedIndex) {
    for (int index = 0; index < segment.getChildCount(); index++) {
      segment.getChildAt(index).setSelected(index == selectedIndex);
    }
  }

  /**
   * 말하기 속도 슬라이더(0.5x ~ 1.5x, 20칸). 끌고 손을 떼거나 좌우 화살표를 눌러 바꾸면 저장한다.
   */
  private void bindSpeechSpeed() {
    SeekBar slider = findViewById(R.id.speech_speed_slider);
    TextView value = findViewById(R.id.speech_speed_value);
    slider.setMax(SettingsConstants.SPEED_STEP_MAX);
    slider.setProgress(settingsStore.speechSpeedStep());
    showSpeechSpeed(value, slider.getProgress());
    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        showSpeechSpeed(value, progress);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        settingsStore.setSpeechSpeedStep(seekBar.getProgress());
      }
    });
    findViewById(R.id.speech_speed_minus).setOnClickListener(view -> {
      slider.setProgress(slider.getProgress() - 1);
      settingsStore.setSpeechSpeedStep(slider.getProgress());
    });
    findViewById(R.id.speech_speed_plus).setOnClickListener(view -> {
      slider.setProgress(slider.getProgress() + 1);
      settingsStore.setSpeechSpeedStep(slider.getProgress());
    });
  }

  /**
   * "1.0x · 보통" 처럼 속도와 느림/보통/빠름을 보여준다. 0.05 단위라 홀수 칸은 소수 둘째 자리까지 표시한다.
   */
  private void showSpeechSpeed(TextView view, int step) {
    float speed = SettingsConstants.SPEED_MIN + step * SettingsConstants.SPEED_UNIT;
    String number = String.format(Locale.US, step % 2 == 0 ? "%.1f" : "%.2f", speed);
    int label = step < SettingsConstants.SPEED_STEP_DEFAULT ? R.string.settings_speed_slow
        : step > SettingsConstants.SPEED_STEP_DEFAULT ? R.string.settings_speed_fast
        : R.string.settings_speed_normal;
    view.setText(getString(R.string.settings_speed_format, number, getString(label)));
  }

  private void dismissSummary() {
    preferences.edit().putBoolean(SessionPrefs.KEY_SUMMARY_PENDING, false).apply();
    showMain();
  }
}
