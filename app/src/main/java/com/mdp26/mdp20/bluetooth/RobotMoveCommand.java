package com.mdp26.mdp20.bluetooth;

public enum RobotMoveCommand {
    FORWARD("f"),
    BACKWARD("r"),
    ROTATE_LEFT("rl"),
    ROTATE_RIGHT("rr"),
    TURN_LEFT("tl"),
    TURN_RIGHT("tr");

    private final String value;

    RobotMoveCommand(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
