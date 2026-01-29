package com.mdp26.mdp20.canvas;

import com.mdp26.mdp20.Facing;
import com.mdp26.mdp20.Position;

public class Robot {
    private Facing facing;
    private final Position position;
    private final double turningRadius = 4.5;

    public Robot(int x, int y, Facing facing) {
        this.facing = facing;
        this.position = Position.of(x, y);
    }

    public static Robot ofDefault() {
        return new Robot(1, 1, Facing.NORTH);
    }

    /**
     * Static factory method with default facing of {@link Facing#NORTH}.
     */
    public static Robot of(int x, int y) {
        return new Robot(x, y, Facing.NORTH);
    }

    public static Robot of(int x, int y, Facing facing) {
        return new Robot(x, y, facing);
    }

    public Robot updatePosition(int x, int y) {
        position.setX(x);
        position.setY(y);
        return this;
    }

    public Position getPosition() {
        return this.position;
    }

    public Robot updateFacing(Facing facing) {
        if (!facing.equals(Facing.SKIP))
            this.facing = facing;
        return this;
    }

    public Facing getFacing() {
        return this.facing;
    }

    // Helper to check bounds
    private boolean isWithinBounds(double x, double y) {
        return x >= 1 && x < Grid.GRID_SIZE - 1 && y >= 1 && y < Grid.GRID_SIZE - 1;
    }

    // Robot moves forward by one cell in the direction it is facing
    public void moveForward() {
        int newX = this.position.getXInt();
        int newY = this.position.getYInt();
        if (this.facing == Facing.NORTH) {
            newY += 1;
        } else if (this.facing == Facing.EAST) {
            newX += 1;
        } else if (this.facing == Facing.SOUTH) {
            newY -= 1;
        } else if (this.facing == Facing.WEST) {
            newX -= 1;
        }
        if (isWithinBounds(newX, newY)) {
            this.position.setX(newX);
            this.position.setY(newY);
        }
    }

    // Robot moves backward by one cell in the direction it is facing
    public void moveBackward() {
        int newX = this.position.getXInt();
        int newY = this.position.getYInt();
        if (this.facing == Facing.NORTH) {
            newY -= 1;
        } else if (this.facing == Facing.EAST) {
            newX -= 1;
        } else if (this.facing == Facing.SOUTH) {
            newY += 1;
        } else if (this.facing == Facing.WEST) {
            newX += 1;
        }
        if (isWithinBounds(newX, newY)) {
            this.position.setX(newX);
            this.position.setY(newY);
        }
    }

    // Robot turns right by moving forward by one cell and the turning radius
    public void turnRight() {
        double newX = this.position.getXInt();
        double newY = this.position.getYInt();
        Facing newFacing = this.facing;

        if (this.facing == Facing.NORTH) {
            newY += 1;
            newX += turningRadius;
            newFacing = Facing.EAST;
        } else if (this.facing == Facing.EAST) {
            newX += 1;
            newY -= turningRadius;
            newFacing = Facing.SOUTH;
        } else if (this.facing == Facing.SOUTH) {
            newY -= 1;
            newX -= turningRadius;
            newFacing = Facing.WEST;
        } else if (this.facing == Facing.WEST) {
            newX -= 1;
            newY += turningRadius;
            newFacing = Facing.NORTH;
        }

        if (isWithinBounds(newX, newY)) {
            this.position.setX(newX);
            this.position.setY(newY);
            this.facing = newFacing;
        }
    }

    // Robot turns left by moving forward by one cell and the turning radius
    public void turnLeft() {
        double newX = this.position.getXInt();
        double newY = this.position.getYInt();
        Facing newFacing = this.facing;

        if (this.facing == Facing.NORTH) {
            newY += 1;
            newX -= turningRadius;
            newFacing = Facing.WEST;
        } else if (this.facing == Facing.WEST) {
            newX -= 1;
            newY -= turningRadius;
            newFacing = Facing.SOUTH;
        } else if (this.facing == Facing.SOUTH) {
            newY -= 1;
            newX += turningRadius;
            newFacing = Facing.EAST;
        } else if (this.facing == Facing.EAST) {
            newX += 1;
            newY += turningRadius;
            newFacing = Facing.NORTH;
        }
        
        if (isWithinBounds(newX, newY)) {
            this.position.setX(newX);
            this.position.setY(newY);
            this.facing = newFacing;
        }
    }

    // Robot rotates right in place (Spot Turn)
    public void rotateRight() {
        if (this.facing == Facing.NORTH) {
            this.facing = Facing.EAST;
        } else if (this.facing == Facing.EAST) {
            this.facing = Facing.SOUTH;
        } else if (this.facing == Facing.SOUTH) {
            this.facing = Facing.WEST;
        } else if (this.facing == Facing.WEST) {
            this.facing = Facing.NORTH;
        }
    }

    // Robot rotates left in place (Spot Turn)
    public void rotateLeft() {
        if (this.facing == Facing.NORTH) {
            this.facing = Facing.WEST;
        } else if (this.facing == Facing.WEST) {
            this.facing = Facing.SOUTH;
        } else if (this.facing == Facing.SOUTH) {
            this.facing = Facing.EAST;
        } else if (this.facing == Facing.EAST) {
            this.facing = Facing.NORTH;
        }
    }

    // Reverse logic (To be implemented) - Not used but good to safeguard
    public void reverseLeft() { return; }
    public void reverseRight() { return; }
}
