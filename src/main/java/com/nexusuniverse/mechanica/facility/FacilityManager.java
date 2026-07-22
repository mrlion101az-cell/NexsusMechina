package com.nexusuniverse.mechanica.facility;

import com.nexusuniverse.mechanica.NexusMechanicaPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Top-level entry point for the facility system: owns the generator
 * and item factory, and tracks every site that's been generated this
 * session. Generation is always available -- no piece library to load
 * or run out of, since everything is built procedurally.
 *
 * NOTE: sites are in-memory only in this pass -- a server restart
 * currently loses track of which facilities exist (the actual placed
 * blocks stay in the world fine, just the plugin's bookkeeping of
 * "where are they / are they restored / their integrity" doesn't
 * survive yet). Flagged clearly in the README as the next thing to
 * add, same pattern as every other manager in this codebase (which all
 * DO persist) -- didn't want to guess at a persistence format before
 * you've seen the generation and maintenance loop itself work.
 */
public class FacilityManager {

    private final NexusMechanicaPlugin plugin;
    private final FacilityGenerator generator;
    private final FacilityItems items;
    private final List<FacilitySite> sites = new ArrayList<>();

    public FacilityManager(NexusMechanicaPlugin plugin) {
        this.plugin = plugin;
        this.generator = new FacilityGenerator();
        this.items = new FacilityItems(plugin);
    }

    public void load() {
        // nothing to load -- kept for symmetry with the other managers
        // and as a hook point for future persistence
    }

    public FacilitySite generateAt(Location origin) {
        String id = "facility-" + UUID.randomUUID().toString().substring(0, 8);
        FacilitySite site = generator.generate(origin, id);
        if (site != null) {
            sites.add(site);
        }
        return site;
    }

    public List<FacilitySite> all() {
        return sites;
    }

    public FacilityItems getItems() {
        return items;
    }

    public FacilitySite findNearest(Location location, double radius) {
        FacilitySite closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (FacilitySite site : sites) {
            if (!site.getGeneratorLocation().getWorld().equals(location.getWorld())) continue;
            double distance = site.getGeneratorLocation().distance(location);
            if (distance <= radius && distance < closestDistance) {
                closest = site;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * Manual admin trigger for the initial restoration until the real
     * "repair the broken generator-room machine" mechanic (tied into
     * the kinetic network system) is built -- see README.
     */
    public String restoreNearest(Player player) {
        FacilitySite site = findNearest(player.getLocation(), 20.0);
        if (site == null) {
            return "No facility generator room nearby.";
        }
        if (site.isRestored()) {
            return "That facility is already restored.";
        }
        site.markRestored();
        return "Facility \"" + site.getId() + "\" restored -- integrity 100%. It will decay over time; bring Maintenance Kits to keep it running.";
    }

    public String maintainNearest(Player player) {
        FacilitySite site = findNearest(player.getLocation(), FacilityItems.MAINTAIN_USE_RADIUS);
        if (site == null) {
            return "No facility generator room within range.";
        }
        if (!site.isRestored()) {
            return "That facility is offline -- it needs a full restoration, not maintenance.";
        }

        var inventory = player.getInventory();
        boolean consumed = false;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (items.isKit(inventory.getItem(i))) {
                var stack = inventory.getItem(i);
                stack.setAmount(stack.getAmount() - 1);
                consumed = true;
                break;
            }
        }

        if (!consumed) {
            return "You need a Maintenance Kit to do this.";
        }

        site.maintain(FacilityItems.MAINTENANCE_RESTORE_AMOUNT);
        return "Maintained \"" + site.getId() + "\" -- integrity now " + (int) site.getIntegrity() + "%.";
    }
}
