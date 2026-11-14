package com.itmecasey.primamateria.view;

/**
 * Implemented by each block entity of the mod (workstream A) so the integrations can read its state: SMWTI and Jade lines, the
 * script bridges. {@link #view()} returns the block entity's view record ({@code SieveView}, {@code BarrelView},
 * {@code CrucibleView}, {@code TrayView}, {@code DriveView} or {@code AutoHammerView}). It is a read of a snapshot: no side
 * effects, and it is safe to call on the client with the synced values.
 */
public interface ViewProvider {
    Object view();
}
