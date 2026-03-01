package com.mdp26.mdp20.bluetooth;

import android.util.Log;

import com.mdp26.mdp20.Facing;
import com.mdp26.mdp20.Position;
import com.mdp26.mdp20.canvas.GridObstacle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Represents a bluetooth message sent FROM the robot.
 * <p> Essentially wraps around a raw message string and provides the parsed info as getters.
 * <p> Use {@code ofXXX()} to create the subclassed record.
 */
public sealed interface BluetoothMessage permits BluetoothMessage.CustomMessage, BluetoothMessage.ObstaclesMessage, BluetoothMessage.PlainStringMessage, BluetoothMessage.RobotMoveMessage, BluetoothMessage.RobotPositionMessage, BluetoothMessage.RobotStartMessage, BluetoothMessage.RobotStatusMessage, BluetoothMessage.TargetFoundMessage, BluetoothMessage.RobotStateMessage, BluetoothMessage.ObstacleEventMessage {
    // this class uses the sealed..permit feature as a usage example, not strictly necessary

    public static final String TAG = "BluetoothMessage";

    /**
     * Provided for convenience. Use cautiously, this is an unchecked cast.
     * @return possibly null
     */
    default public JsonMessage getAsJsonMessage() {
        if (this instanceof JsonMessage)
            return (JsonMessage) this;
        return null;
    }

    /**
     * Received from RPI
     */
    public record RobotStatusMessage(String rawMsg, String status) implements BluetoothMessage {}
    public static BluetoothMessage ofRobotStatusMessage(String rawMsg, String status) {
        return new RobotStatusMessage(rawMsg, status);
    }

    /**
     * Received from RPI.
     */
    /**
     * Received from RPI.
     * Direction is -1 if not provided/applicable.
     */
    public record TargetFoundMessage(String rawMsg, int obstacleId, int targetId, int direction) implements BluetoothMessage {}
    public static BluetoothMessage ofTargetFoundMessage(String rawMsg, int obstacleId, int targetId) {
        return new TargetFoundMessage(rawMsg, obstacleId, targetId, -1);
    }
    public static BluetoothMessage ofTargetFoundMessage(String rawMsg, int obstacleId, int targetId, int direction) {
        return new TargetFoundMessage(rawMsg, obstacleId, targetId, direction);
    }

    /**
     * Received from RPI.
     */
    public record RobotPositionMessage(String rawMsg, int x, int y, int direction) implements BluetoothMessage {}
    public static BluetoothMessage ofRobotPositionMessage(String rawMsg, int x, int y, int direction) {
        return new RobotPositionMessage(rawMsg, x, y, direction);
    }

    /**
     * Any plain old string message from RPI.
     */
    public record PlainStringMessage(String rawMsg) implements BluetoothMessage {}
    public static BluetoothMessage ofPlainStringMessage(String rawMsg) {
        return new PlainStringMessage(rawMsg);
    }

    /**
     * Sent to RPI.
     */
    public record RobotMoveMessage(RobotMoveCommand cmd) implements BluetoothMessage, JsonMessage {
        @Override
        public String getAsJson() {
            return getFormattedStr("manual", cmd.value());
        }
    }
    public static BluetoothMessage ofRobotMoveMessage(RobotMoveCommand cmd) {
        return new RobotMoveMessage(cmd);
    }
    public record RobotStartMessage() implements BluetoothMessage, JsonMessage {
        @Override
        public String getAsJson() {
            return getFormattedStr("control", "start");
        }
    }
    public static BluetoothMessage ofRobotStartMessage() {
        return new RobotStartMessage();
    }

    /**
     * Sent to RPI. Robot State.
     * Format: ROBOT,<x>,<y>,<DIRECTION>
     * x: X-coordinate in cm (0-based from left, (col-2)*5).
     * y: Y-coordinate in cm (0-based from bottom, (row-1)*5).
     */
    public record RobotStateMessage(int x, int y, Facing direction) implements BluetoothMessage, JsonMessage {
        @Override
        public String getAsJson() {
            int valX = (x - 2) * 5;
            int valY = (y - 1) * 5;
            return String.format(Locale.ENGLISH, "ROBOT,%d,%d,%s", valX, valY, direction.name());
        }
    }
    public static BluetoothMessage ofRobotStateMessage(int x, int y, Facing direction) {
        return new RobotStateMessage(x, y, direction);
    }

    /**
     * Sent to RPI. Obstacle Management.
     * Format: OBSTACLE,<id>,<x>,<y>,<FACE>
     * x: X-coordinate in cm (grid index * 10).
     * y: Y-coordinate in cm (grid index * 10). Note: Top-Left origin or inverted Y-axis logic in some contexts ((19-row)*10).
     * Removal: FACE is -1.
     */
    public record ObstacleEventMessage(int id, int x, int y, Facing face, boolean isRemove) implements BluetoothMessage, JsonMessage {
        @Override
        public String getAsJson() {
            int cmX = x * 10;
            // Y: y * 10 (Bottom-Left Origin to match Python Cartesian grid)
            int cmY = y * 10;
            
            if (isRemove) {
                return String.format(Locale.ENGLISH, "OBSTACLE,%d,%d,%d,-1", id, cmX, cmY);
            } else {
                return String.format(Locale.ENGLISH, "OBSTACLE,%d,%d,%d,%s", id, cmX, cmY, face.name());
            }
        }
    }
    public static BluetoothMessage ofObstacleEventMessage(int id, int x, int y, Facing face, boolean isRemove) {
        return new ObstacleEventMessage(id, x, y, face, isRemove);
    }

    /**
     * Sent to RPI. Task Initiation.
     * Format: ALG|<x>,<y>,<dir>,<id>|...
     */
    public record ObstaclesMessage(List<GridObstacle> obstacleList) implements BluetoothMessage, JsonMessage {
        @Override
        public String getAsJson() {
            // Android sends individual "OBSTACLE" messages to the RPi memory bank.
            // When we click "Send Data", we just send "PATH" to trigger the RPi to forward its bank to the PC.
            return "PATH";
        }
    }
    public static BluetoothMessage ofObstaclesMessage(List<GridObstacle> obstacleList) {
        return new ObstaclesMessage(obstacleList);
    }

    /**
     * This non-sealed class is provided for future extension.
     */
    public abstract non-sealed class CustomMessage implements BluetoothMessage {}

}
