package com.orai.dahta.speech;

import ai.pleos.playground.stt.SpeechToText;
import ai.pleos.playground.stt.constant.ErrorType;
import ai.pleos.playground.stt.constant.Mode;
import ai.pleos.playground.stt.listener.OnInitializeListener;
import ai.pleos.playground.stt.listener.ResultListener;

import android.content.Context;

import java.util.Locale;

/**
 * 손님이 말하는 목적지를 한국어로 한 번 듣는다. Pleos STT 를 감싸서 화면은 SDK 를 몰라도 되게 한다.
 * 결과 콜백은 SDK 가 부르는 스레드에서 오므로, 화면을 바꿀 때는 메인 스레드로 옮겨야 한다.
 */
public final class DestinationRecognizer {

  /** 인식 결과를 받는 쪽. */
  public interface Listener {
    /** 문장이 끝까지 인식됐다. */
    void onRecognized(String text);

    /** 듣기에 실패했다. */
    void onFailed(String reason);
  }

  private final SpeechToText stt;
  private final Listener listener;

  public DestinationRecognizer(Context context, Listener listener) {
    this.stt = new SpeechToText(context.getApplicationContext(), Mode.HYBRID);
    this.listener = listener;
  }

  /** 듣기를 시작한다. */
  public void start() {
    stt.initialize(new OnInitializeListener() {
      @Override
      public void onSuccess() {
        stt.setLocale(Locale.KOREA);
        stt.addListener(resultListener);
        stt.request();
      }

      @Override
      public void onFailure(ErrorType errorType) {
        listener.onFailed(errorType.name());
      }
    });
  }

  /** 듣기를 멈추고 연결을 끊는다. */
  public void stop() {
    stt.removeListener(resultListener);
    stt.stop();
    stt.release();
  }

  private final ResultListener resultListener = new ResultListener() {
    @Override
    public void onUpdated(String text, boolean isFinal) {
      if (isFinal && text != null && !text.trim().isEmpty()) {
        listener.onRecognized(text.trim());
      }
    }

    @Override
    public void onError(ErrorType errorType) {
      listener.onFailed(errorType.name());
    }

    @Override
    public void onUpdatedEpdData(long begin, long end) {
    }

    @Override
    public void onStartedRecognition() {
    }

    @Override
    public void onEndedRecognition() {
    }

    @Override
    public void onReady() {
    }

    @Override
    public void onUpdatedRms(double rms) {
    }
  };
}
