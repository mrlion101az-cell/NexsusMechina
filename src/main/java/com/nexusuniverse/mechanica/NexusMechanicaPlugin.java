package com.nexusuniverse.mechanica;

import com.nexusuniverse.mechanica.command.NexusMechanicaCommand;
import com.nexusuniverse.mechanica.contraption.ContraptionManager;
import com.nexusuniverse.mechanica.contraption.ContraptionTickTask;
import com.nexusuniverse.mechanica.display.RotationDisplayManager;
import com.nexusuniverse.mechanica.facility.FacilityManager;
import com.nexusuniverse.mechanica.facility.FacilityMaintenanceTask;
import com.nexusuniverse.mechanica.kinetic.KineticItemFactory;
import com.nexusuniverse.mechanica.kinetic.KineticNetworkManager;
import com.nexusuniverse.mechanica.listener.KineticBlockListener;
import com.nexusuniverse.mechanica.machine.MechanicalPress;
import com.nexusuniverse.mechanica.machine.Millstone;
import com.nexusuniverse.mechanica.machine.Saw;
import com.nexusuniverse.mechanica.task.KineticTickTask;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class NexusMechanicaPlugin extends JavaPlugin {

    private KineticNetworkManager networkManager;
    private KineticItemFactory itemFactory;
    private RotationDisplayManager displayManager;
    private MechanicalPress mechanicalPress;
    private Millstone millstone;
    private Saw saw;
    private ContraptionManager contraptionManager;
    private FacilityManager facilityManager;
    private BukkitTask tickTask;
    private BukkitTask contraptionTickTask;

    @Override
    public void onEnable() {
        this.networkManager = new KineticNetworkManager(this);
        this.itemFactory = new KineticItemFactory(this);
        this.displayManager = new RotationDisplayManager();
        this.mechanicalPress = new MechanicalPress();
        this.millstone = new Millstone();
        this.saw = new Saw();

        networkManager.load();
        // load() reconstructs block/network data but has no display entities
        // yet (those don't survive a restart in a re-linkable way) -- respawn
        // one fresh BlockDisplay per loaded block now, before the tick task
        // starts, so rotation doesn't silently freeze after every restart.
        for (var block : networkManager.allBlocks()) {
            displayManager.spawn(block.location(), block.type().anchorMaterial(), block);
        }

        // ContraptionManager needs networkManager (for stress linkage on
        // ROTATING contraptions), so it's built after the kinetic system
        // is fully loaded, and its own load() runs after that too -- a
        // restored contraption needs its bearing block to already exist
        // to correctly re-apply extra stress demand.
        this.contraptionManager = new ContraptionManager(this, networkManager);
        contraptionManager.load();

        this.facilityManager = new FacilityManager(this);
        facilityManager.load();

        // integrity decay runs every real-world minute (1200 ticks) --
        // slow enough that "keeping it online" is a real return-trip
        // task, not something you have to babysit constantly
        getServer().getScheduler().runTaskTimer(this, new FacilityMaintenanceTask(facilityManager), 1200L, 1200L);

        Bukkit.getPluginManager().registerEvents(
                new KineticBlockListener(networkManager, itemFactory, displayManager), this);

        KineticTickTask task = new KineticTickTask(networkManager, displayManager, mechanicalPress, millstone, saw);
        this.tickTask = task.runTaskTimer(this, 1L, 1L);

        ContraptionTickTask contraptionTask = new ContraptionTickTask(contraptionManager, networkManager);
        this.contraptionTickTask = contraptionTask.runTaskTimer(this, 1L, 1L);

        PluginCommand command = getCommand("nexusmechanica");
        if (command != null) {
            command.setExecutor(new NexusMechanicaCommand(networkManager, itemFactory, contraptionManager, facilityManager));
        }

        getLogger().info("NexusMechanica v" + getDescription().getVersion() + " enabled. "
                + networkManager.totalBlockCount() + " block(s) loaded across "
                + networkManager.totalNetworkCount() + " network(s), "
                + contraptionManager.all().size() + " contraption(s) restored.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (contraptionTickTask != null) {
            contraptionTickTask.cancel();
        }
        // save() persists exact contraption state (rotation angle,
        // position, throttle) across a restart -- riders are dismounted
        // first (see ContraptionManager.save()) since a mid-ride player
        // isn't something this version tries to restore. If save() itself
        // fails for any reason, it falls back to safely disassembling
        // everything back into real blocks rather than risking silent loss.
        if (contraptionManager != null) {
            contraptionManager.save();
        }
        if (networkManager != null) {
            // Remove every display entity on a clean shutdown so the next
            // load() doesn't spawn duplicates alongside leftover ones.
            // BlockDisplay entities are still marked persistent as a safety
            // net for ungraceful shutdowns (crashes), where this loop never runs.
            if (displayManager != null) {
                for (var block : networkManager.allBlocks()) {
                    displayManager.remove(block);
                }
            }
            networkManager.save();
        }
    }
}
