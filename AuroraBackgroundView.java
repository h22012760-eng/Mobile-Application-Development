package com.puzzleverse.game;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AuroraBackgroundView extends View {

    // Removed unused COL_SPACE and COL_CYAN constants

    private float w1x = 0f, w1y = 0f;
    private float w2x = 0f, w2y = 0f;
    private float starAlpha = 0.4f;

    private ValueAnimator wave1Anim, wave2Anim, starAnim;

    private static class Star {
        float x, y, r, phase;
    }

    private final List<Star> stars    = new ArrayList<>();
    private boolean          starsGen = false;

    private final Paint bgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Reusable colors for gradients
    private final int[] bgColors = {
            0xFF0D0D1A,   // deep space center
            0xFF08080F,   // mid
            0xFF030308    // edge — almost black
    };
    private final float[] bgPositions = {0f, 0.55f, 1f};

    private final int[] wave1Colors = {
            Color.argb(0,   102, 252, 241),  // transparent
            Color.argb(28,  102, 252, 241),  // soft cyan glow
            Color.argb(14,  102, 252, 241),  // fade out
            Color.argb(0,   102, 252, 241)   // transparent
    };
    private final float[] wave1Positions = {0f, 0.3f, 0.7f, 1f};

    private final int[] wave2Colors = {
            Color.argb(0,  102, 252, 241),
            Color.argb(22, 102, 252, 241),
            Color.argb(10, 102, 252, 241),
            Color.argb(0,  102, 252, 241)
    };
    private final float[] wave2Positions = {0f, 0.35f, 0.65f, 1f};

    public AuroraBackgroundView(Context ctx) { super(ctx); init(); }
    public AuroraBackgroundView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public AuroraBackgroundView(Context ctx, AttributeSet a, int def) { super(ctx, a, def); init(); }

    private void init() {
        bgPaint.setStyle(Paint.Style.FILL);
        wavePaint.setStyle(Paint.Style.FILL);
        starPaint.setStyle(Paint.Style.FILL);
        if (!isInEditMode()) {
            startAnimations();
        }
    }

    private void startAnimations() {
        // Wave 1 — 18 seconds (spec)
        wave1Anim = ValueAnimator.ofFloat(0f, 1f);
        wave1Anim.setDuration(18000);
        wave1Anim.setRepeatCount(ValueAnimator.INFINITE);
        wave1Anim.setRepeatMode(ValueAnimator.REVERSE);
        wave1Anim.setInterpolator(new LinearInterpolator());
        wave1Anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            w1x = f * 130f - 65f;
            w1y = (float)(Math.sin(f * Math.PI) * 90f);
            invalidate();
        });
        wave1Anim.start();

        // Wave 2 — 22 seconds (spec)
        wave2Anim = ValueAnimator.ofFloat(0f, 1f);
        wave2Anim.setDuration(22000);
        wave2Anim.setRepeatCount(ValueAnimator.INFINITE);
        wave2Anim.setRepeatMode(ValueAnimator.REVERSE);
        wave2Anim.setInterpolator(new LinearInterpolator());
        wave2Anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            w2x = -f * 110f + 55f;
            w2y = (float)(Math.cos(f * Math.PI) * 70f);
            invalidate();
        });
        wave2Anim.start();

        // Star twinkle — 4 seconds (spec)
        starAnim = ValueAnimator.ofFloat(0.25f, 0.65f);
        starAnim.setDuration(4000);
        starAnim.setRepeatCount(ValueAnimator.INFINITE);
        starAnim.setRepeatMode(ValueAnimator.REVERSE);
        starAnim.addUpdateListener(a -> starAlpha = (float) a.getAnimatedValue());
        starAnim.start();
    }

    private void genStars(int w, int h) {
        if (starsGen) return;
        starsGen = true;
        Random rng = new Random(7);
        for (int i = 0; i < 90; i++) {
            Star s = new Star();
            s.x     = rng.nextFloat() * w;
            s.y     = rng.nextFloat() * h;
            s.r     = 0.6f + rng.nextFloat() * 1.6f;
            s.phase = rng.nextFloat(); // individual twinkle offset
            stars.add(s);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        genStars(w, h);

        // ── Deep space radial gradient base ─────────────────────────
        // #0D0D1A center → #030308 edge
        bgPaint.setShader(new RadialGradient(
                w * 0.5f, h * 0.35f,
                Math.max(w, h) * 0.9f,
                bgColors,
                bgPositions,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, bgPaint);

        // ── Stars — cyan-tinted white dots ──────────────────────────
        for (Star s : stars) {
            // Each star has a slightly different phase for organic feel
            float a = starAlpha * (0.5f + 0.5f * s.phase);
            starPaint.setColor(Color.argb((int)(a * 200), 102, 252, 241));
            canvas.drawCircle(s.x, s.y, s.r, starPaint);
        }

        // ── Aurora wave 1 — cyan glow sweeping upper portion ────────
        float w1StartX = w * 0.1f + w1x;
        float w1StartY = h * 0.05f + w1y;
        wavePaint.setShader(new LinearGradient(
                w1StartX, w1StartY,
                w1StartX + w * 0.9f, w1StartY + h * 0.45f,
                wave1Colors,
                wave1Positions,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, wavePaint);

        // ── Aurora wave 2 — second cyan band from opposite angle ─────
        float w2StartX = w * 0.7f + w2x;
        float w2StartY = h * 0.35f + w2y;
        wavePaint.setShader(new LinearGradient(
                w2StartX, w2StartY,
                w2StartX - w * 0.7f, w2StartY + h * 0.5f,
                wave2Colors,
                wave2Positions,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, wavePaint);
    }

    public void pauseAnimations() {
        if (wave1Anim != null && wave1Anim.isRunning()) wave1Anim.pause();
        if (wave2Anim != null && wave2Anim.isRunning()) wave2Anim.pause();
        if (starAnim  != null && starAnim.isRunning())  starAnim.pause();
    }

    public void resumeAnimations() {
        if (wave1Anim != null && wave1Anim.isPaused()) wave1Anim.resume();
        if (wave2Anim != null && wave2Anim.isPaused()) wave2Anim.resume();
        if (starAnim  != null && starAnim.isPaused())  starAnim.resume();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (wave1Anim != null) wave1Anim.cancel();
        if (wave2Anim != null) wave2Anim.cancel();
        if (starAnim  != null) starAnim.cancel();
    }
}
