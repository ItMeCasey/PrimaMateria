package com.itmecasey.primamateria.neoforge.compat;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.compat.Stacks;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.viewer.Cards;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * JEI: the same categories and cards as BEI, built from the shared {@link Cards}: sifting, crushing, composting, barrel, melting,
 * casting and the drop table pages. Fluids are fluid ingredients; melts are a tinted square. Loaded only when JEI is installed.
 */
@JeiPlugin
public class PrimaJeiPlugin implements IModPlugin {
    private static final Map<Cards.Category, IRecipeType<Cards.Card>> TYPES = new EnumMap<>(Cards.Category.class);

    static IRecipeType<Cards.Card> type(Cards.Category c) {
        return TYPES.computeIfAbsent(c, k -> IRecipeType.create(Identifier.fromNamespaceAndPath(Ids.MODID, k.path), Cards.Card.class));
    }

    static String station(Cards.Category category) {
        return switch (category) {
            case SIFTING, DROPS -> Ids.SIEVE;
            case CRUSHING -> Ids.HAMMERS.get(0);
            case COMPOSTING, BARREL -> Ids.WOODEN_BARREL;
            case MELTING -> Ids.CRUCIBLE;
            case CASTING -> Ids.CASTING_TRAY;
        };
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Ids.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (Cards.Category c : Cards.Category.values()) {
            registration.addRecipeCategories(new CardCategory(c, gui, registration.getJeiHelpers().getPlatformFluidHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Map<Cards.Category, java.util.List<Cards.Card>> byCategory = new EnumMap<>(Cards.Category.class);
        for (Cards.Card card : Cards.all(RecipeLookup.get())) {
            byCategory.computeIfAbsent(card.category(), k -> new java.util.ArrayList<>()).add(card);
        }
        byCategory.forEach((c, cards) -> registration.addRecipes(type(c), cards));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Cards.Category c : Cards.Category.values()) {
            ItemStack station = Stacks.stack(Ids.full(station(c)), 1);
            if (!station.isEmpty()) {
                registration.addCraftingStation(type(c), station);
            }
        }
    }

    /** One JEI category for one {@link Cards.Category}; the card is the recipe object. */
    private static final class CardCategory implements IRecipeCategory<Cards.Card> {
        private final Cards.Category category;
        private final IDrawable icon;
        private final IPlatformFluidHelper<?> fluids;

        CardCategory(Cards.Category category, IGuiHelper gui, IPlatformFluidHelper<?> fluids) {
            this.category = category;
            this.fluids = fluids;
            this.icon = gui.createDrawableItemStack(Stacks.stack(Ids.full(station(category)), 1));
        }

        @Override
        public IRecipeType<Cards.Card> getRecipeType() {
            return type(category);
        }

        @Override
        public Component getTitle() {
            return Component.translatable(category.titleKey());
        }

        @Override
        public int getWidth() {
            return category.width;
        }

        @Override
        public int getHeight() {
            return category.height;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Cards.Card card, IFocusGroup focuses) {
            Cards.Layout layout = Cards.layout(card);
            for (int i = 0; i < card.inputs().size(); i++) {
                Cards.Entry e = card.inputs().get(i);
                Cards.Spot p = layout.inputs().get(i);
                fill(builder.addInputSlot(p.x() + 1, p.y() + 1), e);
            }
            if (!card.catalysts().isEmpty()) {
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, layout.catalyst().x() + 1,
                        layout.catalyst().y() + 1);
                card.catalysts().forEach(e -> fill(slot, e));
            }
            for (int i = 0; i < card.outputs().size(); i++) {
                Cards.Entry e = card.outputs().get(i).entry();
                Cards.Spot p = layout.outputs().get(i);
                fill(builder.addOutputSlot(p.x() + 1, p.y() + 1), e);
            }
        }

        private <T> void addFluid(IPlatformFluidHelper<T> helper, IRecipeSlotBuilder slot, Fluid fluid, long amount) {
            slot.addIngredients(helper.getFluidIngredientType(), java.util.List.of(helper.create(BuiltInRegistries.FLUID.wrapAsHolder(fluid), amount)));
        }

        private void fill(IRecipeSlotBuilder slot, Cards.Entry e) {
            switch (e.kind()) {
                case ITEM -> slot.addItemStacks(Stacks.alternatives(e.id(), e.count()));
                case FLUID -> {
                    Fluid fluid = Stacks.fluid(e.id());
                    if (fluid != Fluids.EMPTY) {
                        addFluid(fluids, slot, fluid, e.count());
                    }
                }
                case MELT -> {
                    // a melt has no ingredient; draw() paints its square
                }
            }
        }

        @Override
        public void draw(Cards.Card card, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
            Cards.Layout layout = Cards.layout(card);
            Font font = Minecraft.getInstance().font;
            for (int i = 0; i < card.inputs().size(); i++) {
                swatch(g, card.inputs().get(i), layout.inputs().get(i));
            }
            for (int i = 0; i < card.outputs().size(); i++) {
                swatch(g, card.outputs().get(i).entry(), layout.outputs().get(i));
            }
            for (Cards.Text t : layout.texts()) {
                int x = t.centered() ? t.x() - font.width(t.text()) / 2 : t.x();
                g.text(font, t.text(), x, t.y(), 0xFF404040, false);
            }
        }

        private static void swatch(GuiGraphicsExtractor g, Cards.Entry entry, Cards.Spot spot) {
            if (entry.kind() == Cards.Kind.MELT) {
                g.fill(spot.x() + 2, spot.y() + 2, spot.x() + Cards.SLOT - 2, spot.y() + Cards.SLOT - 2,
                        0xFF000000 | Stacks.meltColor(entry.id()));
            }
        }
    }
}
