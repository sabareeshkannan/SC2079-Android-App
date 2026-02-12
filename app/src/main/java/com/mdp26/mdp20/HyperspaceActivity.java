package com.mdp26.mdp20;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mdp26.mdp20.bluetooth.BluetoothMessage;
import com.mdp26.mdp20.bluetooth.BluetoothMessageParser;
import com.mdp26.mdp20.bluetooth.BluetoothMessageReceiver;

/**
 * Hyperspace Launch Activity.
 * Interaction: Scale video to fill screen.
 * Scrubbing: "Play on Drag" (High Speed) OR "Fling" to launch immediately.
 */
public class HyperspaceActivity extends AppCompatActivity {
    private static final String TAG = "HyperspaceActivity";
    private MyApplication myApp;
    private BluetoothMessageReceiver msgReceiver;

    // UI
    private VideoView videoView;
    private TextView txtHint;

    // Logic
    private boolean engaged = false;
    private int videoDuration = 0;
    private Vibrator vibrator;
    private MediaPlayer mediaPlayer; // Direct reference for speed control

    // Touch handling
    private float lastY;
    private GestureDetector gestureDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hyperspace);

        myApp = (MyApplication) getApplication();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Bluetooth Receiver
        msgReceiver = new BluetoothMessageReceiver(BluetoothMessageParser.ofDefault(), this::onMsgReceived);
        getApplicationContext().registerReceiver(msgReceiver,
                new IntentFilter(BluetoothMessageReceiver.ACTION_MSG_READ), RECEIVER_NOT_EXPORTED);

        // Setup UI
        videoView = findViewById(R.id.videoView);
        txtHint = findViewById(R.id.txtHint);

        // Check Connection
        if (myApp.btConnection() == null) {
            showConnectionError();
        }

        // Setup Video
        setupVideo();

        // Setup Interaction
        setupTouchInteraction();
    }

    private void setupVideo() {
        int resId = getResources().getIdentifier("launch_video", "raw", getPackageName());

        if (resId != 0) {
            String path = "android.resource://" + getPackageName() + "/" + resId;
            videoView.setVideoPath(path);

            videoView.setOnPreparedListener(mp -> {
                this.mediaPlayer = mp;
                videoDuration = mp.getDuration();
                mp.setLooping(false);
                adjustAspectRatio(mp);
                videoView.seekTo(1);
            });

            videoView.setOnCompletionListener(mp -> {
                if (engaged) {
                    // Slight delay to let the "arrival" frame linger
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::launchToGrid, 200);
                } else {
                    engageHyperdrive();
                }
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Video Error: " + what);
                Toast.makeText(this, "Video Error. Launching Grid...", Toast.LENGTH_SHORT).show();
                launchToGrid();
                return true;
            });
        } else {
            Toast.makeText(this, "Video 'launch_video.mp4' not found in res/raw!", Toast.LENGTH_LONG).show();
        }
    }

    private void adjustAspectRatio(MediaPlayer mp) {
        float videoWidth = mp.getVideoWidth();
        float videoHeight = mp.getVideoHeight();
        float viewWidth = videoView.getWidth();
        float viewHeight = videoView.getHeight();

        if (videoWidth / videoHeight > viewWidth / viewHeight) {
            float scale = (videoWidth / videoHeight) / (viewWidth / viewHeight);
            videoView.setScaleX(scale);
        } else {
            float scale = (viewWidth / viewHeight) / (videoWidth / videoHeight);
            videoView.setScaleY(scale);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchInteraction() {
        // Setup Gesture Detector for Fling
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // If swiped UP (negative Y velocity) fast enough
                if (velocityY < -1000) {
                    engageHyperdrive(); // BALLISTIC LAUNCH!
                    return true;
                }
                return false;
            }
        });

        View root = findViewById(android.R.id.content);
        root.setOnTouchListener((v, event) -> {
            if (engaged)
                return true;

            // Pass to gesture detector first
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float currY = event.getRawY();
                    float delta = lastY - currY; // +ve = Up

                    if (delta > 1) { // Low threshold
                        if (!videoView.isPlaying()) {
                            videoView.start();
                            // Standard 1.0f Speed for stability
                        }
                        txtHint.animate().alpha(0f).setDuration(200).start();
                    } else {
                        // Pause if stopped moving up
                        if (videoView.isPlaying())
                            videoView.pause();
                    }

                    lastY = currY;

                    // Engage if 40% done (Enough "momentum" gathered)
                    if (videoView.isPlaying() && videoView.getCurrentPosition() > videoDuration * 0.40) {
                        engageHyperdrive();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (videoView.isPlaying())
                        videoView.pause();

                    int currentPos = videoView.getCurrentPosition();
                    // Threshold reduced to 40% for release-to-engage
                    if (currentPos > videoDuration * 0.40) {
                        engageHyperdrive();
                    } else {
                        // Soft reset - snap back a bit but not all the way?
                        // Or just full reset. Full reset feels cleaner if they fail.
                        resetToStart();
                    }
                    return true;
            }
            return false;
        });
    }

    private void resetToStart() {
        if (videoView.isPlaying())
            videoView.pause();

        // Ensure speed is normal
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(1.0f));
            } catch (Exception e) {
            }
        }

        videoView.seekTo(1);
        txtHint.animate().alpha(1.0f).setDuration(300).start();
        if (vibrator != null)
            vibrator.vibrate(VibrationEffect.createOneShot(50, 50));
    }

    private void engageHyperdrive() {
        engaged = true;
        txtHint.setVisibility(View.GONE);

        if (vibrator != null)
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));

        // Set Speed to 1.0f (Compromise: Stable)
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(1.0f));
            } catch (Exception e) {
            }
        }

        if (!videoView.isPlaying()) {
            videoView.start();
        }

        if (myApp.btConnection() != null) {
            BluetoothMessage msg = BluetoothMessage.ofRobotStartMessage();
            myApp.btConnection().sendMessage(msg.getAsJsonMessage().getAsJson());
            Toast.makeText(this, "JUMP INITIATED", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Simulation Mode", Toast.LENGTH_SHORT).show();
        }

        // REMOVED immediate check. We MUST wait for OnCompletion to fire.
        // This ensures the video plays out.
    }

    private void launchToGrid() {
        startActivity(new Intent(this, CanvasActivity.class));
        finish();
    }

    private void showConnectionError() {
        new AlertDialog.Builder(this)
                .setTitle("Systems Offline")
                .setMessage("No Bluetooth connection detected.")
                .setPositiveButton("Connect", (d, w) -> {
                    finish(); // Return to BluetoothActivity
                })
                .setNegativeButton("Ignore", null)
                .show();
    }

    private void onMsgReceived(BluetoothMessage btMsg) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().unregisterReceiver(msgReceiver);
    }
}
