package com.puzzleverse.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnapParticleSystem {

    private static class Particle {
        float x, y, vx, vy;
        float alpha;
        float radius;
        int   color;
    }

    private static final int[] COLORS = {
            Color.parseColor("#66FCF1"),  // aurora cyan
            Color.parseColor("#8B5CF6"),  // aurora violet
            Color.parseColor("#45A29E"),  // aurora teal
            Color.parseColor("#2DD4BF"),  // aurora mint
            Color.parseColor("#A78BFA"),  // aurora purple
    };

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng  = new Random();

    // Spawn particles at (cx, cy) — call on group formed
    public void emit(float cx, float cy, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x      = cx;
            p.y      = cy;
            double a = rng.nextDouble() * Math.PI * 2;
            float  s = 3f + rng.nextFloat() * 5f;
            p.vx     = (float)(Math.cos(a) * s);
            p.vy     = (float)(Math.sin(a) * s);
            p.alpha  = 1f;
            p.radius = 2f + rng.nextFloat() * 3f;
            p.color  = COLORS[rng.nextInt(COLORS.length)];
            particles.add(p);
        }
    }

    // Update and draw — returns true if still animating
    public boolean updateAndDraw(Canvas canvas) {
        if (particles.isEmpty()) return false;

        List<Particle> dead = new ArrayList<>();
        for (Particle p : particles) {
            p.x     += p.vx;
            p.y     += p.vy;
            p.vx    *= 0.92f;
            p.vy    *= 0.92f;
            p.vy    += 0.15f; // gravity
            p.alpha -= 0.035f;

            if (p.alpha <= 0) { dead.add(p); continue; }

            paint.setColor(p.color);
            paint.setAlpha((int)(p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.radius, paint);
        }
        particles.removeAll(dead);
        return !particles.isEmpty();
    }

    public boolean isActive() { return !particles.isEmpty(); }
    public void clear()       { particles.clear(); }
}