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
 * Cuts logs into planks, and planks into sticks -- the third distinct
 * processing theme (cutting, vs. the Press's compression and the
 * Millstone's grinding).
 */
public class Saw {

    private static final Map<Material, ItemStack> RECIPES = new HashMap<>();

    static {
        RECIPES.put(Material.OAK_LOG, new ItemStack(Material.OAK_PLANKS, 4));
        RECIPES.put(Material.SPRUCE_LOG, new ItemStack(Material.SPRUCE_PLANKS, 4));
        RECIPES.put(Material.BIRCH_LOG, new ItemStack(Material.BIRCH_PLANKS, 4));
        RECIPES.put(Material.DARK_OAK_LOG, new ItemStack(Material.DARK_OAK_PLANKS, 4));
        RECIPES.put(Material.OAK_PLANKS, new ItemStack(Material.STICK, 2));
    }

    public boolean tryProcess(Location sawLocation) {
        Location scanCenter = sawLocation.clone().add(0.5, 1.1, 0.5);
        List<Item> items = sawLocation.getWorld().getNearbyEntities(scanCenter, 0.6, 0.6, 0.6)
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

            sawLocation.getWorld().dropItem(scanCenter, output.clone());
            sawLocation.getWorld().playSound(sawLocation, Sound.BLOCK_WOOD_BREAK, 0.6f, 1.2f);
            return true;
        }

        return false;
    }
}
