package com.tom.createores.jei;

import com.tom.createores.CreateOreExcavation;

import mezz.jei.api.recipe.RecipeType;

public class JEIRecipes {
	public static final RecipeType<DrillDisplay> DRILLING = RecipeType.create(CreateOreExcavation.MODID, "drilling", DrillDisplay.class);
}
