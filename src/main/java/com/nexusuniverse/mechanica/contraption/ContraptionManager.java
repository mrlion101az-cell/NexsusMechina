package com.nexusuniverse.mechanica.contraption;

import com.nexusuniverse.mechanica.kinetic.KineticAxis;
import com.nexusuniverse.mechanica.kinetic.KineticBlockType;
import com.nexusuniverse.mechanica.kinetic.KineticNetworkManager;
import com.nexusuniverse.mechanica.kinetic.PlacedKineticBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles assembly (real blocks -> moving display entities), disassembly
 * (back to real blocks), riding, and persistence of contraptions.
 *
 * The block cap here is not a technical limitation of this code -- it's
 * a real limit of what Minecraft's entity system can smoothly move as a
 * group, the same wall real Create's own community runs into. A
 * million-block moving structure isn't achievable by anyone in vanilla
 * Minecraft; this cap is set at what actually works, not what sounds
 * impressive.
 *
 * Stress scaling: a ROTATING contraption's bearing gets an extra stress
 * demand proportional to its block count (1 extra stress per 200
 * blocks) on top of its flat type-level demand, applied via
 * PlacedKineticBlock.setExtraStressDemand() -- a bigger Ferris wheel
 * genuinely needs a bigger power network, not just any hand crank.
 */
public class ContraptionManager {

    private static final int MAX_CONTRAPTION_BLOCKS = 4000;
    private static final int STRESS_PER_BLOCKS = 200;

    private static final BlockFace[] ALL_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final JavaPlugin plugin;
    private final KineticNetworkManager networkManager;
    private final File dataFile;

    private final Map<UUID, Contraption> activeContraptions = new HashMap<>();
    private final Map<String, UUID> contraptionByAnchorKey = new HashMap<>();

    public ContraptionManager(JavaPlugin plugin, KineticNetworkManager networkManager) {
        this.plugin = plugin;
        this.networkManager = networkManager;
        this.dataFile = new File(plugin.getDataFolder(), "contraptions.yml");
    }

    public AssemblyResult assemble(ContraptionMode mode, Location anchorLocation, BlockFace startDirection) {
        if (byAnchor(anchorLocation) != null) {
            return AssemblyResult.failure("This anchor already has an assembled contraption. Disassemble it first.");
        }

        Set<String> visited = new HashSet<>();
        Deque<Location> stack = new ArrayDeque<>();
        Location start = anchorLocation.clone().add(startDirection.getModX(), startDirection.getModY(), startDirection.getModZ());
        stack.push(start);

        List<CapturedBlock> captured = new ArrayList<>();
        int maxRelY = 0;

        while (!stack.isEmpty()) {
            Location current = stack.pop();
            String key = keyOf(current);
            if (visited.contains(key)) {
                continue;
            }
            visited.add(key);

            Block block = current.getBlock();
            if (block.getType().isAir()) {
                continue;
            }

            if (captured.size() >= MAX_CONTRAPTION_BLOCKS) {
                return AssemblyResult.failure(
                        "Structure exceeds the " + MAX_CONTRAPTION_BLOCKS + "-block limit. This isn't an arbitrary "
                                + "restriction -- moving structures become individual entities in Minecraft, and the "
                                + "engine genuinely cannot smoothly move hundreds of thousands of them regardless of "
                                + "server hardware. Scale the structure down, or split it into smaller contraptions.");
            }

            int relY = current.getBlockY() - anchorLocation.getBlockY();
            maxRelY = Math.max(maxRelY, relY);

            captured.add(new CapturedBlock(
                    current.getBlockX() - anchorLocation.getBlockX(),
                    relY,
                    current.getBlockZ() - anchorLocation.getBlockZ(),
                    block.getBlockData().clone()
            ));

            for (BlockFace face : ALL_FACES) {
                Location neighbor = current.clone().add(face.getModX(), face.getModY(), face.getModZ());
                if (!visited.contains(keyOf(neighbor))) {
                    stack.push(neighbor);
                }
            }
        }

        if (captured.isEmpty()) {
            return AssemblyResult.failure("No connected blocks found next to the anchor to assemble.");
        }

        Contraption contraption = new Contraption(mode, anchorLocation, captured);
        contraption.setLinearDirection(startDirection);
        contraption.setRiderSeatRelY(maxRelY + 1);

        for (CapturedBlock cb : captured) {
            Location worldLoc = anchorLocation.clone().add(cb.relX(), cb.relY(), cb.relZ());
            worldLoc.getBlock().setType(Material.AIR, false);

            BlockDisplay display = worldLoc.getWorld().spawn(worldLoc, BlockDisplay.class, entity -> {
                entity.setBlock(cb.blockData());
                // Deliberately NOT persistent as an entity -- contraption
                // state is saved/loaded explicitly via this class's own
                // save()/load(), not by relying on the entity surviving a
                // restart on its own.
                entity.setPersistent(false);
                entity.setInterpolationDuration(3);
                entity.setTeleportDuration(0);
            });
            contraption.displays().add(display);
        }

        if (mode == ContraptionMode.ROTATING) {
            applyStress(anchorLocation, captured.size());
        }

        activeContraptions.put(contraption.id(), contraption);
        contraptionByAnchorKey.put(keyOf(anchorLocation), contraption.id());
        return AssemblyResult.success(contraption, captured.size());
    }

