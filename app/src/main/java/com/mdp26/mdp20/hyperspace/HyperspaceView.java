package com.mdp26.mdp20.hyperspace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.mdp26.mdp20.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HyperspaceView extends View {
    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint();
    private float warpFactor = 0.0f; // 0.0 to 1.0 (1.0 = full warp)
    private float centerX, centerY;
    private long lastTime = 0;

    public HyperspaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        // Initialize stars
        for (int i = 0; i < 200; i++) {
            resetStar(new Star());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Background color
        canvas.drawColor(getContext().getColor(R.color.bg_dark)); // Should be deep space blue

        long now = System.currentTimeMillis();
        float deltaTime = (lastTime == 0) ? 0 : (now - lastTime) / 1000f;
        lastTime = now;

        float speedBase = 0.5f + (warpFactor * 30.0f); // Fast acceleration

        for (Star star : stars) {
            // Update star position
            star.z -= speedBase * deltaTime * 500; 

            if (star.z <= 1) {
                resetStar(star);
                star.z = 1000;
                star.pz = star.z;
            }

            // Project 3D to 2D
            float k = 128.0f / star.z;
            float px = star.x * k + centerX;
            float py = star.y * k + centerY;

            // Previous position for trails
            float pk = 128.0f / star.pz;
            float ppx = star.x * pk + centerX;
            float ppy = star.y * pk + centerY;

            star.pz = star.z;

            // Draw trail if moving fast (warp)
            paint.setStrokeWidth(warpFactor * 5 + 2);
            paint.setAlpha((int) (255 * (1 - star.z / 1000f)));

            if (px >= 0 && px <= getWidth() && py >= 0 && py <= getHeight()) {
                if (warpFactor > 0.1f) {
                    canvas.drawLine(ppx, ppy, px, py, paint);
                } else {
                    canvas.drawCircle(px, py, 2, paint);
                }
            } else {
                // Out of bounds, reset
                resetStar(star);
            }
        }

        if (warpFactor > 0) {
            invalidate(); // Animate
        }
    }

    private void resetStar(Star star) {
        star.x = (random.nextFloat() - 0.5f) * getWidth() * 10; // spread wide
        star.y = (random.nextFloat() - 0.5f) * getHeight() * 10;
        star.z = 1000; // far away
        star.pz = star.z;
        if (stars.size() < 200) stars.add(star);
    }

    public void setWarpFactor(float factor) {
        this.warpFactor = factor;
        invalidate();
    }

    private static class Star {
        float x, y, z, pz;
    }
}
