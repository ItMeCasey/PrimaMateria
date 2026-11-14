package com.itmecasey.primamateria.compat;

import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.CardPainter;
import com.itmecasey.bei.api.CustomRecipe;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.ICategoryRegistration;
import com.itmecasey.bei.api.IRecipeRegistration;
import com.itmecasey.bei.api.RecipeCategory;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.viewer.Cards;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * BEI (Barely Enough Items): the recipe categories of Prima Materia (sifting, crushing, composting, barrel, melting, casting) and
 * the drop table pages. Every card comes from {@link Cards}, so it looks the same in every viewer. Fluids show as their bucket and
 * melts as a tinted square, since BEI shows items. Loaded only when BEI is installed.
 */
@BeiPlugin
public class PrimaBeiPlugin implements IBeiPlugin {
    private static final Map<Cards.Category, RecipeCategory> CATEGORIES = new EnumMap<>(Cards.Category.class);

    private static Cards.Card card(CustomRecipe recipe) {
        return (Cards.Card) recipe.data();
    }


    /** The card painter: values under the slots, and a tinted square where a melt sits. */
    private static final CardPainter PAINTER = new CardPainter() {
        @Override
        public void paint(GuiGraphicsExtractor g, CustomRecipe recipe, int mx, int my) {
            Cards.Card card = card(recipe);
            Cards.Layout layout = Cards.layout(card);
            Font font = Minecraft.getInstance().font;
            for (Cards.Text t : layout.texts()) {
                int x = t.centered() ? t.x() - font.width(t.text()) / 2 : t.x();
                g.text(font, t.text(), x, t.y(), 0xFFE0E0E0, false);
            }
            for (int i = 0; i < card.inputs().size(); i++) {
                swatch(g, card.inputs().get(i), layout.inputs().get(i));
            }
            for (int i = 0; i < card.outputs().size(); i++) {
                swatch(g, card.outputs().get(i).entry(), layout.outputs().get(i));
            }
        }

        private void swatch(GuiGraphicsExtractor g, Cards.Entry entry, Cards.Spot spot) {
            if (entry.kind() == Cards.Kind.MELT) {
                int color = 0xFF000000 | Stacks.meltColor(entry.id());
                g.fill(spot.x() + 1, spot.y() + 1, spot.x() + Cards.SLOT - 1, spot.y() + Cards.SLOT - 1, color);
            }
        }
    };

    private static RecipeCategory category(Cards.Category c) {
        return CATEGORIES.computeIfAbsent(c, key -> {
            RecipeCategory.Builder b = RecipeCategory.builder(Identifier.fromNamespaceAndPath(Ids.MODID, key.path))
                    .title(Component.translatable(key.titleKey()))
                    .station(Stacks.stack(Ids.full(key.station()), 1))
                    .size(key.width, key.height)
                    .painter(PAINTER);
            // Slot positions of a representative card of this category: inputs, then the catalyst (also an input), then outputs.
            Cards.Card sample = sample(key);
            Cards.Layout l = Cards.layout(sample);
            l.inputs().forEach(s -> b.input(s.x(), s.y()));
            b.input(l.catalyst().x(), l.catalyst().y());
            l.outputs().forEach(s -> b.output(s.x(), s.y()));
            return b.build();
        });
    }

    /** A card with the most slots a card of the category can have, so the category has a slot for each. */
    private static Cards.Card sample(Cards.Category key) {
        int inputs = key == Cards.Category.BARREL ? 2 : 1;
        int outputs = switch (key) {
            case SIFTING, CRUSHING -> Cards.MAX_GRID_OUTPUTS;
            case DROPS -> Cards.DROP_ROWS;
            default -> 1;
        };
        List<Cards.Entry> in = java.util.Collections.nCopies(inputs, Cards.Entry.item(Ids.full(Ids.SIEVE), 1));
        List<Cards.Output> out = java.util.Collections.nCopies(outputs,
                new Cards.Output(Cards.Entry.item(Ids.full(Ids.SIEVE), 1), "", ""));
        return new Cards.Card(key, "sample", in, List.of(), out, List.of(), List.of());
    }

    @Override
    public void registerCategories(ICategoryRegistration registration) {
        for (Cards.Category c : Cards.Category.values()) {
            registration.register(category(c));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Cards.Card card : Cards.all(RecipeLookup.get())) {
            RecipeCategory category = category(card.category());
            CustomRecipe.Builder b = CustomRecipe.builder().id(card.id()).data(card);
            for (Cards.Entry in : card.inputs()) {
                List<ItemStack> items = Stacks.itemsOf(in);
                if (items.isEmpty()) {
                    b.emptyInput();
                } else {
                    b.input(items);
                }
            }
            List<ItemStack> catalysts = new java.util.ArrayList<>();
            card.catalysts().forEach(e -> catalysts.addAll(Stacks.itemsOf(e)));
            if (catalysts.isEmpty()) {
                b.emptyInput();
            } else {
                b.input(catalysts);
            }
            boolean any = false;
            for (Cards.Output out : card.outputs()) {
                List<ItemStack> items = Stacks.itemsOf(out.entry());
                if (items.isEmpty()) {
                    b.output(ItemStack.EMPTY);
                } else {
                    b.output(items.get(0));
                    any = true;
                }
            }
            try {
                registration.add(category, b.build());
            } catch (RuntimeException e) {
                System.err.println("[PrimaMateria] BEI card " + card.id() + " skipped: " + e);
            }
        }
    }
}
