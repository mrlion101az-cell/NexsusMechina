package com.nexusuniverse.mechanica.contraption;

/**
 * How an assembled contraption moves.
 *
 * ROTATING: spins around a fixed pivot (the bearing block). Driven by
 * the same kinetic network system as everything else -- a Contraption
 * Bearing is a real KineticBlockType MACHINE, so a Ferris wheel is
 * powered by literally the same shafts and cranks as a mechanical press.
 *
 * LINEAR: translates in a straight line. Not kinetically powered --
 * player-ridden and throttle-toggled instead, since ships/vehicles in
 * the spirit of this feature aren't always tied to a power network.
 */
public enum ContraptionMode {
    ROTATING,
    LINEAR
}
