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

/**
 * Handles all touch interactions with the canvas. Namely:
 * <ul>
 *     <li>If an empty cell is selected, an obstacle is placed when the finger is lifted
 *     Obstacle replacement is not allowed if the finger is lifted over an occupied grid cell</li>
 *     <li>Touch & hold in an occupied spot, then drag to move/remove obstacle</li>
 * </ul>
 * Extra: Uses vibration to feedback to the user.
 */
public class CanvasTouchController implements View.OnTouchListener {
    private final static String TAG = "CanvasTouchController";
    private final Grid grid;
    private final MyApplication myApp;
    private Optional<GridObstacle> selectedObstacle = Optional.empty();
    private final int SELECTION_RADIUS;

    // to track x and y touched down on
    private int downX = 0, downY = 0;

    public CanvasTouchController(MyApplication myApp) {
        this.myApp = myApp;
        this.grid = myApp.grid();
        this.SELECTION_RADIUS = convertDpToPx(myApp.getApplicationContext(), 2); // 2dp
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
                final int upX = x; //for readability
                final int upY = y; //for readability
                Log.d(TAG, "Touched up at (" + upX + ", " + upY + ")");
                if (selectedObstacle.isPresent()) {
                    GridObstacle obstacle = selectedObstacle.get();
                    obstacle.setSelected(false);
                    int oldX = obstacle.getPosition().getXInt();
                    int oldY = obstacle.getPosition().getYInt();
                    Log.d(TAG, downX + " " + downY + " " + upX + " " + upY);
                    if (downX == upX && downY == upY) { // if the finger is lifted on the same cell
                        // Send REMOVE first!
                        if (myApp.btConnection() != null) {
                            BluetoothMessage msgRemove = BluetoothMessage.ofObstacleEventMessage(obstacle.getId(), oldX, oldY, obstacle.getFacing(), true);
                            myApp.btConnection().sendMessage(msgRemove.getAsJsonMessage().getAsJson());
                        }

                        // Rotate obstacle clockwise if lifted on the same cell
                        obstacle.rotateClockwise();
                        // Send update
                        if (myApp.btConnection() != null) {
                            BluetoothMessage msg = BluetoothMessage.ofObstacleEventMessage(obstacle.getId(), obstacle.getPosition().getXInt(), obstacle.getPosition().getYInt(), obstacle.getFacing(), false);
                            myApp.btConnection().sendMessage(msg.getAsJsonMessage().getAsJson());
                        }
                        Log.d(TAG, "Rotated obstacle clockwise at " + obstacle.getPosition());
                        canvasView.invalidate(); // Refresh canvas
                    } else if (!grid.isInsideGrid(upX, upY)) { // if finger lifted outside of grid
                        // Remove if lifted outside the grid
                        grid.removeObstacle(oldX, oldY);
                        // Send remove command (Using -1 or REMOVE keyword)
                         if (myApp.btConnection() != null) {
                            BluetoothMessage msg = BluetoothMessage.ofObstacleEventMessage(obstacle.getId(), oldX, oldY, obstacle.getFacing(), true);
                            myApp.btConnection().sendMessage(msg.getAsJsonMessage().getAsJson());
                         }
                        Log.d(TAG, "Removed obstacle at (" + oldX + ", " + oldY + ")");
                        canvasView.invalidate(); // Refresh canvas
                    } else if (!grid.hasObstacle(upX, upY)) { // if finger lifted on empty cell
                        // Send REMOVE first from the old position
                        if (myApp.btConnection() != null) {
                            BluetoothMessage msgRemove = BluetoothMessage.ofObstacleEventMessage(obstacle.getId(), oldX, oldY, obstacle.getFacing(), true);
                            myApp.btConnection().sendMessage(msgRemove.getAsJsonMessage().getAsJson());
                        }

                        // Move obstacle only if lifted on an empty cell
                        obstacle.updatePosition(upX, upY);
                        // Send move update
                        if (myApp.btConnection() != null) {
                             BluetoothMessage msg = BluetoothMessage.ofObstacleEventMessage(obstacle.getId(), upX, upY, obstacle.getFacing(), false);
                             myApp.btConnection().sendMessage(msg.getAsJsonMessage().getAsJson());
                        }
                        Log.d(TAG, "Moved obstacle from (" + oldX + ", " + oldY + ") to (" + upX + ", " + upY + ")");
                        Toast.makeText(myApp, "Moved obst to (" + upX + ", " + upY + ")", Toast.LENGTH_SHORT).show();
                        canvasView.invalidate(); // Refresh canvas
                    }
                } else {
                    // If no obstacle was selected, add a new one
                    if (grid.isInsideGrid(upX, upY) && !grid.hasObstacle(upX, upY)) {
                        GridObstacle obstacle = GridObstacle.of(upX, upY);
                        grid.addObstacle(obstacle);
                        // Send add update
                        if (myApp.btConnection() != null) {
                             // Default facing is NORTH
                             BluetoothMessage msg = BluetoothMessage.ofObstacleEventMessage(obstacle.getId(), upX, upY, Facing.NORTH, false);
                             myApp.btConnection().sendMessage(msg.getAsJsonMessage().getAsJson());
                        }
                        Log.d(TAG, "Added new obstacle at (" + upX + ", " + upY + ")");
                        Toast.makeText(myApp, "Added obst at (" + upX + ", " + upY + ")", Toast.LENGTH_SHORT).show();
                        canvasView.invalidate(); // Refresh canvas
                    }
                }
                selectedObstacle = Optional.empty(); // Clear selection
                break;
        }
        return true;
    }

    private String convertFacingToInt(com.mdp26.mdp20.Facing facing) {
        return String.valueOf(facing.getMappedCode());
    }
}
