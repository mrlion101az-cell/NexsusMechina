package com.nexusuniverse.mechanica.facility;

import com.nexusuniverse.mechanica.NexusMechanicaPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * The consumable players bring back to a facility to keep it running.
 * Tagged via PersistentDataContainer so it can't be faked with a plain
 * iron ingot.
 */
public class FacilityItems {

    public static final double MAINTENANCE_RESTORE_AMOUNT = 40.0;
    public static final double MAINTAIN_USE_RADIUS = 10.0;

    private final NamespacedKey kitKey;

    public FacilityItems(NexusMechanicaPlugin plugin) {
        this.kitKey = new NamespacedKey(plugin, "facility_maintenance_kit");
    }

    public ItemStack createKit() {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bMaintenance Kit");
        meta.setLore(List.of(
                "§7Use near a facility's generator room",
                "§7to restore §f" + (int) MAINTENANCE_RESTORE_AMOUNT + "% §7integrity."
        ));
        meta.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isKit(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_INGOT || !item.hasItemMeta()) return false;
        Byte tag = item.getItemMeta().getPersistentDataContainer().get(kitKey, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }
}
