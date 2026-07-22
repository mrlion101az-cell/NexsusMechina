package com.nexusuniverse.mechanica.kinetic;

import org.bukkit.block.BlockFace;

/**
 * The axis a kinetic block is oriented along. Real Create connectivity
 * rules are more nuanced (cogwheels mesh perpendicular, not just
 * axis-matched) -- this is a deliberate V1 simplification: two adjacent
 * kinetic blocks connect only if the direction between them matches
 * BOTH blocks' own axis. A shaft placed running north-south will not
 * connect to one running east-west even if they're touching, which is
 * the real behavior this was missing before.
 */
public enum KineticAxis {
    X, Y, Z;

    public static KineticAxis fromPlayerFacing(BlockFace facing) {
        return switch (facing) {
            case UP, DOWN -> Y;
            case EAST, WEST -> X;
            default -> Z; // NORTH, SOUTH, and any other reported face default to Z
        };
    }

    /** Whether a step in this direction is consistent with this axis. */
    public boolean matches(BlockFace direction) {
        return switch (this) {
            case X -> direction == BlockFace.EAST || direction == BlockFace.WEST;
            case Y -> direction == BlockFace.UP || direction == BlockFace.DOWN;
            case Z -> direction == BlockFace.NORTH || direction == BlockFace.SOUTH;
        };
    }
}
