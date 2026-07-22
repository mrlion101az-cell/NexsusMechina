package com.nexusuniverse.mechanica.contraption;

import com.nexusuniverse.mechanica.kinetic.KineticAxis;
import com.nexusuniverse.mechanica.kinetic.KineticNetwork;
import com.nexusuniverse.mechanica.kinetic.KineticNetworkManager;
import com.nexusuniverse.mechanica.kinetic.PlacedKineticBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * Drives every active contraption's actual movement, once per tick.
 *
 * ROTATING contraptions read their bearing's live kinetic network speed
 * -- the same KineticNetwork stress/speed system every other machine
 * uses -- and advance rotation accordingly. If the network is stalled
 * (not enough stress capacity) or has no active source, the contraption
 * simply doesn't move, exactly like a mechanical press would sit idle.
 *
 * LINEAR contraptions ignore the kinetic system entirely and move at a
 * constant velocity while their throttle is toggled on, teleporting each
 * display to its new absolute world position every tick.
 */
public class ContraptionTickTask extends BukkitRunnable {

    private static final float DELTA_SECONDS = 1f / 20f;
    /** Constant speed for LINEAR contraptions while throttled on. Not tied to any power system -- see class docs. */
    private static final double LINEAR_BLOCKS_PER_SECOND = 2.0;

    private static final Vector3f CENTER_OFFSET = new Vector3f(-0.5f, -0.5f, -0.5f);
    private static final Vector3f UNIT_SCALE = new Vector3f(1f, 1f, 1f);

    private final ContraptionManager contraptionManager;
    private final KineticNetworkManager networkManager;

    public ContraptionTickTask(ContraptionManager contraptionManager, KineticNetworkManager networkManager) {
        this.contraptionManager = contraptionManager;
        this.networkManager = networkManager;
    }

    @Override
    public void run() {
        for (Contraption contraption : contraptionManager.all()) {
            if (contraption.mode() == ContraptionMode.ROTATING) {
                tickRotating(contraption);
            } else {
                tickLinear(contraption);
            }
        }
    }

    private void tickRotating(Contraption contraption) {
        PlacedKineticBlock bearing = networkManager.getAt(contraption.anchorLocation());
        if (bearing == null) {
            return;
        }
        KineticNetwork network = networkManager.networkOf(bearing);
        if (network == null || network.isStalled() || network.currentSpeed() == 0) {
            return;
        }

        float radiansPerSecond = network.currentSpeed() * 0.5f;
        float newAngle = contraption.rotationAngle() + (radiansPerSecond * DELTA_SECONDS);
        newAngle %= (float) (Math.PI * 2);
        contraption.setRotationAngle(newAngle);

        List<CapturedBlock> blocks = contraption.blocks();
        List<BlockDisplay> displays = contraption.displays();

        for (int i = 0; i < blocks.size(); i++) {
            CapturedBlock cb = blocks.get(i);
            BlockDisplay display = displays.get(i);
            if (!display.isValid()) {
                continue;
            }

            double[] rotated = RotationMath.rotate(cb.relX(), cb.relY(), cb.relZ(), contraption.rotationAxis(), newAngle);
            Vector3f translation = new Vector3f(
                    CENTER_OFFSET.x + (float) rotated[0],
                    CENTER_OFFSET.y + (float) rotated[1],
                    CENTER_OFFSET.z + (float) rotated[2]
            );
            Quaternionf spin = new Quaternionf().rotateAxis(newAngle, axisVector(contraption.rotationAxis()));
            display.setTransformation(new Transformation(translation, spin, UNIT_SCALE, new Quaternionf()));
        }

        teleportRiders(contraption, rotatedSeatLocation(contraption, newAngle));
    }

    private Location rotatedSeatLocation(Contraption contraption, float angle) {
        double[] rotated = RotationMath.rotate(0, contraption.riderSeatRelY(), 0, contraption.rotationAxis(), angle);
        return contraption.anchorLocation().clone().add(rotated[0], rotated[1], rotated[2]);
    }

    private void tickLinear(Contraption contraption) {
        if (!contraption.isThrottleOn()) {
            return;
        }

        BlockFace direction = contraption.linearDirection();
        double distanceThisTick = LINEAR_BLOCKS_PER_SECOND * DELTA_SECONDS;
        contraption.addLinearOffset(
                direction.getModX() * distanceThisTick,
                direction.getModY() * distanceThisTick,
                direction.getModZ() * distanceThisTick
        );

        double[] offset = contraption.linearOffset();
        List<CapturedBlock> blocks = contraption.blocks();
        List<BlockDisplay> displays = contraption.displays();

        for (int i = 0; i < blocks.size(); i++) {
            CapturedBlock cb = blocks.get(i);
            BlockDisplay display = displays.get(i);
            if (!display.isValid()) {
                continue;
            }

            Location newLoc = contraption.anchorLocation().clone().add(
                    cb.relX() + offset[0],
                    cb.relY() + offset[1],
                    cb.relZ() + offset[2]
            );
            display.teleport(newLoc);
        }

        Location seatLoc = contraption.anchorLocation().clone().add(
                offset[0], contraption.riderSeatRelY() + offset[1], offset[2]
        );
        teleportRiders(contraption, seatLoc);
    }

    /**
     * Riding is implemented as repeated per-tick teleportation to a
     * computed seat position, NOT Bukkit's passenger-mount system --
     * passenger seat offsets are fixed small local positions, not full
     * arbitrary 3D transforms, so they can't represent a large-radius
     * orbit around a rotating pivot the way this needs. Teleporting is
     * less elegant but is a real, correct technique for exactly this
     * situation, and keeps ROTATE and LINEAR mode riding consistent with
     * each other and with how blocks themselves are already positioned.
     */
    private void teleportRiders(Contraption contraption, Location seatLocation) {
        for (UUID riderId : contraption.riders()) {
            Player player = Bukkit.getPlayer(riderId);
            if (player != null && player.isOnline()) {
                player.teleport(seatLocation.clone().add(0.5, 0, 0.5));
            }
        }
    }

    private Vector3f axisVector(KineticAxis axis) {
        return switch (axis) {
            case X -> new Vector3f(1f, 0f, 0f);
            case Y -> new Vector3f(0f, 1f, 0f);
            case Z -> new Vector3f(0f, 0f, 1f);
        };
    }
}