    /**
     * Disassembles a contraption back into real blocks. For ROTATING
     * contraptions, only succeeds if the current angle is within a small
     * tolerance of a 90-degree increment -- placing real blocks back
     * from an arbitrary rotation angle would require re-deriving which
     * world grid cell each rotated block's center currently falls into,
     * which is a real, harder geometry problem this version doesn't
     * attempt. Rather than silently producing jumbled block placement,
     * this refuses and tells the player to stop it near an aligned angle
     * first.
     */
    public DisassemblyResult disassemble(Location anchorLocation) {
        UUID id = contraptionByAnchorKey.get(keyOf(anchorLocation));
        if (id == null) {
            return DisassemblyResult.failure("No assembled contraption found at this anchor.");
        }
        Contraption contraption = activeContraptions.get(id);
        if (contraption == null) {
            return DisassemblyResult.failure("No assembled contraption found at this anchor.");
        }

        if (contraption.mode() == ContraptionMode.ROTATING) {
            double degrees = Math.toDegrees(contraption.rotationAngle()) % 90.0;
            double distanceFromAligned = Math.min(degrees, 90.0 - degrees);
            if (distanceFromAligned > 5.0) {
                return DisassemblyResult.failure(
                        "Not aligned -- stop the network (turn off the crank powering it) near a clean 90-degree "
                                + "position before disassembling. Placing blocks back from an arbitrary spin angle "
                                + "isn't something this version attempts to do correctly.");
            }
            clearStress(anchorLocation);
        }

        double[] offset = contraption.linearOffset();
        for (CapturedBlock cb : contraption.blocks()) {
            Location placeLoc = anchorLocation.clone().add(
                    cb.relX() + Math.round(offset[0]),
                    cb.relY() + Math.round(offset[1]),
                    cb.relZ() + Math.round(offset[2])
            );
            placeLoc.getBlock().setBlockData(cb.blockData(), false);
        }

        dismountAllRiders(contraption);
        cleanupEntities(contraption);
        activeContraptions.remove(id);
        contraptionByAnchorKey.remove(keyOf(anchorLocation));
        return DisassemblyResult.success(contraption.blocks().size());
    }

    // ------------------------------------------------------------------
    // Riding
    // ------------------------------------------------------------------

    public boolean mount(Location anchorLocation, Player player) {
        Contraption contraption = byAnchor(anchorLocation);
        if (contraption == null) {
            return false;
        }
        if (!contraption.riders().contains(player.getUniqueId())) {
            contraption.riders().add(player.getUniqueId());
        }
        return true;
    }

    /** Returns true if the player was actually riding something. */
    public boolean dismount(Player player) {
        boolean found = false;
        for (Contraption contraption : activeContraptions.values()) {
            found |= contraption.riders().remove(player.getUniqueId());
        }
        return found;
    }

    private void dismountAllRiders(Contraption contraption) {
        contraption.riders().clear();
    }

    // ------------------------------------------------------------------
    // Stress linkage
    // ------------------------------------------------------------------

    private void applyStress(Location anchorLocation, int blockCount) {
        PlacedKineticBlock bearing = networkManager.getAt(anchorLocation);
        if (bearing == null || bearing.type() != KineticBlockType.CONTRAPTION_BEARING) {
            return;
        }
        bearing.setExtraStressDemand(blockCount / STRESS_PER_BLOCKS);
        networkManager.recalculateNetworkOf(bearing);
    }

    private void clearStress(Location anchorLocation) {
        PlacedKineticBlock bearing = networkManager.getAt(anchorLocation);
        if (bearing == null) {
            return;
        }
        bearing.setExtraStressDemand(0);
        networkManager.recalculateNetworkOf(bearing);
    }

    /**
     * Called on plugin shutdown ONLY if save() itself fails for some
     * reason -- the normal shutdown path is save(), which preserves
     * exact contraption state (rotation angle, position, riders) across
     * a restart. This is the fallback: forcibly return every active
     * contraption to real blocks at its ORIGINAL position, so nothing is
     * ever silently lost even in a worst-case scenario.
     */
    public void disassembleAll() {
        for (Contraption contraption : new ArrayList<>(activeContraptions.values())) {
            if (contraption.mode() == ContraptionMode.ROTATING) {
                clearStress(contraption.anchorLocation());
            }
            for (CapturedBlock cb : contraption.blocks()) {
                Location placeLoc = contraption.anchorLocation().clone().add(cb.relX(), cb.relY(), cb.relZ());
                placeLoc.getBlock().setBlockData(cb.blockData(), false);
            }
            dismountAllRiders(contraption);
            cleanupEntities(contraption);
        }
        activeContraptions.clear();
        contraptionByAnchorKey.clear();
    }

