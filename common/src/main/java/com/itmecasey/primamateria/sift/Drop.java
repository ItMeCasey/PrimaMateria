package com.itmecasey.primamateria.sift;

/** A stack of one item: full item id and count. */
public record Drop(String item, int count) {
    public Drop {
        if (count < 1) {
            throw new IllegalArgumentException("count must be at least 1: " + count);
        }
    }
}
