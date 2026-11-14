package com.itmecasey.primamateria.crush;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The queue and stroke count of one auto hammer. The block entity owns one and saves it as plain strings. Blocks wait in a
 * queue of at most {@link #CAPACITY}; the oldest is struck {@code strokesPerBlock} times and then reported as finished.
 */
public final class AutoHammerState {
    public static final int CAPACITY = 16;

    private final Deque<String> queue = new ArrayDeque<>();
    private String current = "";
    private int strokes;
    private int needed = 1;
    private long lastStrokeTick = Long.MIN_VALUE / 2;

    /** Blocks waiting plus the one under the stamps. */
    public int queued() {
        return queue.size() + (current.isEmpty() ? 0 : 1);
    }

    public boolean canAccept() {
        return queued() < CAPACITY;
    }

    /** Adds a block to the queue; false when full. */
    public boolean offer(String blockId) {
        if (blockId == null || blockId.isEmpty() || !canAccept()) {
            return false;
        }
        queue.addLast(blockId);
        return true;
    }

    /** The block that will be (or is being) struck next, or empty. */
    public String next() {
        return current.isEmpty() ? (queue.isEmpty() ? "" : queue.peekFirst()) : current;
    }

    public boolean hasBlock() {
        return !next().isEmpty();
    }

    public long lastStrokeTick() {
        return lastStrokeTick;
    }

    /** True when a stroke would count at this game time. */
    public boolean ready(long gameTime, int minTicks) {
        return hasBlock() && gameTime - lastStrokeTick >= minTicks;
    }

    /** 0 to 1 for the block under the stamps. */
    public float progress() {
        return current.isEmpty() ? 0F : Math.min(1F, strokes / (float) needed);
    }

    /**
     * One stroke. Returns the id of the block that is now crushed, or null while it is still being struck (or when the queue is
     * empty). The caller rolls and delivers the results.
     */
    public String stroke(long gameTime, int strokesPerBlock) {
        if (current.isEmpty()) {
            if (queue.isEmpty()) {
                return null;
            }
            current = queue.pollFirst();
            strokes = 0;
            needed = Math.max(1, strokesPerBlock);
        }
        lastStrokeTick = gameTime;
        strokes++;
        if (strokes >= needed) {
            String done = current;
            current = "";
            strokes = 0;
            return done;
        }
        return null;
    }

    // ---- saving ----

    public String encodeQueue() {
        return String.join(";", queue);
    }

    public String current() {
        return current;
    }

    public int strokes() {
        return strokes;
    }

    public int needed() {
        return needed;
    }

    public void restore(String queueText, String currentId, int strokes, int needed, long lastStrokeTick) {
        queue.clear();
        if (queueText != null && !queueText.isEmpty()) {
            for (String id : queueText.split(";")) {
                if (!id.isEmpty() && queue.size() < CAPACITY) {
                    queue.addLast(id);
                }
            }
        }
        this.current = currentId == null ? "" : currentId;
        this.strokes = Math.max(0, strokes);
        this.needed = Math.max(1, needed);
        this.lastStrokeTick = lastStrokeTick;
        if (this.current.isEmpty()) {
            this.strokes = 0;
        }
    }

    /** Everything still inside (queue and current block), for dropping when the machine is broken. */
    public List<String> contents() {
        List<String> all = new ArrayList<>();
        if (!current.isEmpty()) {
            all.add(current);
        }
        all.addAll(queue);
        return all;
    }

    public void clear() {
        queue.clear();
        current = "";
        strokes = 0;
        needed = 1;
    }
}