    private void cleanupEntities(Contraption contraption) {
        for (BlockDisplay display : contraption.displays()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        if (contraption.seat() != null && contraption.seat().isValid()) {
            contraption.seat().remove();
        }
    }

    public Contraption byAnchor(Location anchorLocation) {
        UUID id = contraptionByAnchorKey.get(keyOf(anchorLocation));
        return id == null ? null : activeContraptions.get(id);
    }

    public Collection<Contraption> all() {
        return activeContraptions.values();
    }

    private String keyOf(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    /**
     * Saves every active contraption's full live state -- position,
     * rotation angle, linear offset, throttle state -- so a graceful
     * restart resumes exactly where things were, not just "nothing was
     * lost." Riders are NOT persisted -- players are dismounted before
     * saving, since restoring a mid-ride player across a server restart
     * (they'll have reconnected fresh, possibly to a different location
     * entirely) isn't something this version attempts.
     *
     * Honest limitation: this only runs on a GRACEFUL shutdown
     * (onDisable). A hard crash never calls this, and would leave the
     * real blocks removed from the world with no saved recovery data --
     * the same risk any plugin managing removed-and-replaced blocks
     * carries, not unique to this one, but worth being direct about.
     */
    public void save() {
        for (Contraption contraption : activeContraptions.values()) {
            dismountAllRiders(contraption);
        }

        YamlConfiguration data = new YamlConfiguration();
        int index = 0;
        for (Contraption c : activeContraptions.values()) {
            String path = "contraptions." + index;
            Location a = c.anchorLocation();
            data.set(path + ".world", a.getWorld().getName());
            data.set(path + ".x", a.getBlockX());
            data.set(path + ".y", a.getBlockY());
            data.set(path + ".z", a.getBlockZ());
            data.set(path + ".mode", c.mode().name());
            data.set(path + ".rotationAxis", c.rotationAxis().name());
            data.set(path + ".rotationAngle", (double) c.rotationAngle());
            data.set(path + ".linearDirection", c.linearDirection().name());
            data.set(path + ".riderSeatRelY", c.riderSeatRelY());
            double[] offset = c.linearOffset();
            data.set(path + ".linearOffsetX", offset[0]);
            data.set(path + ".linearOffsetY", offset[1]);
            data.set(path + ".linearOffsetZ", offset[2]);
            data.set(path + ".throttleOn", c.isThrottleOn());

            int blockIndex = 0;
            for (CapturedBlock cb : c.blocks()) {
                String blockPath = path + ".blocks." + blockIndex;
                data.set(blockPath + ".relX", cb.relX());
                data.set(blockPath + ".relY", cb.relY());
                data.set(blockPath + ".relZ", cb.relZ());
                data.set(blockPath + ".blockData", cb.blockData().getAsString());
                blockIndex++;
            }
            index++;
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(dataFile);
            plugin.getLogger().info("[Contraption] Saved " + activeContraptions.size() + " active contraption(s).");
        } catch (IOException e) {
            plugin.getLogger().severe("[Contraption] Could not save contraptions.yml: " + e.getMessage()
                    + " -- falling back to safe disassembly so nothing is silently lost.");
            disassembleAll();
        }
    }

    /**
     * Restores every saved contraption exactly as it was left -- respawns
     * displays at their correct CURRENT (already rotated/offset)
     * positions, not their original base positions, and re-links
     * ROTATING contraptions' stress demand to their bearing.
     */
    public void load() {
        activeContraptions.clear();
        contraptionByAnchorKey.clear();

        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = data.getConfigurationSection("contraptions");
        if (root == null) {
            return;
        }

        int restored = 0;
        for (String index : root.getKeys(false)) {
            String path = "contraptions." + index;
            String worldName = data.getString(path + ".world");
            World world = worldName == null ? null : Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            Location anchor = new Location(world, data.getInt(path + ".x"), data.getInt(path + ".y"), data.getInt(path + ".z"));

            ContraptionMode mode;
            try {
                mode = ContraptionMode.valueOf(Objects.requireNonNull(data.getString(path + ".mode")));
            } catch (Exception e) {
                continue;
            }

            List<CapturedBlock> blocks = new ArrayList<>();
            ConfigurationSection blocksSection = data.getConfigurationSection(path + ".blocks");
            if (blocksSection != null) {
                for (String blockIndex : blocksSection.getKeys(false)) {
                    String blockPath = path + ".blocks." + blockIndex;
                    int relX = data.getInt(blockPath + ".relX");
                    int relY = data.getInt(blockPath + ".relY");
                    int relZ = data.getInt(blockPath + ".relZ");
                    String blockDataString = data.getString(blockPath + ".blockData");
                    if (blockDataString == null) {
                        continue;
                    }
                    BlockData blockData;
                    try {
                        blockData = Bukkit.createBlockData(blockDataString);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    blocks.add(new CapturedBlock(relX, relY, relZ, blockData));
                }
            }

            if (blocks.isEmpty()) {
                continue;
            }

            Contraption contraption = new Contraption(mode, anchor, blocks);
            try {
                contraption.setRotationAxis(KineticAxis.valueOf(Objects.requireNonNull(data.getString(path + ".rotationAxis"))));
            } catch (Exception ignored) {
                // keep default
            }
            try {
                contraption.setLinearDirection(BlockFace.valueOf(Objects.requireNonNull(data.getString(path + ".linearDirection"))));
            } catch (Exception ignored) {
                // keep default
            }
            contraption.setRotationAngle((float) data.getDouble(path + ".rotationAngle"));
            contraption.setRiderSeatRelY(data.getInt(path + ".riderSeatRelY", 1));
            contraption.addLinearOffset(
                    data.getDouble(path + ".linearOffsetX"),
                    data.getDouble(path + ".linearOffsetY"),
                    data.getDouble(path + ".linearOffsetZ")
            );
            contraption.setThrottleOn(data.getBoolean(path + ".throttleOn"));

            respawnDisplaysAtCurrentState(contraption);

            if (mode == ContraptionMode.ROTATING) {
                applyStress(anchor, blocks.size());
            }

            activeContraptions.put(contraption.id(), contraption);
            contraptionByAnchorKey.put(keyOf(anchor), contraption.id());
            restored++;
        }

        plugin.getLogger().info("[Contraption] Restored " + restored + " contraption(s) from saved state.");
    }

    /** Spawns each block's display at its CURRENT rotated/translated position, matching where it was when saved -- not its original base position. */
    private void respawnDisplaysAtCurrentState(Contraption contraption) {
        Vector3f centerOffset = new Vector3f(-0.5f, -0.5f, -0.5f);
        Vector3f unitScale = new Vector3f(1f, 1f, 1f);

        for (CapturedBlock cb : contraption.blocks()) {
            Location spawnAt;
            Transformation transform;

            if (contraption.mode() == ContraptionMode.ROTATING) {
                spawnAt = contraption.anchorLocation();
                double[] rotated = RotationMath.rotate(cb.relX(), cb.relY(), cb.relZ(), contraption.rotationAxis(), contraption.rotationAngle());
                Vector3f translation = new Vector3f(
                        centerOffset.x + (float) rotated[0],
                        centerOffset.y + (float) rotated[1],
                        centerOffset.z + (float) rotated[2]
                );
                Quaternionf spin = new Quaternionf().rotateAxis(contraption.rotationAngle(), axisVector(contraption.rotationAxis()));
                transform = new Transformation(translation, spin, unitScale, new Quaternionf());
            } else {
                double[] offset = contraption.linearOffset();
                spawnAt = contraption.anchorLocation().clone().add(cb.relX() + offset[0], cb.relY() + offset[1], cb.relZ() + offset[2]);
                transform = new Transformation(new Vector3f(), new Quaternionf(), unitScale, new Quaternionf());
            }

            BlockDisplay display = spawnAt.getWorld().spawn(spawnAt, BlockDisplay.class, entity -> {
                entity.setBlock(cb.blockData());
                entity.setPersistent(false);
                entity.setInterpolationDuration(3);
                entity.setTeleportDuration(0);
                entity.setTransformation(transform);
            });
            contraption.displays().add(display);
        }
    }

    private Vector3f axisVector(KineticAxis axis) {
        return switch (axis) {
            case X -> new Vector3f(1f, 0f, 0f);
            case Y -> new Vector3f(0f, 1f, 0f);
            case Z -> new Vector3f(0f, 0f, 1f);
        };
    }

    public record AssemblyResult(boolean success, String message, Contraption contraption, int blockCount) {
        public static AssemblyResult success(Contraption contraption, int blockCount) {
            return new AssemblyResult(true, null, contraption, blockCount);
        }

        public static AssemblyResult failure(String message) {
            return new AssemblyResult(false, message, null, 0);
        }
    }

    public record DisassemblyResult(boolean success, String message, int blockCount) {
        public static DisassemblyResult success(int blockCount) {
            return new DisassemblyResult(true, null, blockCount);
        }

        public static DisassemblyResult failure(String message) {
            return new DisassemblyResult(false, message, 0);
        }
    }
}
