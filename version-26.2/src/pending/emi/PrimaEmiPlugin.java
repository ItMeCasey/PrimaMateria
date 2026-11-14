package com.itmecasey.primamateria.compat;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.viewer.Cards;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * EMI: the same categories and cards as BEI, JEI and REI, built from the shared {@link Cards}.
 *
 * <p><b>Not compiled and not tested.</b> There is no EMI build for Minecraft 26.2 or 26.3 yet (checked on CurseForge 2026-11-13: the
 * newest EMI is for 1.21.1), so this file lives in {@code src/pending/emi} and is in no source set. It is written against EMI's
 * published API ({@code EmiPlugin}, {@code EmiRegistry}, {@code EmiRecipe}, {@code WidgetHolder}) and uses the same card layout as the
 * other plugins. When an EMI build for 26.2 or 26.3 exists: move it to {@code src/main/java/.../compat} of a NeoForge layer, add EMI as
 * {@code compileOnly}, fix whatever the API changed, and run the viewer tour. See {@code docs/known-gaps.md}.
 */
@EmiEntrypoint
public class PrimaEmiPlugin implements EmiPlugin {
    private static final Map<Cards.Category, EmiRecipeCategory> CATEGORIES = new EnumMap<>(Cards.Category.class);

    private static EmiRecipeCategory category(Cards.Category c) {
        return CATEGORIES.computeIfAbsent(c, key -> {
            ItemStack station = Stacks.stack(Ids.full(station(key)), 1);
            EmiRenderable icon = EmiStack.of(station);
            return new EmiRecipeCategory(Identifier.fromNamespaceAndPath(Ids.MODID, key.path), icon) {
                @Override
                public Component getName() {
                    return Component.translatable(key.titleKey());
                }
            };
        });
    }

    private static String station(Cards.Category category) {
        return switch (category) {
            case SIFTING, DROPS -> Ids.SIEVE;
            case CRUSHING -> Ids.HAMMERS.get(0);
            case COMPOSTING, BARREL -> Ids.WOODEN_BARREL;
            case MELTING -> Ids.CRUCIBLE;
            case CASTING -> Ids.CASTING_TRAY;
        };
    }

    /** An entry as an EMI ingredient: items and tags as stacks, fluids as fluid stacks; melts have none. */
    private static EmiIngredient ingredient(Cards.Entry e) {
        List<EmiStack> stacks = new ArrayList<>();
        switch (e.kind()) {
            case ITEM -> Stacks.alternatives(e.id(), e.count()).forEach(s -> stacks.add(EmiStack.of(s)));
            case FLUID -> stacks.add(EmiStack.of(Stacks.fluid(e.id()), e.count()));
            case MELT -> {
            }
        }
        return stacks.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(stacks);
    }

    private static final class CardRecipe implements EmiRecipe {
        private final Cards.Card card;

        CardRecipe(Cards.Card card) {
            this.card = card;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return category(card.category());
        }

        @Override
        public Identifier getId() {
            return Identifier.fromNamespaceAndPath(Ids.MODID, "/" + card.id().toLowerCase().replaceAll("[^a-z0-9/._-]", "_"));
        }

        @Override
        public List<EmiIngredient> getInputs() {
            List<EmiIngredient> out = new ArrayList<>();
            card.inputs().forEach(e -> out.add(ingredient(e)));
            return out;
        }

        @Override
        public List<EmiIngredient> getCatalysts() {
            List<EmiStack> stacks = new ArrayList<>();
            card.catalysts().forEach(e -> Stacks.alternatives(e.id(), 1).forEach(s -> stacks.add(EmiStack.of(s))));
            return stacks.isEmpty() ? List.of() : List.of(EmiIngredient.of(stacks));
        }

        @Override
        public List<EmiStack> getOutputs() {
            List<EmiStack> out = new ArrayList<>();
            for (Cards.Output o : card.outputs()) {
                EmiIngredient i = ingredient(o.entry());
                if (!i.isEmpty()) {
                    out.addAll(i.getEmiStacks());
                }
            }
            return out;
        }

        @Override
        public int getDisplayWidth() {
            return card.category().width;
        }

        @Override
        public int getDisplayHeight() {
            return card.category().height;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            Cards.Layout layout = Cards.layout(card);
            for (int i = 0; i < card.inputs().size(); i++) {
                widgets.addSlot(ingredient(card.inputs().get(i)), layout.inputs().get(i).x(), layout.inputs().get(i).y());
            }
            List<EmiIngredient> catalysts = getCatalysts();
            if (!catalysts.isEmpty()) {
                widgets.addSlot(catalysts.get(0), layout.catalyst().x(), layout.catalyst().y()).catalyst(true);
            }
            for (int i = 0; i < card.outputs().size(); i++) {
                Cards.Spot p = layout.outputs().get(i);
                widgets.addSlot(ingredient(card.outputs().get(i).entry()), p.x(), p.y()).recipeContext(this);
            }
            for (Cards.Text t : layout.texts()) {
                widgets.addText(Component.literal(t.text()), t.x(), t.y(), 0xFF404040, false);
            }
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        for (Cards.Category c : Cards.Category.values()) {
            registry.addCategory(category(c));
            ItemStack station = Stacks.stack(Ids.full(station(c)), 1);
            if (!station.isEmpty()) {
                registry.addWorkstation(category(c), EmiStack.of(station));
            }
        }
        for (Cards.Card card : Cards.all(RecipeLookup.get())) {
            registry.addRecipe(new CardRecipe(card));
        }
    }
}
