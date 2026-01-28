package com.mdp26.mdp20.bluetooth;

import android.util.Log;

import java.util.function.Function;

/**
 * Interface for a message parser. Has a default implementation {@link #ofDefault}.
 */
public interface BluetoothMessageParser extends Function<String, BluetoothMessage> {

    public static final String TAG = "BluetoothMessageParser";

    /**
     * Default message format is {@code "command;param1;param2" }.
     * <p> Expected messages:
     * <ul>
     *     <li>info</li>
     *     <li>error</li>
     *     <li>mode</li>
     *     <li>status</li>
     *     <li>location</li>
     *     <li>image-rec</li>
     * </ul>
     */
    BluetoothMessageParser DEFAULT = msg -> {
        // Handle comma-separated or semi-colon separated
        String[] params;
        if (msg.contains(",")) params = msg.split(",");
        else params = msg.split(";");

        if (params.length > 1) {
            String command = params[0].toUpperCase().trim(); // normalize
            BluetoothMessage ret;
            // Expected format: ROBOT,<x>,<y>,<direction>
            // Expected format: TARGET,<ObstacleID>,<TargetID>
            switch (command) {
                case "INFO" -> ret = BluetoothMessage.ofPlainStringMessage("[info] " + params[1]);
                case "ERROR" -> ret = BluetoothMessage.ofPlainStringMessage("[error] " + params[1]);
                case "MODE" -> ret = BluetoothMessage.ofPlainStringMessage("[mode] " + params[1]);
                case "STATUS" -> ret = BluetoothMessage.ofRobotStatusMessage(msg, params[1]);
                case "ROBOT", "LOCATION" -> {
                    // ROBOT,<x>,<y>,<direction>
                    if(params.length >= 4) {
                        try {
                            int x = Integer.parseInt(params[1].trim());
                            int y = Integer.parseInt(params[2].trim());
                            // direction is a string (N/S/E/W) or int? 
                            // The RobotPositionMessage expects an int direction code usually, OR we parse N/S/E/W to int
                            // Let's assume we need to convert String Direction to int if necessary
                            // But BluetoothMessage.ofRobotPositionMessage takes int direction.
                            // However, the previous implementation used tryGetIntParams for 3 params.
                            // Let's infer direction from string if it's not a number.
                            int dir = 0;
                             try {
                                dir = Integer.parseInt(params[3].trim());
                            } catch (NumberFormatException e) {
                                // parse N/S/E/W
                                String dStr = params[3].trim().toUpperCase();
                                dir = switch(dStr) {
                                    case "N", "NORTH" -> 0; // Mapping: 0=North, 1=East, 2=South, 3=West
                                    case "E", "EAST" -> 1;
                                    case "S", "SOUTH" -> 2;
                                    case "W", "WEST" -> 3;
                                    default -> 0;
                                };
                            }
                            ret = BluetoothMessage.ofRobotPositionMessage(msg, x, y, dir);
                        } catch (Exception e) {
                             ret = BluetoothMessage.ofPlainStringMessage(msg);
                        }
                    } else {
                         ret = BluetoothMessage.ofPlainStringMessage(msg);
                    }
                }
                case "TARGET", "IMAGE-REC" -> {
                    // TARGET,<ObstacleID>,<TargetID>
                    int[] intParams = tryGetIntParams(params, 2);
                    ret = BluetoothMessage.ofTargetFoundMessage(msg, intParams[0], intParams[1]);
                }
                default -> ret = BluetoothMessage.ofPlainStringMessage(msg);
            }
            ;
            return ret;
        }
        return BluetoothMessage.ofPlainStringMessage(msg);
    };

    public static int[] tryGetIntParams(String[] params, int expectedSize) {
        int[] ret = new int[expectedSize];
        // if input not valid just return empty list
        if (params.length <= expectedSize) {
            Log.e(TAG, String.format("Error in params, expected %d but size was %d", expectedSize, params.length));
            return ret;
        }
        // start from params index 1, since 0 is command
        for (int i = 0; i < expectedSize; ++i) {
            try {
                ret[i] = Integer.parseInt(params[i + 1].trim());
            } catch (NumberFormatException e) {
                ret[i] = 0; //put a safe non-crash value
                Log.e(TAG, "Error in parsing " + params[i + 1]);
            }
        }
        return ret;
    }

    public static BluetoothMessageParser ofDefault() {
        return DEFAULT;
    }
}
