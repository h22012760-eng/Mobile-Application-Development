package com.puzzleverse.game;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfettiView extends View {

    private static class Particle {
        float x, y, vx, vy, rotation, rotSpeed, w, h, alpha;
        int color, shape;
    }

    // Only 2 colors — space dark and aurora cyan — at different alphas
    private static final int[] COLORS = {
            0xFF66FCF1,  // full aurora cyan
            0xCC66FCF1,  // 80% cyan
            0x9966FCF1,  // 60% cyan
            0x6666FCF1,  // 40% cyan — darker accent
            0xFF1A1A2E,  // space surface — dark contrast piece
    };

    private final List<Particle> particles = new ArrayList<>();
    private final Paint          paint     = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ConfettiView(Context ctx) { super(ctx); }

    public void launch() {
        particles.clear();
        Random rng = new Random();
        int w = getWidth(), h = getHeight();

        int i = 0;
        while (i < 80) {
            Particle p  = new Particle();
            p.x         = rng.nextFloat() * w;
            p.y         = rng.nextFloat() * h * 0.3f;
            double a    = Math.PI * 0.25 + rng.nextFloat() * Math.PI * 0.5;
            float  spd  = 8f + rng.nextFloat() * 14f;
            p.vx        = (float)(Math.cos(a) * spd) * (rng.nextBoolean() ? 1 : -1);
            p.vy        = (float)(Math.sin(a) * spd * 0.55f);
            p.rotation  = rng.nextFloat() * 360f;
            p.rotSpeed  = (rng.nextFloat() - 0.5f) * 14f;
            p.w         = 5f + rng.nextFloat() * 10f;
            p.h         = 3f + rng.nextFloat() * 6f;
            p.color     = COLORS[rng.nextInt(COLORS.length)];
            p.alpha     = 1f;
            p.shape     = rng.nextInt(3);
            particles.add(p);
            i++;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2600);
        animator.setInterpolator(new AccelerateInterpolator(0.3f));
        animator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            for (Particle p : particles) {
                p.x += p.vx; p.y += p.vy;
                p.vy += 0.55f; p.vx *= 0.98f;
                p.rotation += p.rotSpeed;
                if (frac > 0.55f) p.alpha = 1f - (frac - 0.55f) / 0.45f;
            }
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                particles.clear(); invalidate();
                if (getParent() instanceof ViewGroup)
                    ((ViewGroup) getParent()).removeView(ConfettiView.this);
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (Particle p : particles) {
            paint.setColor(p.color);
            paint.setAlpha((int)(p.alpha * Color.alpha(p.color)));
            canvas.save();
            canvas.translate(p.x, p.y);
            canvas.rotate(p.rotation);
            switch (p.shape) {
                case 0: canvas.drawRect(-p.w/2, -p.h/2, p.w/2, p.h/2, paint); break;
                case 1: canvas.drawCircle(0, 0, p.w/2, paint); break;
                case 2:
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2f);
                    canvas.drawLine(-p.w/2, 0, p.w/2, 0, paint);
                    paint.setStyle(Paint.Style.FILL);
                    break;
            }
            canvas.restore();
        }
    }
}