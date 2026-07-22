package com.nexusuniverse.mechanica.contraption;

import org.bukkit.block.data.BlockData;

/** One block captured into a contraption, as an offset from the anchor and its original BlockData. */
public record CapturedBlock(int relX, int relY, int relZ, BlockData blockData) {
}
