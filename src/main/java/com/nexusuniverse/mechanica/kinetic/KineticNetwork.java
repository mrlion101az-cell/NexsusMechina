package com.nexusuniverse.mechanica.kinetic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A connected group of kinetic blocks that all share one rotation speed.
 * Mirrors Create's stress-unit system: every SOURCE block in the network
 * contributes stress capacity, every active MACHINE block consumes
 * stress. If total demand exceeds total capacity, the whole network
 * stalls (speed drops to zero) rather than partially running -- this is
 * a deliberate design choice matching Create's own behavior, and gives
 * players a real incentive to build multiple power sources for bigger
 * factories instead of one crank driving everything.
 */
public class KineticNetwork {

    private final UUID id = UUID.randomUUID();
    private final Set<String> memberKeys = new HashSet<>();

    private int stressCapacity;
    private int stressDemand;
    private int currentSpeed;
    private boolean stalled;

    public UUID id() {
        return id;
    }

    public Set<String> memberKeys() {
        return memberKeys;
    }

    public void addMember(PlacedKineticBlock block) {
        memberKeys.add(block.key());
        block.setNetworkId(id);
    }

    public void removeMember(PlacedKineticBlock block) {
        memberKeys.remove(block.key());
    }

    public boolean isEmpty() {
        return memberKeys.isEmpty();
    }

    /**
     * Recomputes capacity/demand/speed from the current member set.
     * Call this after any block is added, removed, or changes active state.
     */
    public void recalculate(KineticNetworkManager manager) {
        int capacity = 0;
        int demand = 0;
        int highestSourceSpeed = 0;

        for (String key : memberKeys) {
            PlacedKineticBlock block = manager.getByKey(key);
            if (block == null) {
                continue;
            }
            KineticBlockType type = block.type();
            if (type.isSource() && block.isActive()) {
                capacity += type.stressCapacity();
                highestSourceSpeed = Math.max(highestSourceSpeed, type.baseSpeed());
            }
            if (type.isMachine()) {
                demand += type.stressDemand() + block.extraStressDemand();
            }
        }

        this.stressCapacity = capacity;
        this.stressDemand = demand;
        this.stalled = demand > capacity || capacity == 0;
        this.currentSpeed = stalled ? 0 : highestSourceSpeed;
    }

    public int stressCapacity() {
        return stressCapacity;
    }

    public int stressDemand() {
        return stressDemand;
    }

    public int currentSpeed() {
        return currentSpeed;
    }

    public boolean isStalled() {
        return stalled;
    }
}
