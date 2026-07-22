package com.nexusuniverse.mechanica.kinetic;

import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;

import java.util.UUID;

/**
 * One kinetic block placed in the world. Tracks the real anchor block's
 * location, its logical type, which network it belongs to, and the
 * BlockDisplay entity giving it a rotating visual.
 */
public class PlacedKineticBlock {

    private final Location location;
    private final KineticBlockType type;
    private KineticAxis axis = KineticAxis.Y;
    private UUID networkId;
    /** Per-instance stress on top of the type's flat demand -- used by contraption bearings, whose real cost depends on the size of the attached structure, not a fixed type-level constant. */
    private int extraStressDemand = 0;
    private BlockDisplay displayEntity;
    /** Accumulated rotation angle in radians, persisted so a restart doesn't visually snap. */
    private float rotationAngle;
    /** Whether this specific block instance is actively driving power (e.g. hand crank being cranked). */
    private boolean active;

    public PlacedKineticBlock(Location location, KineticBlockType type) {
        this.location = location;
        this.type = type;
    }

    public Location location() {
        return location;
    }

    public KineticBlockType type() {
        return type;
    }

    public int extraStressDemand() {
        return extraStressDemand;
    }

    public void setExtraStressDemand(int extraStressDemand) {
        this.extraStressDemand = extraStressDemand;
    }

    public KineticAxis axis() {
        return axis;
    }

    public void setAxis(KineticAxis axis) {
        this.axis = axis;
    }

    public UUID networkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public BlockDisplay displayEntity() {
        return displayEntity;
    }

    public void setDisplayEntity(BlockDisplay displayEntity) {
        this.displayEntity = displayEntity;
    }

    public float rotationAngle() {
        return rotationAngle;
    }

    public void setRotationAngle(float rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Stable key for lookups -- one kinetic block per world block position. */
    public String key() {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
