package com.mdp26.mdp20.canvas;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.content.Context;
import android.widget.Toast;

import com.mdp26.mdp20.MyApplication;
import com.mdp26.mdp20.bluetooth.BluetoothMessage;
import com.mdp26.mdp20.Facing;

import java.util.Optional;

import com.mdp26.mdp20.CanvasActivity;

/**
 * Handles all touch interactions with the canvas. Namely:
 * <ul>
 * <li>If an empty cell is selected, an obstacle is placed when the finger is
 * lifted
 * Obstacle replacement is not allowed if the finger is lifted over an occupied
 * grid cell</li>
 * <li>Touch & hold in an occupied spot, then drag to move/remove obstacle</li>
 * </ul>
 * Extra: Uses vibration to feedback to the user.
 */
public class CanvasTouchController implements View.OnTouchListener {
    private final static String TAG = "CanvasTouchController";
    private final Grid grid;
    private final MyApplication myApp;
    private final CanvasActivity activity;
    private Optional<GridObstacle> selectedObstacle = Optional.empty();
    private final int SELECTION_RADIUS;
    private static final float SELECTION_RADIUS_DP = 2f;

    // to track x and y touched down on
    private int downX = 0, downY = 0;

    public CanvasTouchController(CanvasActivity activity, MyApplication myApp) {
        this.activity = activity;
        this.myApp = myApp;
        this.grid = myApp.grid();
        this.SELECTION_RADIUS = convertDpToPx(myApp.getApplicationContext(), SELECTION_RADIUS_DP);
    }

    private static int convertDpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        CanvasView canvasView = (CanvasView) v;
        int x = (int) ((event.getX() - canvasView.getOffsetX()) / canvasView.getCellSize());
        int y = (int) ((event.getY() - canvasView.getOffsetY()) / canvasView.getCellSize());
        y = (Grid.GRID_SIZE - 1) - y; // Flip Y to match bottom-left origin

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                Log.d(TAG, "Touched down at (" + downX + ", " + downY + ")");
                if (grid.isInsideGrid(downX, downY)) {
                    selectedObstacle = grid.findObstacleWithApproxPos(downX, downY, SELECTION_RADIUS);
                    selectedObstacle.ifPresent(obst -> {
                        Log.d(TAG, "Selected obstacle at " + obst.getPosition());
                        obst.setSelected(true);
                        canvasView.invalidate();
                    });
                }
                break;

            case MotionEvent.ACTION_UP:
                v.performClick();
                Log.d(TAG, "Touched up at (" + x + ", " + y + ")");
                if (selectedObstacle.isPresent()) {
                    GridObstacle obstacle = selectedObstacle.get();
                    obstacle.setSelected(false);
                    int oldX = obstacle.getPosition().getXInt();
                    int oldY = obstacle.getPosition().getYInt();
                    Log.d(TAG, downX + " " + downY + " " + x + " " + y);
                    if (downX == x && downY == y) { // if the finger is lifted on the same cell
                        // Rotate obstacle clockwise if lifted on the same cell
                        obstacle.rotateClockwise();
                        Log.d(TAG, "Rotated obstacle clockwise at " + obstacle.getPosition());
                        canvasView.invalidate(); // Refresh canvas
                        rebuildRemoteMap();
                    } else if (!grid.isInsideGrid(x, y)) { // if finger lifted outside of grid
                        // Remove if lifted outside the grid
                        grid.removeObstacle(oldX, oldY);
                        Log.d(TAG, "Removed obstacle at (" + oldX + ", " + oldY + ")");
                        canvasView.invalidate(); // Refresh canvas
                        rebuildRemoteMap();
                    } else if (!grid.hasObstacle(x, y)) { // if finger lifted on empty cell
                        // Move obstacle only if lifted on an empty cell
                        obstacle.updatePosition(x, y);
                        Log.d(TAG, "Moved obstacle from (" + oldX + ", " + oldY + ") to (" + x + ", " + y + ")");
                        Toast.makeText(myApp, "Moved obst to (" + x + ", " + y + ")", Toast.LENGTH_SHORT).show();
                        canvasView.invalidate(); // Refresh canvas
                        rebuildRemoteMap();
                    }
                } else {
                    // If no obstacle was selected, add a new one
                    if (grid.isInsideGrid(x, y) && !grid.hasObstacle(x, y)) {
                        GridObstacle obstacle = GridObstacle.of(x, y);
                        grid.addObstacle(obstacle);
                        Log.d(TAG, "Added new obstacle at (" + x + ", " + y + ")");
                        Toast.makeText(myApp, "Added obst at (" + x + ", " + y + ")", Toast.LENGTH_SHORT).show();
                        canvasView.invalidate(); // Refresh canvas
                        rebuildRemoteMap();
                    }
                }
                selectedObstacle = Optional.empty(); // Clear selection
                break;
    }
        return true;
    }

    private void rebuildRemoteMap() {
        if (myApp.btConnection() == null) return;
        
        // Wipe remote RPi Bank
        String strClear = "CLEAR\n";
        myApp.btConnection().sendMessage(strClear);
        activity.logMessage("SENT", "CLEAR", "#00BCD4");
        
        // Broadcast clean map sequentially
        for (GridObstacle obs : grid.getObstacleList()) {
            BluetoothMessage msg = BluetoothMessage.ofObstacleEventMessage(
                obs.getId(), 
                obs.getPosition().getXInt(), 
                obs.getPosition().getYInt(), 
                obs.getFacing(), 
                false
            );
            String msgStr = msg.getAsJsonMessage().getAsJson() + "\n";
            myApp.btConnection().sendMessage(msgStr);
            activity.logMessage("SENT", msg.getAsJsonMessage().getAsJson(), "#00BCD4");
        }
    }
}
