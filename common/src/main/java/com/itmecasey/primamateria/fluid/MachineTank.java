package com.itmecasey.primamateria.fluid;

/**
 * One machine's fluid as a pipe, a tank mod or a capability sees it: a single tank that holds one fluid kind at a time. The
 * machines keep their fluid as a plain id and an amount in mB; an implementation is only an adapter over that state, so nothing
 * here knows about the game or a loader. Pure Java.
 *
 * <p>{@code simulate} never changes anything and answers exactly what the same call without it would do right now.
 */
public interface MachineTank {
    /** The fluid id inside, or an empty string when the tank is empty. */
    String fluid();

    /** The amount inside in mB. */
    int amount();

    /** The most of {@code fluid} the tank can hold in total (also for a kind it holds none of right now). */
    int capacity(String fluid);

    /** Whether the tank would take this fluid kind at all in its present state (ignoring how much room is left). */
    boolean accepts(String fluid);

    /** Puts up to {@code amount} mB of {@code fluid} in and returns how much went in (0 when refused). */
    int insert(String fluid, int amount, boolean simulate);

    /**
     * Takes up to {@code max} mB out and returns how much came out. {@code fluid} names the kind wanted; an empty string or null
     * means whatever is inside.
     */
    int extract(String fluid, int max, boolean simulate);

    /** The tank's state, to be handed to {@link #restore} when a transaction is rolled back. */
    Object snapshot();

    void restore(Object snapshot);
}
