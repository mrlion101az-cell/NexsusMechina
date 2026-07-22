package com.nexusuniverse.mechanica.listener;

import com.nexusuniverse.mechanica.display.RotationDisplayManager;
import com.nexusuniverse.mechanica.kinetic.KineticBlockType;
import com.nexusuniverse.mechanica.kinetic.KineticItemFactory;
import com.nexusuniverse.mechanica.kinetic.KineticNetworkManager;
import com.nexusuniverse.mechanica.kinetic.PlacedKineticBlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class KineticBlockListener implements Listener {

    private final KineticNetworkManager networkManager;
    private final KineticItemFactory itemFactory;
    private final RotationDisplayManager displayManager;

    public KineticBlockListener(KineticNetworkManager networkManager, KineticItemFactory itemFactory, RotationDisplayManager displayManager) {
        this.networkManager = networkManager;
        this.itemFactory = itemFactory;
        this.displayManager = displayManager;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        KineticBlockType type = itemFactory.readType(event.getItemInHand());
        if (type == null) {
            return;
        }

        com.nexusuniverse.mechanica.kinetic.KineticAxis axis =
                com.nexusuniverse.mechanica.kinetic.KineticAxis.fromPlayerFacing(event.getPlayer().getFacing());

        PlacedKineticBlock block = networkManager.registerBlock(event.getBlock().getLocation(), type, axis);
        displayManager.spawn(event.getBlock().getLocation(), type.anchorMaterial(), block);

        event.getPlayer().sendMessage(Component.text(
                "Placed " + type.name() + " (axis: " + axis.name() + ") -- network now has "
                        + networkManager.networkOf(block).memberKeys().size() + " block(s).",
                NamedTextColor.GRAY));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        PlacedKineticBlock block = networkManager.getAt(event.getBlock().getLocation());
        if (block == null) {
            return;
        }
        displayManager.remove(block);
        networkManager.unregisterBlock(event.getBlock().getLocation());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        PlacedKineticBlock block = networkManager.getAt(event.getClickedBlock().getLocation());
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();

        if (itemFactory.isWrench(event.getItem())) {
            var network = networkManager.networkOf(block);
            player.sendMessage(Component.text("--- " + block.type().name() + " (" + block.axis().name() + " axis) ---", NamedTextColor.GOLD));
            if (network != null) {
                player.sendMessage(Component.text(
                        "Network: " + network.memberKeys().size() + " block(s), speed "
                                + network.currentSpeed() + ", stress " + network.stressDemand() + "/" + network.stressCapacity()
                                + (network.isStalled() ? " (STALLED)" : ""),
                        network.isStalled() ? NamedTextColor.RED : NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("No network data found.", NamedTextColor.RED));
            }
            event.setCancelled(true);
            return;
        }

        if (block.type() != KineticBlockType.HAND_CRANK) {
            return;
        }

        block.setActive(!block.isActive());
        networkManager.recalculateNetworkOf(block);

        player.sendMessage(Component.text(
                block.isActive() ? "Crank engaged." : "Crank disengaged.",
                block.isActive() ? NamedTextColor.GREEN : NamedTextColor.RED));

        event.setCancelled(true);
    }
}
