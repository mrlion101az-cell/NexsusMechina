package com.nexusuniverse.mechanica.facility;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;
import java.util.Set;

/**
 * Builds one facility cell block-by-block using plain Bukkit World API
 * -- no schematic files, no external plugin dependency for the
 * geometry itself. Every cell is a hollow box (floor/walls/ceiling)
 * with doorway gaps toward whichever neighbor cells are also occupied,
 * then gets type-specific details layered on top.
 */
public class FacilityRoomBuilder {

    public static final int GRID_SIZE = 16;
    public static final int ROOM_HEIGHT = 6;
    private static final int DOOR_WIDTH = 3;
    private static final int DOOR_HEIGHT = 3;
    private static final double WALL_DECAY_CHANCE = 0.06;
    private static final double FLOOR_DECAY_CHANCE = 0.04;

    private final Random random = new Random();

    /**
     * Builds one cell. baseX/baseY/baseZ is the cell's minimum corner
     * (matches how FacilityGenerator computes paste-equivalent
     * coordinates). openSides is which cardinal directions have an
     * occupied neighbor and therefore need a doorway gap instead of a
     * solid wall.
     */
    public void build(World world, int baseX, int baseY, int baseZ,
                       FacilityPieceType type, Set<FacilityDirection> openSides) {
        buildShell(world, baseX, baseY, baseZ, openSides);

        switch (type) {
            case ENTRANCE -> buildEntranceDetails(world, baseX, baseY, baseZ);
            case ROOM -> buildRoomDetails(world, baseX, baseY, baseZ);
            case JUNCTION -> buildJunctionDetails(world, baseX, baseY, baseZ);
            case GENERATOR_ROOM -> buildGeneratorDetails(world, baseX, baseY, baseZ);
            case CORRIDOR -> buildCorridorDetails(world, baseX, baseY, baseZ);
        }
    }

    private void buildShell(World world, int baseX, int baseY, int baseZ, Set<FacilityDirection> openSides) {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                boolean edgeX = (x == 0 || x == GRID_SIZE - 1);
                boolean edgeZ = (z == 0 || z == GRID_SIZE - 1);

                // floor
                setBlock(world, baseX + x, baseY, baseZ + z,
                        FacilityMaterials.withDecayChance(FacilityMaterials.pick(FacilityMaterials.FLOOR), FLOOR_DECAY_CHANCE));

                // ceiling
                setBlock(world, baseX + x, baseY + ROOM_HEIGHT, baseZ + z,
                        FacilityMaterials.pick(FacilityMaterials.CEILING));

                if (edgeX || edgeZ) {
                    boolean isDoorway = isInDoorwayGap(x, z, openSides);
                    for (int y = 1; y < ROOM_HEIGHT; y++) {
                        if (isDoorway && y <= DOOR_HEIGHT) {
                            setBlock(world, baseX + x, baseY + y, baseZ + z, Material.AIR);
                        } else {
                            setBlock(world, baseX + x, baseY + y, baseZ + z,
                                    FacilityMaterials.withDecayChance(FacilityMaterials.pick(FacilityMaterials.WALL), WALL_DECAY_CHANCE));
                        }
                    }
                } else {
                    // clear the interior air space (in case terrain
                    // intersected -- ravines, hills, whatever was there before)
                    for (int y = 1; y < ROOM_HEIGHT; y++) {
                        setBlock(world, baseX + x, baseY + y, baseZ + z, Material.AIR);
                    }
                }
            }
        }
    }

    private boolean isInDoorwayGap(int x, int z, Set<FacilityDirection> openSides) {
        int center = GRID_SIZE / 2;
        int half = DOOR_WIDTH / 2;

        if (z == 0 && openSides.contains(FacilityDirection.NORTH) && Math.abs(x - center) <= half) return true;
        if (z == GRID_SIZE - 1 && openSides.contains(FacilityDirection.SOUTH) && Math.abs(x - center) <= half) return true;
        if (x == GRID_SIZE - 1 && openSides.contains(FacilityDirection.EAST) && Math.abs(z - center) <= half) return true;
        if (x == 0 && openSides.contains(FacilityDirection.WEST) && Math.abs(z - center) <= half) return true;
        return false;
    }

    private void buildEntranceDetails(World world, int baseX, int baseY, int baseZ) {
        int cx = baseX + GRID_SIZE / 2;
        int cz = baseZ + GRID_SIZE / 2;
        setBlock(world, cx, baseY + 1, cz, FacilityMaterials.pick(FacilityMaterials.LIGHT_SOURCES));
        setBlock(world, cx, baseY + 5, cz, FacilityMaterials.pick(FacilityMaterials.LIGHT_SOURCES));
    }

    private void buildCorridorDetails(World world, int baseX, int baseY, int baseZ) {
        // sparse hanging light every corridor, off-center so it doesn't
        // block the doorway sightline
        int lightX = baseX + 3 + random.nextInt(GRID_SIZE - 6);
        int lightZ = baseZ + 3 + random.nextInt(GRID_SIZE - 6);
        setBlock(world, lightX, baseY + ROOM_HEIGHT - 1, lightZ, FacilityMaterials.pick(FacilityMaterials.LIGHT_SOURCES));
    }

    private void buildRoomDetails(World world, int baseX, int baseY, int baseZ) {
        int propCount = 2 + random.nextInt(3);
        for (int i = 0; i < propCount; i++) {
            int x = baseX + 2 + random.nextInt(GRID_SIZE - 4);
            int z = baseZ + 2 + random.nextInt(GRID_SIZE - 4);
            setBlock(world, x, baseY + 1, z, FacilityMaterials.pick(FacilityMaterials.ROOM_PROPS));
        }
        setBlock(world, baseX + GRID_SIZE / 2, baseY + ROOM_HEIGHT - 1, baseZ + GRID_SIZE / 2,
                FacilityMaterials.pick(FacilityMaterials.LIGHT_SOURCES));
    }

    private void buildJunctionDetails(World world, int baseX, int baseY, int baseZ) {
        int cx = baseX + GRID_SIZE / 2;
        int cz = baseZ + GRID_SIZE / 2;
        // a support pillar in the middle, since junctions can open on
        // up to 4 sides and read as too empty without a centerpiece
        for (int y = 1; y < ROOM_HEIGHT; y++) {
            setBlock(world, cx, baseY + y, cz, Material.IRON_BLOCK);
        }
        setBlock(world, cx, baseY + ROOM_HEIGHT, cz, FacilityMaterials.pick(FacilityMaterials.LIGHT_SOURCES));
    }

    private void buildGeneratorDetails(World world, int baseX, int baseY, int baseZ) {
        int cx = baseX + GRID_SIZE / 2;
        int cz = baseZ + GRID_SIZE / 2;

        // the "broken machine" centerpiece -- a block cluster that
        // reads as dead machinery. Actual restoration logic (tying
        // this into the kinetic network system) isn't built yet, see
        // README; this is purely the visual for now.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setBlock(world, cx + dx, baseY + 1, cz + dz, Material.IRON_BLOCK);
            }
        }
        setBlock(world, cx, baseY + 2, cz, Material.OBSERVER);
        setBlock(world, cx, baseY + 3, cz, Material.REDSTONE_LAMP);

        // warning stripes on the floor around it
        for (int dx = -2; dx <= 2; dx++) {
            setBlock(world, cx + dx, baseY, cz - 3, Material.GOLD_BLOCK);
            setBlock(world, cx + dx, baseY, cz + 3, Material.GOLD_BLOCK);
        }
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) {
            block.setType(material, false);
        }
    }
}
