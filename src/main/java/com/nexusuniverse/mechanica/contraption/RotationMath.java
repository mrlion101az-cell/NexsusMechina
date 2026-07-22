package com.nexusuniverse.mechanica.contraption;

import com.nexusuniverse.mechanica.kinetic.KineticAxis;

/**
 * Standard rigid-body rotation math: given a block's original offset
 * from the pivot, and a current angle, returns where that offset
 * SHOULD be right now. This has to be computed explicitly rather than
 * relying on the Display entity's own rotation transform to do it --
 * a Display's rotation quaternion only rotates the block's own
 * appearance in place, it does not also rotate a translation vector for
 * you. Getting this distinction right is the trickiest, most
 * worth-verifying-live part of the whole contraption system.
 */
public class RotationMath {

    public static double[] rotate(int relX, int relY, int relZ, KineticAxis axis, float angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        return switch (axis) {
            case Y -> new double[]{
                    relX * cos + relZ * sin,
                    relY,
                    -relX * sin + relZ * cos
            };
            case X -> new double[]{
                    relX,
                    relY * cos - relZ * sin,
                    relY * sin + relZ * cos
            };
            case Z -> new double[]{
                    relX * cos - relY * sin,
                    relX * sin + relY * cos,
                    relZ
            };
        };
    }
}
