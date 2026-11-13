package com.orai.dahta;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.orai.dahta.res.R;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_canvas), (view, windowInsets) -> {
      Insets safeArea = windowInsets.getInsets(
          WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      view.setPadding(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom);
      return windowInsets;
    });
    ViewCompat.requestApplyInsets(findViewById(R.id.main_canvas));

    setupCareMode();

    findViewById(R.id.btn_start).setOnClickListener(v ->
        Toast.makeText(this, R.string.start_drive, Toast.LENGTH_SHORT).show());
  }

  private void setupCareMode() {
    CompoundButton careSwitch = findViewById(R.id.switch_care_mode);
    careSwitch.setOnCheckedChangeListener((button, checked) -> {
      // TODO: 배려 모드 설정
    });
  }
}
