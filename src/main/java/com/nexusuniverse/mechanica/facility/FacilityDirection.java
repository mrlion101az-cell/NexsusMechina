package com.nexusuniverse.mechanica.facility;

/**
 * Which side of a grid cell -- used to know which walls of a
 * procedurally-built room need a doorway opening (toward an occupied
 * neighbor cell) versus staying solid (toward nothing/unbuilt space).
 */
public enum FacilityDirection {
    NORTH(0, -1),
    SOUTH(0, 1),
    EAST(1, 0),
    WEST(-1, 0);

    public final int dx;
    public final int dz;

    FacilityDirection(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }
}
