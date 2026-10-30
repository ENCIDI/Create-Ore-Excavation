package com.tom.createores.jei;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

import com.tom.createores.CreateOreExcavation;
import com.tom.createores.DrillConfig;
import com.tom.createores.Registration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

@JeiPlugin
public class JEIHandler implements IModPlugin {
	private static List<DrillDisplay> shown = List.of();

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.tryBuild(CreateOreExcavation.MODID, "jei");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new DrillingCategory());
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		shown = DrillDisplay.create(DrillConfig.get());
		registration.addRecipes(JEIRecipes.DRILLING, shown);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(Registration.DRILL_BLOCK.asStack(), JEIRecipes.DRILLING);
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		// The server sends its config after login or /coe reload, swap the displayed tables
		DrillConfig.setClientUpdateListener(() -> {
			var mngr = runtime.getRecipeManager();
			if (!shown.isEmpty())mngr.hideRecipes(JEIRecipes.DRILLING, shown);
			shown = DrillDisplay.create(DrillConfig.get());
			mngr.addRecipes(JEIRecipes.DRILLING, shown);
		});
	}

	@Override
	public void onRuntimeUnavailable() {
		DrillConfig.setClientUpdateListener(null);
	}
}
