package com.nexusuniverse.mechanica.contraption;

import com.nexusuniverse.mechanica.kinetic.KineticAxis;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One assembled contraption: the anchor's original location (also the
 * rotation pivot for ROTATING mode), every captured block's offset +
 * original BlockData, and a parallel BlockDisplay entity per block that
 * actually moves in the world while the real blocks stay removed.
 */
public class Contraption {

    private final UUID id = UUID.randomUUID();
    private final ContraptionMode mode;
    private final Location anchorLocation;
    private final List<CapturedBlock> blocks;
    private final List<BlockDisplay> displays = new ArrayList<>();

    private KineticAxis rotationAxis = KineticAxis.Y;
    private BlockFace linearDirection = BlockFace.NORTH;
    private float rotationAngle = 0f;
    private double linearOffsetX = 0;
    private double linearOffsetY = 0;
    private double linearOffsetZ = 0;
    private boolean throttleOn = false;
    private Entity seat;
    /**
     * Where riders stand, relative to the anchor -- directly above the
     * tallest captured block, computed once at assembly. All current
     * riders share this one seat position (a known v1 simplification;
     * multiple distinct named seats would be a real next step).
     */
    private int riderSeatRelY = 1;
    private final List<UUID> riders = new ArrayList<>();

    public int riderSeatRelY() {
        return riderSeatRelY;
    }

    public void setRiderSeatRelY(int riderSeatRelY) {
        this.riderSeatRelY = riderSeatRelY;
    }

    public List<UUID> riders() {
        return riders;
    }

    public Contraption(ContraptionMode mode, Location anchorLocation, List<CapturedBlock> blocks) {
        this.mode = mode;
        this.anchorLocation = anchorLocation.clone();
        this.blocks = blocks;
    }

    public UUID id() {
        return id;
    }

    public ContraptionMode mode() {
        return mode;
    }

    public Location anchorLocation() {
        return anchorLocation;
    }

    public List<CapturedBlock> blocks() {
        return blocks;
    }

    public List<BlockDisplay> displays() {
        return displays;
    }

    public KineticAxis rotationAxis() {
        return rotationAxis;
    }

    public void setRotationAxis(KineticAxis rotationAxis) {
        this.rotationAxis = rotationAxis;
    }

    public BlockFace linearDirection() {
        return linearDirection;
    }

    public void setLinearDirection(BlockFace linearDirection) {
        this.linearDirection = linearDirection;
    }

    public float rotationAngle() {
        return rotationAngle;
    }

    public void setRotationAngle(float rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    public double[] linearOffset() {
        return new double[]{linearOffsetX, linearOffsetY, linearOffsetZ};
    }

    public void addLinearOffset(double dx, double dy, double dz) {
        this.linearOffsetX += dx;
        this.linearOffsetY += dy;
        this.linearOffsetZ += dz;
    }

    public boolean isThrottleOn() {
        return throttleOn;
    }

    public void setThrottleOn(boolean throttleOn) {
        this.throttleOn = throttleOn;
    }

    public Entity seat() {
        return seat;
    }

    public void setSeat(Entity seat) {
        this.seat = seat;
    }
}
