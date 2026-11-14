package com.itmecasey.primamateria.anim;

import com.itmecasey.primamateria.view.SieveView;

/**
 * Implemented by the sieve block entity (workstream A's, or the preview one) so the renderer can read its state without knowing
 * the class. {@link #sieveView()} returns the latest synced state; it must be cheap and must not allocate (return a field).
 * The renderer keeps its per-sieve animation state in {@link #renderCache()}.
 */
public interface SieveViewSource {
    SieveView sieveView();

    Object renderCache();

    void renderCache(Object cache);
}
