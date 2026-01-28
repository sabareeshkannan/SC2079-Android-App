package com.mdp26.mdp20;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.mdp26.mdp20.bluetooth.BluetoothMessage;
import com.mdp26.mdp20.bluetooth.BluetoothMessageParser;
import com.mdp26.mdp20.bluetooth.BluetoothMessageReceiver;
import com.mdp26.mdp20.hyperspace.HyperspaceView;

/**
 * Provides a big red button for task 2.
 * Equivalent to pressing "start" on {@link CanvasActivity}.
 */
public class HyperspaceActivity extends AppCompatActivity {
    private MyApplication myApp;
    private BroadcastReceiver msgReceiver;
    private HyperspaceView hyperspaceView;
    private TextView txtPercent;
    private View throttleFill;
    private View throttleHandle;
    private View throttleContainer;
    private TextView txtEngage;

    // Effects
    private Vibrator vibrator;
    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private boolean engaged = false;

    // Colors
    private int colorStart;
    private int colorMid;
    private int colorEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hyperspace);

        // Colors
        colorStart = ContextCompat.getColor(this, R.color.primary);
        colorMid = Color.parseColor("#9C27B0"); // Purple
        colorEnd = ContextCompat.getColor(this, R.color.accent); // Red/Orange

        // Haptics
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        myApp = (MyApplication) getApplication();

        msgReceiver = new BluetoothMessageReceiver(BluetoothMessageParser.ofDefault(), this::onMsgReceived);
        getApplicationContext().registerReceiver(msgReceiver, new IntentFilter(BluetoothMessageReceiver.ACTION_MSG_READ), RECEIVER_NOT_EXPORTED);

        hyperspaceView = findViewById(R.id.hyperspaceView);
        txtPercent = findViewById(R.id.txtPercent);
        txtEngage = findViewById(R.id.txtEngage);
        
        throttleContainer = findViewById(R.id.throttleContainer);
        throttleHandle = findViewById(R.id.throttleHandle);
        throttleFill = findViewById(R.id.throttleFill);

        setupThrottle();
    }

    private void setupThrottle() {
        throttleContainer.setOnTouchListener(new View.OnTouchListener() {
            float startY;
            float handleStartY;
            boolean isDragging = false;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (engaged) return true; // Block interaction once engaged

                float containerHeight = v.getHeight();
                float handleHeight = throttleHandle.getHeight();
                float maxTravel = containerHeight - handleHeight; 
                
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        handleStartY = throttleHandle.getTranslationY();
                        isDragging = true;
                        
                        // Initial haptic
                         if (vibrator != null) vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                        return true;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (!isDragging) return false;
                        float currentY = event.getRawY();
                        float diff = currentY - startY;
                        float newTransY = handleStartY + diff;
                        
                        // Clamp
                        if (newTransY > 0) newTransY = 0; // Bottom limit
                        if (newTransY < -maxTravel) newTransY = -maxTravel; // Top limit
                        
                        throttleHandle.setTranslationY(newTransY);
                        
                        // Calculate percentage (0 at bottom, 1 at top)
                        float progress = Math.abs(newTransY) / maxTravel;
                        updateWarpVisuals(progress, maxTravel);
                        
                        // Auto-Engage if pushed all the way up (punch it!)
                        if (progress > 0.96f) {
                            engaged = true;
                            throttleHandle.setTranslationY(-maxTravel); // Snap to top
                            updateWarpVisuals(1.0f, maxTravel);
                            engageHyperdrive();
                        }
                        return true;
                        
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        float finalTransY = throttleHandle.getTranslationY();
                        float finalProgress = Math.abs(finalTransY) / maxTravel;
                        
                        // Fallback: If they released high enough but didn't trigger auto-engage
                        if (finalProgress > 0.90f) {
                            engaged = true;
                            throttleHandle.animate().translationY(-maxTravel).setDuration(100).start();
                            updateWarpVisuals(1.0f, maxTravel);
                            engageHyperdrive();
                        } else {
                            // Snap back
                            throttleHandle.animate().translationY(0).setDuration(300).setInterpolator(new android.view.animation.BounceInterpolator()).start();
                            // Reset
                            updateWarpVisuals(0f, maxTravel);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void updateWarpVisuals(float progress, float maxTravel) {
        hyperspaceView.setWarpFactor(progress);
        int percent = (int) (progress * 100);
        
        txtPercent.setText(percent + "%");
        
        // Update fill height
        android.view.ViewGroup.LayoutParams params = throttleFill.getLayoutParams();
        params.height = (int) (progress * maxTravel) + 50; 
        throttleFill.setLayoutParams(params);

        // Dynamic Color Shift
        int color;
        if (progress < 0.5f) {
            // Blue to Purple
            float ratio = progress / 0.5f;
            color = (int) argbEvaluator.evaluate(ratio, colorStart, colorMid);
        } else {
            // Purple to Red
            float ratio = (progress - 0.5f) / 0.5f;
            color = (int) argbEvaluator.evaluate(ratio, colorMid, colorEnd);
        }
        throttleFill.setBackgroundColor(color);
        txtPercent.setTextColor(color);
        txtEngage.setTextColor(color);
        
        // Dynamic Haptics (Rumble increases with percentage)
        if (progress > 0.1f && vibrator != null) {
            // Only vibrate periodically or use amplitude control if supported
             if (Math.random() < progress * 0.3) { // Random flutter based on intensity
                 vibrator.vibrate(VibrationEffect.createOneShot(10, (int)(progress * 255)));
             }
        }
    }

    private void engageHyperdrive() {
        if (myApp.btConnection() != null){
            // Heavy Rumble
            if (vibrator != null) vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));

            // Screen Shake
            ObjectAnimator shake = ObjectAnimator.ofFloat(throttleContainer, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
            shake.setDuration(500);
            shake.start();

            BluetoothMessage msg = BluetoothMessage.ofRobotStartMessage();
            myApp.btConnection().sendMessage(msg.getAsJsonMessage().getAsJson());
            Toast.makeText(this, "HYPERDRIVE ENGAGED", Toast.LENGTH_SHORT).show();
            
            // Wait for visual effect then launch Command Center
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new android.content.Intent(this, CanvasActivity.class));
                finish(); // Close launch screen
            }, 800);
            
        } else {
             // OFFLINE - REJECT
             Toast.makeText(this, "Systems Offline (No Bluetooth)", Toast.LENGTH_SHORT).show();
             
             // Shake "No"
             ObjectAnimator shake = ObjectAnimator.ofFloat(throttleContainer, "translationX", 0, 10, -10, 10, -10, 0);
             shake.setDuration(400);
             shake.setInterpolator(new CycleInterpolator(1));
             shake.start();

             // Reset UI
             new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                 View throttleHandle = findViewById(R.id.throttleHandle);
                 View throttleFill = findViewById(R.id.throttleFill);
                 if (throttleHandle != null) {
                     throttleHandle.animate().translationY(0).setDuration(400).setInterpolator(new android.view.animation.BounceInterpolator()).start();
                 }
                 if (throttleFill != null) {
                    android.view.ViewGroup.LayoutParams params = throttleFill.getLayoutParams();
                    params.height = 0;
                    throttleFill.setLayoutParams(params);
                 }
                 hyperspaceView.setWarpFactor(0);
                 engaged = false; // Allow retry
             }, 300);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnectionState();
    }

    private void checkConnectionState() {
        if (myApp.btConnection() == null) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("No Connected Device")
                .setMessage("Please connect to a robot via Bluetooth before initiating launch.")
                .setPositiveButton("Connect", (dialog, which) -> {
                     finish(); // Go back to Bluetooth activity
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
        txtPercent.setText("0%");
        txtPercent.setTextColor(colorStart);
    }

    private void onMsgReceived(BluetoothMessage btMsg) {
        // Optional: React to robot ready status
    }
}
