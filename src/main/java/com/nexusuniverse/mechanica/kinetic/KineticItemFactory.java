package com.nexusuniverse.mechanica.kinetic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Creates the placeable items for each kinetic block type. Several types
 * (HAND_CRANK, SHAFT, COGWHEEL) share the same real anchor material, so
 * the type is disambiguated via a PersistentDataContainer tag on the
 * item -- the same pattern NexusBridge's ArtifactFactory already uses
 * for tagging artifact items, kept consistent across both plugins.
 */
public class KineticItemFactory {

    private final NamespacedKey typeKey;
    private final NamespacedKey wrenchKey;

    public KineticItemFactory(JavaPlugin plugin) {
        this.typeKey = new NamespacedKey(plugin, "nexusmechanica_block_type");
        this.wrenchKey = new NamespacedKey(plugin, "nexusmechanica_wrench");
    }

    /** A diagnostic tool -- right-click any kinetic block with this held to see its network's live stress numbers. */
    public ItemStack createWrench() {
        ItemStack item = new ItemStack(org.bukkit.Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Kinetic Wrench", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Right-click a kinetic block to inspect", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("its network's speed and stress.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(wrenchKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWrench(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        Boolean value = item.getItemMeta().getPersistentDataContainer().get(wrenchKey, PersistentDataType.BOOLEAN);
        return value != null && value;
    }

    public NamespacedKey typeKey() {
        return typeKey;
    }

    public ItemStack create(KineticBlockType type) {
        ItemStack item = new ItemStack(type.anchorMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(displayName(type), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(description(type), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());

        item.setItemMeta(meta);
        return item;
    }

    public KineticBlockType readType(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return KineticBlockType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String displayName(KineticBlockType type) {
        return switch (type) {
            case HAND_CRANK -> "Hand Crank";
            case SHAFT -> "Shaft";
            case COGWHEEL -> "Cogwheel";
            case MECHANICAL_PRESS -> "Mechanical Press";
            case MILLSTONE -> "Millstone";
            case SAW -> "Mechanical Saw";
            case CONTRAPTION_BEARING -> "Contraption Bearing";
        };
    }

    private String description(KineticBlockType type) {
        return switch (type.role()) {
            case SOURCE -> "Generates rotational power.";
            case RELAY -> "Transmits power to adjacent blocks.";
            case MACHINE -> "Consumes power to process items.";
        };
    }
}
