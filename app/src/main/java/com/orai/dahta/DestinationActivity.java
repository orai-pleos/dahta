package com.orai.dahta;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.orai.dahta.res.R;
import com.orai.dahta.speech.DestinationRecognizer;
import com.orai.dahta.ui.Motion;

/**
 * 목적지 확인 화면. 처음에는 "어디로 모실까요?" 듣는 중 화면을 보여 주고,
 * 손님의 말이 인식되면 검색 결과 화면으로 바꾼다.
 *
 * <p>검색 결과 목록은 아직 화면 확인용 예시 데이터다(NaviHelper 에 장소 검색 API 가 없다).
 * 실제 후보가 정해지면 {@link #showResults(String)} 에서 목록을 채우면 된다.
 */
public final class DestinationActivity extends Activity implements DestinationRecognizer.Listener {
  private static final String TAG = "DestinationActivity";

  private View listening;
  private View results;
  private TextView heard;
  private TextView summary;
  private DestinationRecognizer recognizer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_destination);
    listening = findViewById(R.id.destination_listening);
    results = findViewById(R.id.destination_results);
    heard = findViewById(R.id.destination_heard);
    summary = findViewById(R.id.destination_summary);

    View listenAgain = findViewById(R.id.btn_listen_again);
    Motion.pressScale(listenAgain);
    listenAgain.setOnClickListener(view -> startListening());

    recognizer = new DestinationRecognizer(this, this);
    startListening();
  }

  @Override
  protected void onDestroy() {
    recognizer.stop();
    super.onDestroy();
  }

  /** 듣는 중 화면으로 돌아가 다시 듣는다. */
  private void startListening() {
    results.setVisibility(View.GONE);
    listening.setVisibility(View.VISIBLE);
    recognizer.start();
  }

  @Override
  public void onRecognized(String text) {
    runOnUiThread(() -> showResults(text));
  }

  @Override
  public void onFailed(String reason) {
    // 듣기에 실패하면 듣는 중 화면에 그대로 남긴다. 다시 듣기는 결과 화면의 버튼으로 한다.
    Log.w(TAG, "STT failed: " + reason);
  }

  private void showResults(String text) {
    heard.setText(text);
    summary.setText(getString(R.string.destination_result_summary_format, text));
    listening.setVisibility(View.GONE);
    results.setVisibility(View.VISIBLE);
    Motion.enter(results);
  }
}
