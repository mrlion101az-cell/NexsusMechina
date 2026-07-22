package com.nexusuniverse.mechanica.kinetic;

import org.bukkit.Material;

/**
 * Every kinetic block type in the system. Mirrors Create's role split:
 * SOURCE blocks generate rotational speed and stress capacity, RELAY
 * blocks pass speed through unchanged, MACHINE blocks consume stress to
 * do work.
 *
 * "anchorMaterial" is the real vanilla block this kinetic block is
 * placed as -- Paper has no API for truly new block types, so every
 * kinetic block is secretly a real block (tagged via PersistentDataContainer)
 * with a BlockDisplay entity layered on top for the actual visual.
 */
public enum KineticBlockType {

    HAND_CRANK(Role.SOURCE, Material.OAK_FENCE, 0, 8, 16),
    SHAFT(Role.RELAY, Material.OAK_FENCE, 0, 0, 0),
    COGWHEEL(Role.RELAY, Material.OAK_FENCE, 0, 0, 0),
    MECHANICAL_PRESS(Role.MACHINE, Material.SMOOTH_STONE, 4, 0, 0),
    MILLSTONE(Role.MACHINE, Material.SMOOTH_STONE, 2, 0, 0),
    SAW(Role.MACHINE, Material.SMOOTH_STONE, 3, 0, 0),
    /**
     * Anchors a ROTATING contraption -- place one, then assemble the
     * structure touching it. Its stress demand is a flat value, NOT
     * scaled by the size of the attached contraption -- a real known
     * limitation (a 4000-block Ferris wheel costs the same simulated
     * stress as a 10-block one), see the contraption README section.
     */
    CONTRAPTION_BEARING(Role.MACHINE, Material.SMOOTH_STONE, 6, 0, 0);

    public enum Role { SOURCE, RELAY, MACHINE }

    private final Role role;
    private final Material anchorMaterial;
    /** Stress this block consumes from its network when active (0 for sources/relays). */
    private final int stressDemand;
    /** Stress capacity this block contributes if it's a source (0 otherwise). */
    private final int stressCapacity;
    /** Rotational speed (arbitrary units, matching Create's RPM scale) this source provides. */
    private final int baseSpeed;

    KineticBlockType(Role role, Material anchorMaterial, int stressDemand, int stressCapacity, int baseSpeed) {
        this.role = role;
        this.anchorMaterial = anchorMaterial;
        this.stressDemand = stressDemand;
        this.stressCapacity = stressCapacity;
        this.baseSpeed = baseSpeed;
    }

    public Role role() {
        return role;
    }

    public Material anchorMaterial() {
        return anchorMaterial;
    }

    public int stressDemand() {
        return stressDemand;
    }

    public int stressCapacity() {
        return stressCapacity;
    }

    public int baseSpeed() {
        return baseSpeed;
    }

    public boolean isSource() {
        return role == Role.SOURCE;
    }

    public boolean isMachine() {
        return role == Role.MACHINE;
    }
}
