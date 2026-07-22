package com.nexusuniverse.mechanica.machine;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes item entities resting on top of a powered MECHANICAL_PRESS
 * block. V1 works on world item entities rather than inventory slots --
 * simpler to get right first, and it's a legitimate pattern (items can
 * be fed in via hoppers dropping onto the press, or dropped by hand),
 * chains naturally into future belt/conveyor mechanics since everything
 * is already a real item entity in the world.
 */
public class MechanicalPress {

    private static final Map<Material, ItemStack> RECIPES = new HashMap<>();

    static {
        RECIPES.put(Material.COBBLESTONE, new ItemStack(Material.GRAVEL));
        RECIPES.put(Material.GRAVEL, new ItemStack(Material.FLINT));
        RECIPES.put(Material.IRON_INGOT, new ItemStack(Material.IRON_NUGGET, 9));
        RECIPES.put(Material.WHEAT, new ItemStack(Material.HAY_BLOCK));
    }

    /**
     * Attempts to process one item stack found on top of the given
     * location. Returns true if something was processed (caller should
     * throttle how often this runs -- once per second is plenty, calling
     * it every tick would be wasteful).
     */
    public boolean tryProcess(Location pressLocation) {
        Location scanCenter = pressLocation.clone().add(0.5, 1.1, 0.5);
        List<Item> items = pressLocation.getWorld().getNearbyEntities(scanCenter, 0.6, 0.6, 0.6)
                .stream()
                .filter(e -> e instanceof Item)
                .map(e -> (Item) e)
                .toList();

        for (Item entityItem : items) {
            ItemStack stack = entityItem.getItemStack();
            ItemStack output = RECIPES.get(stack.getType());
            if (output == null) {
                continue;
            }

            stack.setAmount(stack.getAmount() - 1);
            if (stack.getAmount() <= 0) {
                entityItem.remove();
            } else {
                entityItem.setItemStack(stack);
            }

            ItemStack drop = output.clone();
            pressLocation.getWorld().dropItem(scanCenter, drop);
            pressLocation.getWorld().playSound(pressLocation, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.6f);
            return true;
        }

        return false;
    }
}
