package com.nexusuniverse.mechanica.task;

import com.nexusuniverse.mechanica.display.RotationDisplayManager;
import com.nexusuniverse.mechanica.kinetic.KineticBlockType;
import com.nexusuniverse.mechanica.kinetic.KineticNetwork;
import com.nexusuniverse.mechanica.kinetic.KineticNetworkManager;
import com.nexusuniverse.mechanica.kinetic.PlacedKineticBlock;
import com.nexusuniverse.mechanica.machine.MechanicalPress;
import com.nexusuniverse.mechanica.machine.Millstone;
import com.nexusuniverse.mechanica.machine.Saw;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs every tick to keep rotation visuals smooth. Machine processing
 * (which is comparatively expensive -- scanning for item entities) only
 * runs once every 20 ticks (once per second) rather than every tick,
 * since presses don't need tick-perfect responsiveness the way visual
 * rotation does.
 */
public class KineticTickTask extends BukkitRunnable {

    private static final float DELTA_SECONDS = 1f / 20f;
    private static final int MACHINE_PROCESS_INTERVAL_TICKS = 20;

    private final KineticNetworkManager networkManager;
    private final RotationDisplayManager displayManager;
    private final MechanicalPress mechanicalPress;
    private final Millstone millstone;
    private final Saw saw;

    private int tickCounter = 0;

    public KineticTickTask(KineticNetworkManager networkManager, RotationDisplayManager displayManager,
                            MechanicalPress mechanicalPress, Millstone millstone, Saw saw) {
        this.networkManager = networkManager;
        this.displayManager = displayManager;
        this.mechanicalPress = mechanicalPress;
        this.millstone = millstone;
        this.saw = saw;
    }

    @Override
    public void run() {
        tickCounter++;
        boolean processMachinesThisTick = tickCounter % MACHINE_PROCESS_INTERVAL_TICKS == 0;

        for (KineticNetwork network : networkManager.networks()) {
            int speed = network.currentSpeed();

            for (String key : network.memberKeys()) {
                PlacedKineticBlock block = networkManager.getByKey(key);
                if (block == null) {
                    continue;
                }

                displayManager.tick(block, speed, DELTA_SECONDS);

                if (processMachinesThisTick && !network.isStalled() && block.type().isMachine()) {
                    processMachine(block);
                }
            }
        }
    }

    private void processMachine(PlacedKineticBlock block) {
        boolean processed = switch (block.type()) {
            case MECHANICAL_PRESS -> mechanicalPress.tryProcess(block.location());
            case MILLSTONE -> millstone.tryProcess(block.location());
            case SAW -> saw.tryProcess(block.location());
            default -> false;
        };

        if (processed) {
            displayManager.squish(block);
        }
    }
}
