package com.nexusuniverse.mechanica.guide;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the NexusMechanica guide as a real in-game written book,
 * following the same convention other kinetic/tech plugins (and Create
 * itself) use for in-game documentation, rather than expecting players
 * to read a wiki page outside the game.
 */
public class GuideBookFactory {

    public ItemStack create() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("NexusMechanica Guide");
        meta.setAuthor("Nexus Universe");

        List<Component> pages = new ArrayList<>();

        pages.add(page(
                title("NexusMechanica"),
                body("A kinetic power system: connect sources, shafts, and cogwheels together, then power machines that turn one item into another."),
                body(""),
                body("Get placeable blocks and the diagnostic wrench with:"),
                command("/nexusmechanica give <type>")
        ));

        pages.add(page(
                title("Placing Blocks"),
                body("Every block you place gets an orientation from the direction you're facing -- this matters."),
                body(""),
                body("Two blocks only connect if the direction between them matches BOTH of their orientations. A north-south shaft will not connect to an east-west one, even touching.")
        ));

        pages.add(page(
                title("Hand Crank"),
                body("Your power source. Right-click to turn it on or off."),
                body(""),
                body("A network needs at least one active crank (or other source) with enough stress capacity for everything it's powering, or the whole network stalls.")
        ));

        pages.add(page(
                title("Shaft"),
                body("Passes power straight through. Connects only along its own placed direction -- same rule as everything else."),
                body(""),
                body("Use these to route power around corners by placing separate shafts along each direction you need.")
        ));

        pages.add(page(
                title("Cogwheel"),
                body("Like a shaft, but with one extra trick: two cogwheels with DIFFERENT (perpendicular) orientations can mesh together and still transmit power, as long as they're touching along the one direction neither of their own axes uses."),
                body(""),
                body("This is how you change the direction power flows through your build, not just extend it in a straight line.")
        ));

        pages.add(page(
                title("Mechanical Press"),
                body("Compresses items. Drop these on top of a powered press:"),
                body(""),
                body("Cobblestone -> Gravel"),
                body("Gravel -> Flint"),
                body("Iron Ingot -> 9 Nuggets"),
                body("Wheat -> Hay Block")
        ));

        pages.add(page(
                title("Millstone"),
                body("Grinds crops down to seeds. Drop these on top of a powered millstone:"),
                body(""),
                body("Wheat -> 3 Wheat Seeds"),
                body("Beetroot -> 2 Beetroot Seeds"),
                body("Melon Slice -> 1 Melon Seed"),
                body("Pumpkin -> 4 Pumpkin Seeds")
        ));

        pages.add(page(
                title("Saw"),
                body("Cuts wood. Drop these on top of a powered saw:"),
                body(""),
                body("Any Log -> 4 matching Planks"),
                body("Oak Planks -> 2 Sticks")
        ));

        pages.add(page(
                title("The Wrench"),
                body("A diagnostic tool, not a placeable block. Right-click any kinetic block while holding it to see that block's network: how many blocks are in it, current speed, and whether it's stressed past capacity."),
                body(""),
                body("Get one with:"),
                command("/nexusmechanica give wrench")
        ));

        pages.add(page(
                title("Contraptions"),
                body("Connect real blocks into a structure that moves as one unit -- a Ferris wheel, a ship, anything."),
                body(""),
                body("Capped at 4,000 blocks. This isn't arbitrary -- it's the real limit of what Minecraft's engine can move smoothly, the same wall the actual Create mod runs into.")
        ));

        pages.add(page(
                title("Rotating Contraptions"),
                body("Place a Contraption Bearing, connect it to a powered kinetic network, then stand facing the structure and run:"),
                command("/nexusmechanica contraption assemble rotate"),
                body(""),
                body("It spins at the network's real live speed -- the same power system as everything else.")
        ));

        pages.add(page(
                title("Linear Contraptions"),
                body("For ships and vehicles. No bearing needed -- just stand facing the structure and run:"),
                command("/nexusmechanica contraption assemble linear"),
                body(""),
                body("Then toggle movement with:"),
                command("/nexusmechanica contraption throttle"),
                body(""),
                body("Moves in a straight line at a constant speed until throttled off.")
        ));

        pages.add(page(
                title("Riding"),
                body("Look at any assembled contraption's anchor and run:"),
                command("/nexusmechanica contraption mount"),
                body(""),
                body("You'll be carried along -- spinning with a Ferris wheel, or moving with a ship. Get off anytime with:"),
                command("/nexusmechanica contraption dismount")
        ));

        pages.add(page(
                title("Disassembling"),
                body("Look at the anchor block and run:"),
                command("/nexusmechanica contraption disassemble"),
                body(""),
                body("Rotating contraptions must be stopped near a clean 90-degree angle first -- this version won't guess where blocks belong from an arbitrary spin position.")
        ));

        pages.add(page(
                title("Quick Start"),
                body("1. Give yourself a Hand Crank and a Mechanical Press."),
                body("2. Place them touching each other."),
                body("3. Right-click the crank to turn it on."),
                body("4. Drop a cobblestone on top of the press."),
                body(""),
                body("If it turns to gravel, it's working.")
        ));

        meta.addPage(pages.toArray(new Component[0]));
        book.setItemMeta(meta);
        return book;
    }

    private Component title(String text) {
        return Component.text(text, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD).append(Component.newline()).append(Component.newline());
    }

    private Component body(String text) {
        return Component.text(text, NamedTextColor.BLACK).append(Component.newline());
    }

    private Component command(String text) {
        return Component.text(text, NamedTextColor.DARK_BLUE, TextDecoration.ITALIC).append(Component.newline());
    }

    private Component page(Component... parts) {
        Component result = Component.empty();
        for (Component part : parts) {
            result = result.append(part);
        }
        return result;
    }
}
