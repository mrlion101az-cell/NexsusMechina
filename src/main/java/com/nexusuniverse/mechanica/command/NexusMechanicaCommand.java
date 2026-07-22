package com.nexusuniverse.mechanica.command;

import com.nexusuniverse.mechanica.contraption.Contraption;
import com.nexusuniverse.mechanica.contraption.ContraptionManager;
import com.nexusuniverse.mechanica.contraption.ContraptionMode;
import com.nexusuniverse.mechanica.facility.FacilityManager;
import com.nexusuniverse.mechanica.facility.FacilitySite;
import com.nexusuniverse.mechanica.guide.GuideBookFactory;
import com.nexusuniverse.mechanica.kinetic.KineticBlockType;
import com.nexusuniverse.mechanica.kinetic.KineticItemFactory;
import com.nexusuniverse.mechanica.kinetic.KineticNetworkManager;
import com.nexusuniverse.mechanica.kinetic.PlacedKineticBlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NexusMechanicaCommand implements CommandExecutor {

    private final KineticNetworkManager networkManager;
    private final KineticItemFactory itemFactory;
    private final ContraptionManager contraptionManager;
    private final FacilityManager facilityManager;
    private final GuideBookFactory guideBookFactory = new GuideBookFactory();

    public NexusMechanicaCommand(KineticNetworkManager networkManager, KineticItemFactory itemFactory,
                                  ContraptionManager contraptionManager, FacilityManager facilityManager) {
        this.networkManager = networkManager;
        this.itemFactory = itemFactory;
        this.contraptionManager = contraptionManager;
        this.facilityManager = facilityManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /nexusmechanica <status|give <type|wrench>|guide|contraption|facility>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> {
                sender.sendMessage(Component.text(
                        "NexusMechanica: " + networkManager.totalBlockCount() + " block(s), "
                                + networkManager.totalNetworkCount() + " network(s), "
                                + contraptionManager.all().size() + " active contraption(s), "
                                + facilityManager.all().size() + " facilities generated.",
                        NamedTextColor.AQUA));
                return true;
            }
            case "give" -> {
                return handleGive(sender, args);
            }
            case "guide" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can be given the guide book.", NamedTextColor.RED));
                    return true;
                }
                player.getInventory().addItem(guideBookFactory.create());
                sender.sendMessage(Component.text("Gave 1x NexusMechanica Guide", NamedTextColor.GREEN));
                return true;
            }
            case "contraption" -> {
                return handleContraption(sender, args);
            }
            case "facility" -> {
                return handleFacility(sender, args);
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                return true;
            }
        }
    }

    private boolean handleFacility(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nexusmechanica facility <generate|list|restore|maintain|givekit>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "generate" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can generate a facility (uses your location).", NamedTextColor.RED));
                    return true;
                }
                FacilitySite site = facilityManager.generateAt(player.getLocation());
                if (site == null) {
                    sender.sendMessage(Component.text("Generation failed -- check console for the error.", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text(
                        "Generated facility \"" + site.getId() + "\" -- " + site.getPieceCount()
                                + " piece(s). Generator room center: "
                                + site.getGeneratorLocation().getBlockX() + ", "
                                + site.getGeneratorLocation().getBlockY() + ", "
                                + site.getGeneratorLocation().getBlockZ(),
                        NamedTextColor.GREEN));
                return true;
            }
            case "list" -> {
                if (facilityManager.all().isEmpty()) {
                    sender.sendMessage(Component.text("No facilities generated yet this session.", NamedTextColor.YELLOW));
                    return true;
                }
                sender.sendMessage(Component.text("Generated facilities:", NamedTextColor.AQUA));
                for (FacilitySite site : facilityManager.all()) {
                    sender.sendMessage(Component.text(
                            " - " + site.getId() + " (" + site.getPieceCount() + " pieces, restored: "
                                    + site.isRestored() + ", integrity: " + (int) site.getIntegrity() + "%)",
                            NamedTextColor.GRAY));
                }
                return true;
            }
            case "restore" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can restore a facility (uses your location).", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text(facilityManager.restoreNearest(player), NamedTextColor.GREEN));
                return true;
            }
            case "maintain" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can maintain a facility.", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text(facilityManager.maintainNearest(player), NamedTextColor.GREEN));
                return true;
            }
            case "givekit" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can be given items.", NamedTextColor.RED));
                    return true;
                }
                player.getInventory().addItem(facilityManager.getItems().createKit());
                sender.sendMessage(Component.text("Gave 1x Maintenance Kit", NamedTextColor.GREEN));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown facility subcommand.", NamedTextColor.RED));
                return true;
            }
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can be given items.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nexusmechanica give <type|wrench>", NamedTextColor.YELLOW));
            return true;
        }

        if (args[1].equalsIgnoreCase("wrench")) {
            player.getInventory().addItem(itemFactory.createWrench());
            sender.sendMessage(Component.text("Gave 1x Kinetic Wrench", NamedTextColor.GREEN));
            return true;
        }

        KineticBlockType type;
        try {
            type = KineticBlockType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Unknown type. Options: " + typeList() + ", wrench", NamedTextColor.RED));
            return true;
        }
        ItemStack item = itemFactory.create(type);
        player.getInventory().addItem(item);
        sender.sendMessage(Component.text("Gave 1x " + type.name(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleContraption(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use contraption commands.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /nexusmechanica contraption <assemble <rotate|linear>|disassemble|throttle|mount|dismount>", NamedTextColor.YELLOW));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null) {
            player.sendMessage(Component.text("No block in range -- look directly at the anchor block.", NamedTextColor.RED));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "assemble" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /nexusmechanica contraption assemble <rotate|linear>", NamedTextColor.YELLOW));
                    return true;
                }

                ContraptionMode mode;
                if (args[2].equalsIgnoreCase("rotate")) {
                    mode = ContraptionMode.ROTATING;
                } else if (args[2].equalsIgnoreCase("linear")) {
                    mode = ContraptionMode.LINEAR;
                } else {
                    player.sendMessage(Component.text("Mode must be 'rotate' or 'linear'.", NamedTextColor.RED));
                    return true;
                }

                PlacedKineticBlock bearing = null;
                if (mode == ContraptionMode.ROTATING) {
                    bearing = networkManager.getAt(targetBlock.getLocation());
                    if (bearing == null || bearing.type() != KineticBlockType.CONTRAPTION_BEARING) {
                        player.sendMessage(Component.text(
                                "Look directly at a placed Contraption Bearing block first (give one with /nexusmechanica give contraption_bearing).",
                                NamedTextColor.RED));
                        return true;
                    }
                }

                ContraptionManager.AssemblyResult result = contraptionManager.assemble(mode, targetBlock.getLocation(), player.getFacing());
                if (!result.success()) {
                    player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
                    return true;
                }

                if (mode == ContraptionMode.ROTATING) {
                    result.contraption().setRotationAxis(bearing.axis());
                    player.sendMessage(Component.text(
                            "Assembled " + result.blockCount() + " block(s). Power the bearing's kinetic network to make it spin.",
                            NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text(
                            "Assembled " + result.blockCount() + " block(s), facing " + player.getFacing().name()
                                    + ". Use /nexusmechanica contraption throttle to move it.",
                            NamedTextColor.GREEN));
                }
                return true;
            }
            case "disassemble" -> {
                ContraptionManager.DisassemblyResult result = contraptionManager.disassemble(targetBlock.getLocation());
                if (result.success()) {
                    player.sendMessage(Component.text("Disassembled " + result.blockCount() + " block(s) back into the world.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
                }
                return true;
            }
            case "throttle" -> {
                Contraption contraption = contraptionManager.byAnchor(targetBlock.getLocation());
                if (contraption == null) {
                    player.sendMessage(Component.text("No assembled contraption at that anchor.", NamedTextColor.RED));
                    return true;
                }
                if (contraption.mode() != ContraptionMode.LINEAR) {
                    player.sendMessage(Component.text("Only LINEAR contraptions have a throttle -- ROTATING ones are driven by their kinetic network instead.", NamedTextColor.RED));
                    return true;
                }
                contraption.setThrottleOn(!contraption.isThrottleOn());
                player.sendMessage(Component.text(
                        contraption.isThrottleOn() ? "Throttle engaged." : "Throttle stopped.",
                        contraption.isThrottleOn() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "mount" -> {
                boolean mounted = contraptionManager.mount(targetBlock.getLocation(), player);
                if (mounted) {
                    player.sendMessage(Component.text("Riding. You'll move with the contraption -- use /nexusmechanica contraption dismount to get off.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("No assembled contraption at that anchor.", NamedTextColor.RED));
                }
                return true;
            }
            case "dismount" -> {
                boolean wasRiding = contraptionManager.dismount(player);
                player.sendMessage(Component.text(
                        wasRiding ? "Dismounted." : "You weren't riding anything.",
                        wasRiding ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
                return true;
            }
            default -> {
                player.sendMessage(Component.text("Unknown contraption subcommand.", NamedTextColor.RED));
                return true;
            }
        }
    }

    private String typeList() {
        StringBuilder builder = new StringBuilder();
        for (KineticBlockType type : KineticBlockType.values()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(type.name());
        }
        return builder.toString();
    }
}
