package com.itmecasey.primamateria.neoforge.compat;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.compat.Stacks;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.viewer.Cards;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Label;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * REI: the same categories and cards as BEI, built from the shared {@link Cards}: sifting, crushing, composting, barrel, melting,
 * casting and the drop table pages. Fluids are fluid entries; melts are a tinted square. Loaded only when REI is installed.
 */
@REIPluginClient
public class PrimaReiPlugin implements REIClientPlugin {
    private static final int PAD = 5;

    static CategoryIdentifier<CardDisplay> id(Cards.Category c) {
        return CategoryIdentifier.of(Ids.MODID, c.path);
    }

    /** One card as a REI display. It is rebuilt from its id when REI saves or syncs it. */
    public static final class CardDisplay extends BasicDisplay {
        static final DisplaySerializer<CardDisplay> SERIALIZER = DisplaySerializer.of(
                Codec.STRING.<CardDisplay>xmap(CardDisplay::byId, d -> d.card.id()).fieldOf("card"),
                ByteBufCodecs.STRING_UTF8.map(CardDisplay::byId, d -> d.card.id()).<RegistryFriendlyByteBuf>cast());

        final Cards.Card card;

        CardDisplay(Cards.Card card) {
            super(inputs(card), outputs(card), Optional.empty());
            this.card = card;
        }

        static CardDisplay byId(String id) {
            for (Cards.Card c : Cards.all(RecipeLookup.get())) {
                if (c.id().equals(id)) {
                    return new CardDisplay(c);
                }
            }
            throw new IllegalArgumentException("No Prima Materia card " + id);
        }

        private static List<EntryIngredient> inputs(Cards.Card card) {
            List<EntryIngredient> out = new ArrayList<>();
            card.inputs().forEach(e -> out.add(ingredient(e)));
            return out;
        }

        private static List<EntryIngredient> outputs(Cards.Card card) {
            List<EntryIngredient> out = new ArrayList<>();
            card.outputs().forEach(o -> out.add(ingredient(o.entry())));
            return out;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return id(card.category());
        }

        @Override
        public DisplaySerializer<? extends Display> getSerializer() {
            return SERIALIZER;
        }
    }

    static EntryIngredient ingredient(Cards.Entry e) {
        return switch (e.kind()) {
            case ITEM -> {
                List<ItemStack> alternatives = Stacks.alternatives(e.id(), e.count());
                yield alternatives.isEmpty() ? EntryIngredient.empty() : EntryIngredients.ofItemStacks(alternatives);
            }
            case FLUID -> {
                Fluid fluid = Stacks.fluid(e.id());
                yield fluid == Fluids.EMPTY ? EntryIngredient.empty() : EntryIngredients.of(fluid, e.count());
            }
            case MELT -> EntryIngredient.empty();
        };
    }

    static String station(Cards.Category category) {
        return PrimaJeiPlugin.station(category);
    }

    private static final class CardCategory implements DisplayCategory<CardDisplay> {
        private final Cards.Category category;

        CardCategory(Cards.Category category) {
            this.category = category;
        }

        @Override
        public CategoryIdentifier<? extends CardDisplay> getCategoryIdentifier() {
            return id(category);
        }

        @Override
        public Component getTitle() {
            return Component.translatable(category.titleKey());
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(Stacks.stack(Ids.full(station(category)), 1));
        }

        @Override
        public int getDisplayHeight() {
            return category.height + 2 * PAD;
        }

        @Override
        public int getDisplayWidth(CardDisplay display) {
            return category.width + 2 * PAD;
        }

        @Override
        public List<Widget> setupDisplay(CardDisplay display, Rectangle bounds) {
            Cards.Card card = display.card;
            Cards.Layout layout = Cards.layout(card);
            int ox = bounds.x + PAD;
            int oy = bounds.y + PAD;
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            for (int i = 0; i < card.inputs().size(); i++) {
                Cards.Spot p = layout.inputs().get(i);
                widgets.add(slot(ox + p.x(), oy + p.y(), card.inputs().get(i)).markInput());
                widgets.add(swatch(ox + p.x(), oy + p.y(), card.inputs().get(i)));
            }
            if (!card.catalysts().isEmpty()) {
                Slot s = Widgets.createSlot(new Point(ox + layout.catalyst().x() + 1, oy + layout.catalyst().y() + 1)).markInput();
                card.catalysts().forEach(e -> {
                    for (ItemStack stack : Stacks.itemsOf(e)) {
                        s.entry(EntryStacks.of(stack));
                    }
                });
                widgets.add(s);
            }
            for (int i = 0; i < card.outputs().size(); i++) {
                Cards.Spot p = layout.outputs().get(i);
                Cards.Entry e = card.outputs().get(i).entry();
                widgets.add(slot(ox + p.x(), oy + p.y(), e).markOutput());
                widgets.add(swatch(ox + p.x(), oy + p.y(), e));
            }
            for (Cards.Text t : layout.texts()) {
                Label label = Widgets.createLabel(new Point(ox + t.x(), oy + t.y()), Component.literal(t.text())).noShadow();
                widgets.add(t.centered() ? label.centered() : label.leftAligned());
            }
            return widgets;
        }

        private static Slot slot(int x, int y, Cards.Entry e) {
            Slot s = Widgets.createSlot(new Point(x + 1, y + 1));
            EntryIngredient ingredient = ingredient(e);
            for (EntryStack<?> stack : ingredient) {
                s.entry(stack);
            }
            return s;
        }

        private static Widget swatch(int x, int y, Cards.Entry e) {
            return Widgets.createDrawableWidget((g, mx, my, delta) -> {
                if (e.kind() == Cards.Kind.MELT) {
                    g.fill(x + 2, y + 2, x + Cards.SLOT - 2, y + Cards.SLOT - 2, 0xFF000000 | Stacks.meltColor(e.id()));
                }
            });
        }
    }

    /** Opens REI's recipe screen for an item (the development tour calls it by name). */
    public static void tourShow(String itemId, boolean uses) {
        ItemStack stack = Stacks.stack(itemId, 1);
        if (stack.isEmpty()) {
            return;
        }
        me.shedaniel.rei.api.client.view.ViewSearchBuilder b = me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder();
        (uses ? b.addUsagesFor(EntryStacks.of(stack)) : b.addRecipesFor(EntryStacks.of(stack))).open();
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (Cards.Category c : Cards.Category.values()) {
            registry.add(new CardCategory(c));
            ItemStack station = Stacks.stack(Ids.full(station(c)), 1);
            if (!station.isEmpty()) {
                registry.addWorkstations(id(c), EntryStacks.of(station));
            }
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (Cards.Card card : Cards.all(RecipeLookup.get())) {
            registry.add(new CardDisplay(card));
        }
    }
}
