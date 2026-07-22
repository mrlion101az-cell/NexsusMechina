package com.nexusuniverse.mechanica.facility;

import org.bukkit.Location;

/**
 * One generated facility. `restored` flips to true once the
 * generator-room machine is repaired. Once restored, `integrity`
 * decays steadily over time (see FacilityMaintenanceTask) -- this
 * isn't a one-time fix, it's an ongoing responsibility. If integrity
 * hits 0, the facility drops back to unrestored and needs a full
 * re-restoration, not just a maintenance top-up.
 */
public class FacilitySite {

    public static final double MAX_INTEGRITY = 100.0;

    private final String id;
    private final Location origin;
    private final int pieceCount;
    private final Location generatorLocation;
    private boolean restored = false;
    private double integrity = 0.0;
    private boolean warnedThisCycle = false;

    public FacilitySite(String id, Location origin, int pieceCount, Location generatorLocation) {
        this.id = id;
        this.origin = origin;
        this.pieceCount = pieceCount;
        this.generatorLocation = generatorLocation;
    }

    public String getId() {
        return id;
    }

    public Location getOrigin() {
        return origin;
    }

    public int getPieceCount() {
        return pieceCount;
    }

    public Location getGeneratorLocation() {
        return generatorLocation;
    }

    public boolean isRestored() {
        return restored;
    }

    /** Marks the facility fully restored and running at full integrity. */
    public void markRestored() {
        this.restored = true;
        this.integrity = MAX_INTEGRITY;
        this.warnedThisCycle = false;
    }

    /** Marks the facility offline -- integrity hit 0, needs full re-restoration. */
    public void markOffline() {
        this.restored = false;
        this.integrity = 0.0;
    }

    public double getIntegrity() {
        return integrity;
    }

    public void decay(double amount) {
        integrity = Math.max(0.0, integrity - amount);
    }

    public void maintain(double amount) {
        integrity = Math.min(MAX_INTEGRITY, integrity + amount);
        if (integrity > FacilityMaintenanceTask.WARNING_THRESHOLD) {
            warnedThisCycle = false;
        }
    }

    public boolean hasWarnedThisCycle() {
        return warnedThisCycle;
    }

    public void setWarnedThisCycle(boolean warned) {
        this.warnedThisCycle = warned;
    }
}

