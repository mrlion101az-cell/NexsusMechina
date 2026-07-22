package com.nexusuniverse.mechanica.display;

import com.nexusuniverse.mechanica.kinetic.KineticAxis;
import com.nexusuniverse.mechanica.kinetic.PlacedKineticBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Spawns and updates the BlockDisplay entity that gives each kinetic
 * block its rotating visual. The real anchor block stays a plain,
 * mostly-invisible block (see KineticBlockListener) -- the BlockDisplay
 * riding on top of it is what the player actually sees spinning.
 *
 * Rotation happens around each block's own KineticAxis (X, Y, or Z),
 * not a fixed axis -- an east-west shaft visually spins around the X
 * axis, a vertical one around Y, and so on, matching how it's actually
 * oriented in the world. This also matters for cogwheel meshing to look
 * right: two perpendicular meshed cogwheels need to visibly spin around
 * their own different axes, not all share one.
 */
public class RotationDisplayManager {

    private static final Vector3f CENTER_OFFSET = new Vector3f(-0.5f, -0.5f, -0.5f);
    private static final Vector3f UNIT_SCALE = new Vector3f(1f, 1f, 1f);
    private static final Vector3f SQUISHED_SCALE = new Vector3f(1.15f, 0.75f, 1.15f);

    private static final Vector3f X_AXIS_VEC = new Vector3f(1f, 0f, 0f);
    private static final Vector3f Y_AXIS_VEC = new Vector3f(0f, 1f, 0f);
    private static final Vector3f Z_AXIS_VEC = new Vector3f(0f, 0f, 1f);

    private Vector3f axisVector(KineticAxis axis) {
        return switch (axis) {
            case X -> X_AXIS_VEC;
            case Y -> Y_AXIS_VEC;
            case Z -> Z_AXIS_VEC;
        };
    }

    public BlockDisplay spawn(Location location, Material visualMaterial, PlacedKineticBlock block) {
        Location center = location.clone().add(0.5, 0.5, 0.5);
        BlockData blockData = visualMaterial.createBlockData();

        BlockDisplay display = location.getWorld().spawn(center, BlockDisplay.class, entity -> {
            entity.setBlock(blockData);
            entity.setPersistent(true);
            entity.setInterpolationDuration(2);
            entity.setTeleportDuration(0);
        });

        block.setDisplayEntity(display);
        applyRotation(display, block.rotationAngle(), block.axis());
        return display;
    }

    /**
     * Advances this block's stored rotation angle by (speed * deltaSeconds)
     * and applies the resulting transform to its BlockDisplay entity.
     * Speed is in the same arbitrary units as KineticBlockType.baseSpeed().
     */
    public void tick(PlacedKineticBlock block, int networkSpeed, float deltaSeconds) {
        BlockDisplay display = block.displayEntity();
        if (display == null || !display.isValid()) {
            return;
        }
        if (networkSpeed == 0) {
            return;
        }

        float radiansPerSecond = networkSpeed * 0.5f;
        float newAngle = block.rotationAngle() + (radiansPerSecond * deltaSeconds);
        newAngle %= (float) (Math.PI * 2);
        block.setRotationAngle(newAngle);

        applyRotation(display, newAngle, block.axis());
    }

    private void applyRotation(BlockDisplay display, float angleRadians, KineticAxis axis) {
        Quaternionf spin = new Quaternionf().rotateAxis(angleRadians, axisVector(axis));
        Transformation transform = new Transformation(
                CENTER_OFFSET,
                spin,
                UNIT_SCALE,
                new Quaternionf()
        );
        display.setTransformation(transform);
    }

    /**
     * Briefly squishes the block's display for one frame, as feedback
     * when a machine successfully processes an item. No separate restore
     * scheduling needed -- tick() runs every single game tick regardless
     * and always applies the correct full-scale rotation transform, so
     * the squish self-corrects on the very next tick and reads as a
     * quick pulse rather than a stuck deformation.
     */
    public void squish(PlacedKineticBlock block) {
        BlockDisplay display = block.displayEntity();
        if (display == null || !display.isValid()) {
            return;
        }
        Quaternionf currentSpin = new Quaternionf().rotateAxis(block.rotationAngle(), axisVector(block.axis()));
        Transformation squished = new Transformation(CENTER_OFFSET, currentSpin, SQUISHED_SCALE, new Quaternionf());
        display.setTransformation(squished);
    }

    public void remove(PlacedKineticBlock block) {
        BlockDisplay display = block.displayEntity();
        if (display != null && display.isValid()) {
            display.remove();
        }
        block.setDisplayEntity(null);
    }
}
