package com.itmecasey.primamateria.machine;

import net.minecraft.server.level.ServerLevel;

/**
 * A machine that a drive block under it can work: the sieve and the auto hammer. A drive calls {@link #hasWork} to see whether it
 * should burn fuel or use energy, and {@link #drive} once per shake or stroke. The machine delivers its results into the
 * container below it, which is the drive itself (a drive is a container with an output buffer), so a full buffer stops the work.
 */
public interface Drivable {
    /** True when a shake or stroke would do something: a block is in, and the output below has room. */
    boolean hasWork(ServerLevel level);

    /**
     * One shake or stroke. {@code minTicks} is the least number of game ticks since the machine's last shake (0 for no limit).
     * Returns whether work was done.
     */
    boolean drive(ServerLevel level, int minTicks);
}
