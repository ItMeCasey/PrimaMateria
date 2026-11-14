package com.itmecasey.primamateria;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Plays the mod's own sound events from the server (to every player in range). The sieve sounds are played by the client renderer
 * from the synced state, so they are not here: nothing is played twice. Server played: drive.crank, drive.tick,
 * auto_hammer.strike, crucible.pour, casting_tray.cool, barrel.finish.
 */
public final class ModSounds {
    private ModSounds() {
    }

    public static void play(Level level, BlockPos pos, String name, float volume, float pitch) {
        SoundEvent event = ModContent.sound(name);
        if (event != null) {
            level.playSound(null, pos, event, SoundSource.BLOCKS, volume, pitch * (0.95F + level.getRandom().nextFloat() * 0.1F));
        }
    }
}
