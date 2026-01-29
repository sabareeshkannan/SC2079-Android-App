package com.mdp26.mdp20;

/**
 * Simple enum for heading/direction/facing.
 */
public enum Facing {
    NORTH(1),
    EAST(2),
    SOUTH(3),
    WEST(4),
    SKIP(0);

    private int mappedCode;

    Facing(int code) {
        mappedCode = code;
    }

    /**
     * @return the mapped integer to represent the direction
     */
    public int getMappedCode() {
        return mappedCode;
    }

    public static Facing getFacingFromCode(int code) {
        return switch (code) {
            case 1 -> NORTH;
            case 2 -> EAST;
            case 3 -> SOUTH;
            case 4 -> WEST;
            default -> NORTH;
        };
    }
}
