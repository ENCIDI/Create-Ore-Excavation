package com.tom.createores.jei;

import static net.minecraft.ChatFormatting.GRAY;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import com.tom.createores.Registration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public class DrillingCategory implements IRecipeCategory<DrillDisplay> {
	private static final int COLUMNS = 9;
	private static final int OUTPUT_Y = 64;
	private static final DecimalFormat PERCENT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
	private final AnimatedBlock block;
	private final IDrawable icon;

	public DrillingCategory() {
		block = new AnimatedBlock(Registration.DRILL_BLOCK.getDefaultState(), 11);
		icon = new DoubleItemIcon(Registration.DRILL_BLOCK::asStack, () -> new ItemStack(Registration.NORMAL_DRILL_ITEM.get()));
	}

	@Override
	public RecipeType<DrillDisplay> getRecipeType() {
		return JEIRecipes.DRILLING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("jei.coe.recipe.drilling");
	}

	@Override
	public int getWidth() {
		return 177;
	}

	@Override
	public int getHeight() {
		return OUTPUT_Y + (DrillDisplay.PER_PAGE / COLUMNS) * 18 + 2;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, DrillDisplay recipe, IFocusGroup focuses) {
		builder
		.addSlot(RecipeIngredientRole.INPUT, 51, 3)
		.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
		.addItemStack(new ItemStack(recipe.tier().item()));

		recipe.fluid().ifPresent(fluid -> {
			builder
			.addSlot(RecipeIngredientRole.INPUT, 51 + 18, 3)
			.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
			.addIngredients(JeiPlatform.FLUID_STACK, JeiPlatform.wrapFluid(List.of(new FluidStack(fluid, recipe.fluidAmount()))))
			.setFluidRenderer(recipe.fluidAmount(), false, 16, 16);
		});

		int xOffset = (getWidth() - COLUMNS * 18) / 2;
		for (int i = 0; i < recipe.drops().size(); i++) {
			DrillDisplay.Chance c = recipe.drops().get(i);
			builder
			.addSlot(RecipeIngredientRole.OUTPUT, xOffset + (i % COLUMNS) * 18 + 1, OUTPUT_Y + (i / COLUMNS) * 18 + 1)
			.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
			.addItemStack(c.stack())
			.addRichTooltipCallback((view, tooltip) -> {
				tooltip.add(Component.translatable("jei.coe.chance", PERCENT.format(c.chance() * 100)).withStyle(GRAY));
				tooltip.add(Component.translatable("jei.coe.amount", c.min() == c.max() ? String.valueOf(c.min()) : c.min() + "-" + c.max()).withStyle(GRAY));
			});
		}
	}

	@Override
	public void draw(DrillDisplay recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX,
			double mouseY) {
		block.draw(graphics, 48, 35);
		Font font = Minecraft.getInstance().font;
		String ns = recipe.dimension().getNamespace(), path = recipe.dimension().getPath();
		Component dim = Component.translatableWithFallback("dimension." + ns + "." + path, recipe.dimension().toString());
		graphics.drawString(font, dim, 95, 8, 0xFF404040, false);
		if (recipe.pages() > 1)
			graphics.drawString(font, Component.translatable("jei.coe.page", recipe.page() + 1, recipe.pages()), 95, 20, 0xFF808080, false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, DrillDisplay recipe, IRecipeSlotsView recipeSlotsView,
			double mouseX, double mouseY) {
		if (mouseX > 40 && mouseX < 80 && mouseY > 25 && mouseY < 60) {
			tooltip.add(Component.translatable("tooltip.coe.processTime", recipe.tier().ticks()));
			boolean hasGoggles = GogglesItem.isWearingGoggles(Minecraft.getInstance().player);
			if (hasGoggles) {
				LangBuilder rpmUnit = CreateLang.translate("generic.unit.rpm");
				tooltip.add(CreateLang.translate("tooltip.stressImpact")
						.style(GRAY)
						.component());

				StressImpact impactId = StressImpact.HIGH;
				LangBuilder builder = CreateLang.builder()
						.add(CreateLang.text(TooltipHelper.makeProgressBar(3, impactId.ordinal() + 1))
								.style(impactId.getAbsoluteColor()));
				tooltip.add(builder.add(CreateLang.number(recipe.tier().stress()))
						.text("x ")
						.add(rpmUnit)
						.component());
			}
		}
	}
}
