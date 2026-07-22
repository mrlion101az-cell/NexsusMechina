package com.nexusuniverse.mechanica.facility;

/**
 * What role a schematic piece plays in an assembled facility. The
 * generator uses these to make sure every facility has exactly one
 * entrance and exactly one generator room, with corridors/rooms/
 * junctions filling out the rest.
 */
public enum FacilityPieceType {
    ENTRANCE,
    CORRIDOR,
    ROOM,
    JUNCTION,
    GENERATOR_ROOM
}
