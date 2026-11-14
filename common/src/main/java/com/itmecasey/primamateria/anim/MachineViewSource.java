package com.itmecasey.primamateria.anim;

/**
 * Implemented by the block entities of the barrels, crucible, casting tray, drives and auto hammer so one renderer can draw them
 * without knowing the classes. {@link #machineView()} returns the latest synced view record of that block (BarrelView,
 * CrucibleView, TrayView, DriveView or AutoHammerView) from a field, with no allocation; the renderer keeps its per-block state in
 * {@link #renderCache()}.
 */
public interface MachineViewSource {
    Object machineView();

    Object renderCache();

    void renderCache(Object cache);
}
