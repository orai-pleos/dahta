package com.orai.dahta;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.orai.dahta.res.R;

public class MainActivity extends AppCompatActivity {
  private SharedPreferences preferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    preferences = getSharedPreferences("drive_session", MODE_PRIVATE);
    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        if (preferences.getBoolean("running", false)) {
          // Leaving the app does not silently end an active session.
          moveTaskToBack(true);
        } else if (preferences.getBoolean("summary_pending", false)) {
          dismissSummary();
        } else {
          finish();
        }
      }
    });
    renderSession();
  }

  private void renderSession() {
    if (preferences.getBoolean("running", false)) {
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
    careSwitch.setChecked(preferences.getBoolean("care_mode", true));
    careSwitch.setOnCheckedChangeListener((button, checked) ->
        preferences.edit().putBoolean("care_mode", checked).apply());
    findViewById(R.id.btn_start).setOnClickListener(view -> startSession());
    boolean pending = preferences.getBoolean("summary_pending", false);
    findViewById(R.id.summary_overlay).setVisibility(pending ? View.VISIBLE : View.GONE);
    if (pending) {
      long minutes = preferences.getLong("duration_ms", 0) / 60000;
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
    preferences.edit().putBoolean("running", true).putBoolean("summary_pending", false)
        .putLong("started_at", System.currentTimeMillis()).apply();
    showWaiting();
  }

  private void showWaiting() {
    setScreen(R.layout.activity_waiting);
    boolean careMode = preferences.getBoolean("care_mode", true);
    ((TextView) findViewById(R.id.care_status_text)).setText(
        careMode ? R.string.care_status_on : R.string.care_status_off);
    findViewById(R.id.care_status).setBackgroundResource(
        careMode ? R.drawable.bg_care_enabled : R.drawable.bg_care_disabled);
    findViewById(R.id.care_status_dot).setAlpha(careMode ? 1f : 0.3f);
    findViewById(R.id.btn_end_drive).setOnClickListener(view -> endSession());
  }

  private void endSession() {
    long duration = Math.max(0, System.currentTimeMillis()
        - preferences.getLong("started_at", System.currentTimeMillis()));
    preferences.edit().putBoolean("running", false).putBoolean("summary_pending", true)
        .putLong("duration_ms", duration).remove("started_at").apply();
    showMain();
  }

  private void dismissSummary() {
    preferences.edit().putBoolean("summary_pending", false).apply();
    showMain();
  }
}
