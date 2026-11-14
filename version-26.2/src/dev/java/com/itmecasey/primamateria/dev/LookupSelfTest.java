package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.view.ViewProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server checks of the recipe lookup that viewers, SMWTI, Jade and scripts read, and of the block entities' view access. */
final class LookupSelfTest {
    private LookupSelfTest() {
    }

    static void run(MinecraftServer server, ServerLevel level) {
        RecipeLookup.Snapshot s = RecipeLookup.get();
        SelfTest.verdict("lookup_has_every_sifting_recipe", s.sifting().size() >= 58 && s.residue().size() >= 2,
                s.sifting().size() + " sifting, " + s.residue().size() + " residue");
        SelfTest.verdict("lookup_has_crushing_and_barrel_recipes", s.crushing().size() >= 20 && s.soaking().size() >= 3 && s.mixing().size() >= 2
                && s.steeping().size() >= 1 && s.growing().size() >= 5 && s.composting().size() >= 1,
                s.crushing().size() + " crushing, " + s.soaking().size() + " soaking, " + s.mixing().size() + " mixing, "
                        + s.steeping().size() + " steeping, " + s.growing().size() + " growing, " + s.composting().size() + " composting");
        SelfTest.verdict("lookup_has_crucible_recipes", s.melting().size() >= 13 && s.casting().size() >= 3,
                s.melting().size() + " melting, " + s.casting().size() + " casting");
        SelfTest.verdict("lookup_has_meshes_heat_and_compost", s.meshes().size() == 5 && s.heat().size() >= 8 && s.compost().size() >= 15,
                s.meshes().size() + " meshes, " + s.heat().size() + " heat, " + s.compost().size() + " compost");
        RecipeLookup.Sifting iron = s.sifting().stream().filter(r -> r.result().equals("primamateria:iron_dust") && r.meshMin() == 3)
                .findFirst().orElse(null);
        SelfTest.verdict("lookup_sifting_fields_survive_the_round_trip", iron != null && iron.input().equals("minecraft:gravel")
                && iron.min() == 1 && iron.max() == 2 && Math.abs(iron.chance() - 0.16) < 1e-9,
                String.valueOf(iron));
        RecipeLookup.Heat campfire = s.heat().stream().filter(h -> h.block().equals("minecraft:campfire")).findFirst().orElse(null);
        SelfTest.verdict("lookup_heat_state_is_text", campfire != null && campfire.state().equals("lit=true") && campfire.heat() == 2,
                String.valueOf(campfire));
        RecipeLookup.Crushing leaves = s.crushing().stream().filter(r -> r.input().equals("#minecraft:leaves")).findFirst().orElse(null);
        SelfTest.verdict("lookup_keeps_item_tags", leaves != null && leaves.results().size() == 2, String.valueOf(leaves));

        BlockPos base = SelfTest.area(level, 9);
        String[] blocks = {"sieve", "wooden_barrel", "clay_barrel", "crucible", "casting_tray", "windup_drive", "furnace_drive",
                "powered_drive", "auto_hammer"};
        StringBuilder missing = new StringBuilder();
        int i = 0;
        for (String id : blocks) {
            BlockPos pos = base.offset(2 * i++, 0, 0);
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, ModContent.block(id).defaultBlockState());
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ViewProvider vp) || vp.view() == null) {
                missing.append(id).append(' ');
            }
        }
        SelfTest.verdict("every_block_entity_offers_its_view", missing.length() == 0, missing.length() == 0 ? "all nine" : "missing: " + missing);
    }
}
