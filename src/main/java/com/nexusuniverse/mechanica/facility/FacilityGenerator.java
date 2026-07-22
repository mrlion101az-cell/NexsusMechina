package com.nexusuniverse.mechanica.facility;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Random-walk facility assembly on a fixed grid, same layout logic as
 * before, but now every cell is built directly with plain block calls
 * (FacilityRoomBuilder) instead of pasting a pre-made schematic. No
 * external plugin dependency for the geometry -- every facility is
 * generated fresh from code, so there's nothing to hand-build first.
 */
public class FacilityGenerator {

    private static final int GRID_SIZE = FacilityRoomBuilder.GRID_SIZE;
    private static final int MIN_PIECES = 6;
    private static final int MAX_PIECES = 12;

    private final Random random = new Random();
    private final FacilityRoomBuilder roomBuilder = new FacilityRoomBuilder();

    public FacilitySite generate(Location origin, String siteId) {
        int targetPieceCount = MIN_PIECES + random.nextInt(MAX_PIECES - MIN_PIECES + 1);

        Set<GridCell> occupied = new HashSet<>();
        Map<GridCell, FacilityPieceType> placements = new HashMap<>();
        List<GridCell> order = new ArrayList<>();

        GridCell entranceCell = new GridCell(0, 0);
        occupied.add(entranceCell);
        placements.put(entranceCell, FacilityPieceType.ENTRANCE);
        order.add(entranceCell);

        GridCell current = entranceCell;
        GridCell farthestFromEntrance = entranceCell;

        for (int i = 1; i < targetPieceCount; i++) {
            GridCell next = randomUnoccupiedNeighbor(current, occupied);
            if (next == null) {
                current = order.get(random.nextInt(order.size()));
                next = randomUnoccupiedNeighbor(current, occupied);
                if (next == null) break;
            }

            FacilityPieceType type = pickFillerType();
            occupied.add(next);
            placements.put(next, type);
            order.add(next);

            if (next.distanceFrom(entranceCell) > farthestFromEntrance.distanceFrom(entranceCell)) {
                farthestFromEntrance = next;
            }

            current = next;
        }

        // generator room always goes at the farthest point reached
        placements.put(farthestFromEntrance, FacilityPieceType.GENERATOR_ROOM);

        buildAll(origin, placements, occupied);

        Location generatorLocation = cellToWorldCenter(origin, farthestFromEntrance);
        return new FacilitySite(siteId, origin, placements.size(), generatorLocation);
    }

    private FacilityPieceType pickFillerType() {
        double roll = random.nextDouble();
        if (roll < 0.5) return FacilityPieceType.CORRIDOR;
        if (roll < 0.85) return FacilityPieceType.ROOM;
        return FacilityPieceType.JUNCTION;
    }

    private GridCell randomUnoccupiedNeighbor(GridCell cell, Set<GridCell> occupied) {
        List<GridCell> candidates = new ArrayList<>();
        for (GridCell neighbor : cell.neighbors()) {
            if (!occupied.contains(neighbor)) {
                candidates.add(neighbor);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private void buildAll(Location origin, Map<GridCell, FacilityPieceType> placements, Set<GridCell> occupied) {
        var world = origin.getWorld();
        for (Map.Entry<GridCell, FacilityPieceType> entry : placements.entrySet()) {
            GridCell cell = entry.getKey();
            FacilityPieceType type = entry.getValue();

            Set<FacilityDirection> openSides = new HashSet<>();
            for (FacilityDirection dir : FacilityDirection.values()) {
                GridCell neighbor = new GridCell(cell.x() + dir.dx, cell.z() + dir.dz);
                if (occupied.contains(neighbor)) {
                    openSides.add(dir);
                }
            }

            int baseX = origin.getBlockX() + (cell.x() * GRID_SIZE);
            int baseY = origin.getBlockY();
            int baseZ = origin.getBlockZ() + (cell.z() * GRID_SIZE);

            roomBuilder.build(world, baseX, baseY, baseZ, type, openSides);
        }
    }

    private Location cellToWorldCenter(Location origin, GridCell cell) {
        return new Location(origin.getWorld(),
                origin.getBlockX() + (cell.x() * GRID_SIZE) + (GRID_SIZE / 2.0),
                origin.getBlockY() + 2,
                origin.getBlockZ() + (cell.z() * GRID_SIZE) + (GRID_SIZE / 2.0));
    }

    private record GridCell(int x, int z) {
        List<GridCell> neighbors() {
            return List.of(
                    new GridCell(x + 1, z),
                    new GridCell(x - 1, z),
                    new GridCell(x, z + 1),
                    new GridCell(x, z - 1)
            );
        }

        double distanceFrom(GridCell other) {
            return Math.abs(x - other.x) + Math.abs(z - other.z);
        }
    }
}
