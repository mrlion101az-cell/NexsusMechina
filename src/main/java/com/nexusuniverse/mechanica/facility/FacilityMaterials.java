package com.nexusuniverse.mechanica.facility;

import org.bukkit.Material;

import java.util.List;
import java.util.Random;

/**
 * Every block choice for the procedural generator lives here, so the
 * whole facility's visual identity can be retuned in one file without
 * touching the generation logic itself.
 */
public class FacilityMaterials {

    public static final List<Material> FLOOR = List.of(
            Material.POLISHED_DEEPSLATE,
            Material.POLISHED_DEEPSLATE,
            Material.IRON_BLOCK,
            Material.GRAY_CONCRETE,
            Material.CRACKED_DEEPSLATE_TILES
    );

    public static final List<Material> WALL = List.of(
            Material.DEEPSLATE_TILES,
            Material.DEEPSLATE_TILES,
            Material.DEEPSLATE_BRICKS,
            Material.CRACKED_DEEPSLATE_BRICKS,
            Material.CHISELED_DEEPSLATE
    );

    public static final List<Material> CEILING = List.of(
            Material.DEEPSLATE_TILES,
            Material.SMOOTH_STONE,
            Material.IRON_BLOCK
    );

    // occasionally replaces a wall/floor block to sell "abandoned" --
    // cobweb, missing blocks (air), and mossy variants
    public static final List<Material> DECAY_OVERLAY = List.of(
            Material.COBWEB,
            Material.AIR,
            Material.MOSS_BLOCK,
            Material.INFESTED_DEEPSLATE
    );

    public static final List<Material> LIGHT_SOURCES = List.of(
            Material.SEA_LANTERN,
            Material.REDSTONE_LAMP,
            Material.SOUL_LANTERN
    );

    // scattered floor props for ROOM pieces -- old workstations/crates
    public static final List<Material> ROOM_PROPS = List.of(
            Material.BARREL,
            Material.CAULDRON,
            Material.IRON_TRAPDOOR,
            Material.CHAIN,
            Material.LADDER
    );

    private static final Random RANDOM = new Random();

    public static Material pick(List<Material> palette) {
        return palette.get(RANDOM.nextInt(palette.size()));
    }

    /** Returns `normal` most of the time, occasionally a decay-overlay material instead. */
    public static Material withDecayChance(Material normal, double decayChance) {
        if (RANDOM.nextDouble() < decayChance) {
            return pick(DECAY_OVERLAY);
        }
        return normal;
    }
}
