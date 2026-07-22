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
 * Grinds food/organic items into seeds -- a distinct theme from
 * MechanicalPress's compression recipes (cobble->gravel->flint), so the
 * two machines feel like genuinely different processing steps rather
 * than the same mechanic reskinned.
 */
public class Millstone {

    private static final Map<Material, ItemStack> RECIPES = new HashMap<>();

    static {
        RECIPES.put(Material.WHEAT, new ItemStack(Material.WHEAT_SEEDS, 3));
        RECIPES.put(Material.BEETROOT, new ItemStack(Material.BEETROOT_SEEDS, 2));
        RECIPES.put(Material.MELON_SLICE, new ItemStack(Material.MELON_SEEDS, 1));
        RECIPES.put(Material.PUMPKIN, new ItemStack(Material.PUMPKIN_SEEDS, 4));
    }

    public boolean tryProcess(Location millstoneLocation) {
        Location scanCenter = millstoneLocation.clone().add(0.5, 1.1, 0.5);
        List<Item> items = millstoneLocation.getWorld().getNearbyEntities(scanCenter, 0.6, 0.6, 0.6)
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

            millstoneLocation.getWorld().dropItem(scanCenter, output.clone());
            millstoneLocation.getWorld().playSound(millstoneLocation, Sound.BLOCK_GRAVEL_BREAK, 0.6f, 0.8f);
            return true;
        }

        return false;
    }
}
