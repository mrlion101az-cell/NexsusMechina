package com.nexusuniverse.mechanica.facility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs on a slow interval (see the scheduler call in
 * NexusMechanicaPlugin -- default every real-world minute) and drains
 * a little integrity off every restored facility. This is what makes
 * "keep it online" an actual ongoing task instead of a one-time
 * restoration -- ignore a facility long enough and it goes dark again.
 */
public class FacilityMaintenanceTask extends BukkitRunnable {

    public static final double WARNING_THRESHOLD = 25.0;
    private static final double DECAY_PER_TICK = 4.0; // integrity lost per run of this task
    private static final double WARNING_RADIUS = 60.0;

    private final FacilityManager facilityManager;

    public FacilityMaintenanceTask(FacilityManager facilityManager) {
        this.facilityManager = facilityManager;
    }

    @Override
    public void run() {
        for (FacilitySite site : facilityManager.all()) {
            if (!site.isRestored()) continue;

            site.decay(DECAY_PER_TICK);

            if (site.getIntegrity() <= 0.0) {
                site.markOffline();
                broadcastNear(site, Component.text(
                        "[" + site.getId() + "] Power failure -- the facility has gone dark. "
                                + "It will need to be fully restored again.",
                        NamedTextColor.RED));
                continue;
            }

            if (site.getIntegrity() <= WARNING_THRESHOLD && !site.hasWarnedThisCycle()) {
                site.setWarnedThisCycle(true);
                broadcastNear(site, Component.text(
                        "[" + site.getId() + "] Integrity at " + (int) site.getIntegrity()
                                + "% -- bring a Maintenance Kit before it fails.",
                        NamedTextColor.YELLOW));
            }
        }
    }

    private void broadcastNear(FacilitySite site, Component message) {
        var center = site.getGeneratorLocation();
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= WARNING_RADIUS) {
                player.sendMessage(message);
            }
        }
    }
}
