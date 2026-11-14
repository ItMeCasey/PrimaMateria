package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModAnim;
import com.itmecasey.primamateria.anim.SieveViewSource;
import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.SieveView;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds a {@link SieveView} for the renderer. For tests and the dev tour it can also play a made-up sift on the client: a load,
 * then one shake every few ticks until the progress reaches 1. It never touches the server, saves nothing and does no gameplay.
 */
public class SievePreviewBlockEntity extends BlockEntity implements SieveViewSource {
    private SieveView view = SieveView.EMPTY;
    private Object cache;

    // the made-up sift
    private long start = -1;
    private String meshId = "";
    private String meshTexture = "";
    private String loadBlock = "";
    private int load;
    private DriveKind drive = DriveKind.HAND;
    private int ticksPerShake = 6;
    private int totalShakes = 40;
    private long seed;
    private List<SieveView.Find> finds = List.of();
    private int lastShakes = -1;

    public SievePreviewBlockEntity(BlockPos pos, BlockState state) {
        super(ModAnim.SIEVE_PREVIEW, pos, state);
    }

    @Override
    public SieveView sieveView() {
        if (start >= 0 && level != null) {
            advance();
        }
        return view;
    }

    @Override
    public Object renderCache() {
        return cache;
    }

    @Override
    public void renderCache(Object cache) {
        this.cache = cache;
    }

    /** Shows a fixed state (no shaking). */
    public void show(SieveView fixed) {
        start = -1;
        view = fixed;
    }

    /** Starts a made-up sift: a load of {@code load} blocks lands, then it shakes {@code shakes} times. */
    public void startSift(String meshId, String meshTexture, String loadBlock, int load, DriveKind drive, int shakes, long seed,
                          List<SieveView.Find> finds) {
        this.meshId = meshId;
        this.meshTexture = meshTexture;
        this.loadBlock = loadBlock;
        this.load = load;
        this.drive = drive;
        this.ticksPerShake = switch (drive) {
            case WINDUP -> 8;
            case FURNACE -> 4;
            case POWERED -> 2;
            default -> 6;
        };
        this.totalShakes = shakes;
        this.seed = seed;
        this.finds = finds;
        this.lastShakes = -1;
        this.start = level == null ? 0 : level.getGameTime();
        view = new SieveView(meshId, meshTexture, loadBlock, load, 0F, 0, 0L, drive, false, seed, finds);
    }

    private void advance() {
        long elapsed = level.getGameTime() - start - 6; // the load plops for 6 ticks before the first shake
        int shakes = elapsed < 0 ? 0 : (int) (elapsed / ticksPerShake) + 1;
        if (shakes == lastShakes) {
            return;
        }
        lastShakes = shakes;
        if (shakes >= totalShakes) {
            view = SieveView.EMPTY;
            start = -1;
            return;
        }
        float progress = Math.min(1F, shakes / (float) totalShakes);
        view = new SieveView(meshId, meshTexture, loadBlock, load, progress, shakes,
                start + 6 + (long) (shakes - 1) * ticksPerShake, drive, false, seed, finds);
    }
}
