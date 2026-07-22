package com.nexusuniverse.mechanica.kinetic;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Owns the entire kinetic block graph. Every placed kinetic block belongs
 * to exactly one KineticNetwork. Placing a block next to existing kinetic
 * blocks merges networks together; breaking a block can split a network
 * into multiple disconnected pieces, so removal triggers a full
 * flood-fill rebuild of the affected area.
 *
 * V1 connectivity rule: any two kinetic blocks touching on any of the 6
 * faces are considered connected, regardless of type. Real Create has
 * axis-alignment rules (shafts only connect along their own axis,
 * cogwheels need perpendicular alignment, etc.) -- that's a natural V2
 * addition once the core loop is proven, not a blocker for now.
 */
public class KineticNetworkManager {

    private final JavaPlugin plugin;
    private final File dataFile;

    private final Map<String, PlacedKineticBlock> blocksByKey = new HashMap<>();
    private final Map<UUID, KineticNetwork> networksById = new HashMap<>();

    public KineticNetworkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "kinetic_blocks.yml");
    }

    public PlacedKineticBlock getByKey(String key) {
        return blocksByKey.get(key);
    }

    public PlacedKineticBlock getAt(Location location) {
        return blocksByKey.get(keyOf(location));
    }

    public Collection<KineticNetwork> networks() {
        return networksById.values();
    }

    public KineticNetwork networkOf(PlacedKineticBlock block) {
        return networksById.get(block.networkId());
    }

    private String keyOf(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    /**
     * Registers a newly placed kinetic block, merging it into any
     * adjacent network(s). If multiple distinct adjacent networks are
     * found, they are all merged into one.
     */
    public PlacedKineticBlock registerBlock(Location location, KineticBlockType type, KineticAxis axis) {
        PlacedKineticBlock block = new PlacedKineticBlock(location.clone(), type);
        block.setAxis(axis);
        blocksByKey.put(block.key(), block);

        Set<KineticNetwork> adjacentNetworks = new HashSet<>();
        for (PlacedKineticBlock neighbor : axisNeighborsOf(block)) {
            KineticNetwork network = networksById.get(neighbor.networkId());
            if (network != null) {
                adjacentNetworks.add(network);
            }
        }

        KineticNetwork target;
        if (adjacentNetworks.isEmpty()) {
            target = new KineticNetwork();
            networksById.put(target.id(), target);
        } else {
            Iterator<KineticNetwork> it = adjacentNetworks.iterator();
            target = it.next();
            while (it.hasNext()) {
                mergeInto(target, it.next());
            }
        }

        target.addMember(block);
        target.recalculate(this);
        return block;
    }

    private void mergeInto(KineticNetwork target, KineticNetwork other) {
        if (target.id().equals(other.id())) {
            return;
        }
        for (String key : new HashSet<>(other.memberKeys())) {
            PlacedKineticBlock member = blocksByKey.get(key);
            if (member != null) {
                other.removeMember(member);
                target.addMember(member);
            }
        }
        networksById.remove(other.id());
    }

    /**
     * Unregisters a broken kinetic block. Since removing one block can
     * split its network into multiple disconnected pieces, this rebuilds
     * the affected network from scratch via flood fill.
     */
    public void unregisterBlock(Location location) {
        String key = keyOf(location);
        PlacedKineticBlock removed = blocksByKey.remove(key);
        if (removed == null) {
            return;
        }

        KineticNetwork oldNetwork = networksById.get(removed.networkId());
        if (oldNetwork == null) {
            return;
        }
        oldNetwork.removeMember(removed);

        // Rebuild every remaining member's connectivity from scratch.
        List<String> remaining = new ArrayList<>(oldNetwork.memberKeys());
        networksById.remove(oldNetwork.id());

        Set<String> visitedGlobal = new HashSet<>();
        for (String memberKey : remaining) {
            if (visitedGlobal.contains(memberKey)) {
                continue;
            }
            PlacedKineticBlock start = blocksByKey.get(memberKey);
            if (start == null) {
                continue;
            }
            KineticNetwork rebuilt = new KineticNetwork();
            networksById.put(rebuilt.id(), rebuilt);
            floodFill(start, rebuilt, visitedGlobal);
            rebuilt.recalculate(this);
        }
    }

    private void floodFill(PlacedKineticBlock start, KineticNetwork network, Set<String> visitedGlobal) {
        Deque<PlacedKineticBlock> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            PlacedKineticBlock current = stack.pop();
            if (visitedGlobal.contains(current.key())) {
                continue;
            }
            visitedGlobal.add(current.key());
            network.addMember(current);
            for (PlacedKineticBlock neighbor : axisNeighborsOf(current)) {
                if (!visitedGlobal.contains(neighbor.key())) {
                    stack.push(neighbor);
                }
            }
        }
    }

    private List<PlacedKineticBlock> neighborsOf(Location location) {
        List<PlacedKineticBlock> result = new ArrayList<>(6);
        for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Location neighborLoc = location.clone().add(face.getModX(), face.getModY(), face.getModZ());
            PlacedKineticBlock neighbor = blocksByKey.get(keyOf(neighborLoc));
            if (neighbor != null) {
                result.add(neighbor);
            }
        }
        return result;
    }

    /**
     * Same as neighborsOf(), but applies the real connectivity rules
     * instead of "any touching block connects":
     *   - Two blocks with the SAME axis connect if the direction between
     *     them matches that axis (shaft-style transmission).
     *   - Two COGWHEEL blocks with DIFFERENT (perpendicular) axes connect
     *     if the direction between them is the third axis -- the one
     *     neither cogwheel uses -- matching how real meshed gears sit
     *     next to each other in the one plane they can physically touch.
     */
    private List<PlacedKineticBlock> axisNeighborsOf(PlacedKineticBlock block) {
        List<PlacedKineticBlock> result = new ArrayList<>();
        for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Location neighborLoc = block.location().clone().add(face.getModX(), face.getModY(), face.getModZ());
            PlacedKineticBlock neighbor = blocksByKey.get(keyOf(neighborLoc));
            if (neighbor == null) {
                continue;
            }

            if (block.axis() == neighbor.axis() && block.axis().matches(face)) {
                result.add(neighbor);
                continue;
            }

            boolean bothCogwheels = block.type() == KineticBlockType.COGWHEEL && neighbor.type() == KineticBlockType.COGWHEEL;
            if (bothCogwheels && block.axis() != neighbor.axis()) {
                KineticAxis meshAxis = thirdAxis(block.axis(), neighbor.axis());
                if (meshAxis != null && meshAxis.matches(face)) {
                    result.add(neighbor);
                }
            }
        }
        return result;
    }

    /** The one KineticAxis value that is neither a nor b -- the valid meshing direction for two perpendicular cogwheels. */
    private KineticAxis thirdAxis(KineticAxis a, KineticAxis b) {
        for (KineticAxis axis : KineticAxis.values()) {
            if (axis != a && axis != b) {
                return axis;
            }
        }
        return null;
    }

    /** Call after any active-state change (e.g. hand crank started/stopped) to refresh physics. */
    public void recalculateNetworkOf(PlacedKineticBlock block) {
        KineticNetwork network = networksById.get(block.networkId());
        if (network != null) {
            network.recalculate(this);
        }
    }

    public Collection<PlacedKineticBlock> allBlocks() {
        return blocksByKey.values();
    }

    public int totalBlockCount() {
        return blocksByKey.size();
    }

    public int totalNetworkCount() {
        return networksById.size();
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        int index = 0;
        for (PlacedKineticBlock block : blocksByKey.values()) {
            String path = "blocks." + index;
            Location loc = block.location();
            data.set(path + ".world", loc.getWorld().getName());
            data.set(path + ".x", loc.getBlockX());
            data.set(path + ".y", loc.getBlockY());
            data.set(path + ".z", loc.getBlockZ());
            data.set(path + ".type", block.type().name());
            data.set(path + ".axis", block.axis().name());
            index++;
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save kinetic_blocks.yml: " + e.getMessage());
        }
    }

    /**
     * Loads persisted blocks and rebuilds all networks via flood fill.
     * Call once at startup, after the world is available.
     */
    public void load() {
        blocksByKey.clear();
        networksById.clear();

        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (!data.contains("blocks")) {
            return;
        }

        List<PlacedKineticBlock> loaded = new ArrayList<>();
        for (String index : Objects.requireNonNull(data.getConfigurationSection("blocks")).getKeys(false)) {
            String path = "blocks." + index;
            String worldName = data.getString(path + ".world");
            org.bukkit.World world = worldName == null ? null : org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            int x = data.getInt(path + ".x");
            int y = data.getInt(path + ".y");
            int z = data.getInt(path + ".z");
            String typeName = data.getString(path + ".type");
            if (typeName == null) {
                continue;
            }
            KineticBlockType type;
            try {
                type = KineticBlockType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown kinetic block type in save data: " + typeName);
                continue;
            }
            Location loc = new Location(world, x, y, z);
            PlacedKineticBlock block = new PlacedKineticBlock(loc, type);

            String axisName = data.getString(path + ".axis");
            if (axisName != null) {
                try {
                    block.setAxis(KineticAxis.valueOf(axisName));
                } catch (IllegalArgumentException ignored) {
                    // keep default Y axis
                }
            }

            blocksByKey.put(block.key(), block);
            loaded.add(block);
        }

        Set<String> visitedGlobal = new HashSet<>();
        for (PlacedKineticBlock block : loaded) {
            if (visitedGlobal.contains(block.key())) {
                continue;
            }
            KineticNetwork network = new KineticNetwork();
            networksById.put(network.id(), network);
            floodFill(block, network, visitedGlobal);
            network.recalculate(this);
        }

        plugin.getLogger().info("[NexusMechanica] Loaded " + blocksByKey.size() + " kinetic block(s) across " + networksById.size() + " network(s).");
    }
}
