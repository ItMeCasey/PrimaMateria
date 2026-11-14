package com.itmecasey.primamateria.sift;

/** The finer material a sifted block can leave behind: gravel leaves sand, sand leaves stone meal. One item per block. */
public record ResidueEntry(String item, double chance) {
}
