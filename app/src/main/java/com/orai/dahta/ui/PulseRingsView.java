package com.orai.dahta.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.orai.dahta.res.R;

/**
 * 파란 원 둘레로 번지는 고리. 단계마다 모양이 다르다.
 * <ul>
 *   <li>0 (멀리): 얇은 고리 3겹</li>
 *   <li>1 (가까이): 가운데 원 바로 밖에 짙은 띠 + 바깥에 옅은 굵은 고리</li>
 *   <li>2 (도착): 색이 점점 옅어지는 면 3겹</li>
 * </ul>
 * 안쪽부터 차례로 짙어졌다 옅어지기를 되풀이해 와이파이처럼 번지는 느낌을 준다.
 * 가운데 아이콘(화살표 / 핀)은 이 뷰 위에 다른 뷰로 올린다.
 */
public final class PulseRingsView extends View {

  /**
   * 시안 아이콘 틀(224 x 224)을 기준으로 한 값. 실제 크기는 뷰 폭에 맞춰 비율로 줄이거나 늘린다.
   * 레이아웃에서 이 뷰(고리 + 아이콘)는 112dp x 112dp 로 쓴다.
   */
  private static final float DESIGN_SIZE = 224f;

  /** 고리 하나. stroke 가 0 이면 반지름까지 꽉 찬 원(면)이다. 길이는 모두 시안 기준. */
  private static final class Ring {
    final float radius;
    final float stroke;
    final float alpha;

    Ring(float radius, float stroke, float alpha) {
      this.radius = radius;
      this.stroke = stroke;
      this.alpha = alpha;
    }
  }

  /** 단계별 고리. 안쪽부터. 면은 바깥 것을 먼저 그려야 하므로 그릴 때 거꾸로 돈다. */
  private static final Ring[][] LEVELS = {
      // 멀리: 얇은 고리 3겹
      {new Ring(83.5f, 3.6f, 0.22f), new Ring(95.5f, 3.6f, 0.17f), new Ring(107.5f, 3.6f, 0.12f)},
      // 가까이: 짙은 띠(면) + 바깥 옅은 굵은 고리
      {new Ring(89f, 0f, 0.62f), new Ring(104f, 8.5f, 0.16f)},
      // 도착: 면 3겹
      {new Ring(84.5f, 0f, 0.78f), new Ring(98f, 0f, 0.56f), new Ring(111f, 0f, 0.38f)},
  };
  private static final float CORE_RADIUS = 70f;
  private static final float CORE_STROKE_RADIUS = 64f;
  private static final float CORE_STROKE_WIDTH = 2f;
  private static final long PERIOD_MS = 1800;
  private static final float RING_DELAY = 0.16f;
  /** 번지는 정도. 1 이면 투명도가 0 까지 오르내리고, 작을수록 은은하다. */
  private static final float PULSE_DEPTH = 0.45f;

  private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint coreStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int blue;
  private int level;
  private float progress;
  private ValueAnimator animator;

  public PulseRingsView(Context context, AttributeSet attrs) {
    super(context, attrs);
    blue = ContextCompat.getColor(context, R.color.informative_positive);
    corePaint.setStyle(Paint.Style.FILL);
    corePaint.setColor(blue);
    coreStrokePaint.setStyle(Paint.Style.STROKE);
    coreStrokePaint.setColor(0xFFFFFFFF);
  }

  /** 고리 모양. 0: 멀리, 1: 가까이, 2: 도착. */
  public void setLevel(int newLevel) {
    level = Math.max(0, Math.min(newLevel, LEVELS.length - 1));
    invalidate();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(PERIOD_MS);
    animator.setInterpolator(new LinearInterpolator());
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.addUpdateListener(animation -> {
      progress = (float) animation.getAnimatedValue();
      invalidate();
    });
    animator.start();
  }

  @Override
  protected void onDetachedFromWindow() {
    if (animator != null) {
      animator.cancel();
      animator = null;
    }
    super.onDetachedFromWindow();
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    float cx = getWidth() / 2f;
    float cy = getHeight() / 2f;
    float unit = Math.min(getWidth(), getHeight()) / DESIGN_SIZE;
    Ring[] rings = LEVELS[level];
    // 바깥 고리부터 그려야 안쪽 면이 위에 얹힌다.
    for (int i = rings.length - 1; i >= 0; i--) {
      Ring ring = rings[i];
      // 안쪽 고리부터 RING_DELAY 만큼씩 늦게 짙어졌다 옅어진다.
      double wave = 0.5 + 0.5 * Math.sin(2 * Math.PI * (progress - i * RING_DELAY));
      float alpha = ring.alpha * (1f - PULSE_DEPTH + PULSE_DEPTH * (float) wave);
      ringPaint.setColor(blue);
      ringPaint.setAlpha(Math.round(255 * alpha));
      if (ring.stroke <= 0f) {
        ringPaint.setStyle(Paint.Style.FILL);
      } else {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(ring.stroke * unit);
      }
      canvas.drawCircle(cx, cy, ring.radius * unit, ringPaint);
    }
    canvas.drawCircle(cx, cy, CORE_RADIUS * unit, corePaint);
    coreStrokePaint.setStrokeWidth(CORE_STROKE_WIDTH * unit);
    canvas.drawCircle(cx, cy, CORE_STROKE_RADIUS * unit, coreStrokePaint);
  }
}
