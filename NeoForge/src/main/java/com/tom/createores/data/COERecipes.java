package com.tom.createores.data;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import com.tom.createores.CreateOreExcavation;
import com.tom.createores.Registration;

public class COERecipes extends RecipeProvider {
	public COERecipes(PackOutput output, CompletableFuture<Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void buildRecipes(RecipeOutput consumer) {
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.NORMAL_DRILL_ITEM.get())
		.pattern("bi ")
		.pattern("ibi")
		.pattern(" ii")
		.define('b', Tags.Items.STORAGE_BLOCKS_IRON)
		.define('i', Tags.Items.INGOTS_IRON)
		.group("create")
		.unlockedBy("iron", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(Tags.Items.INGOTS_IRON).build()))
		.save(consumer);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.DIAMOND_DRILL_ITEM.get())
		.pattern("bi ")
		.pattern("idi")
		.pattern(" ii")
		.define('b', Tags.Items.STORAGE_BLOCKS_DIAMOND)
		.define('i', Tags.Items.GEMS_DIAMOND)
		.define('d', Registration.NORMAL_DRILL_ITEM.get())
		.group("create")
		.unlockedBy("diamond", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(Tags.Items.GEMS_DIAMOND).build()))
		.save(consumer);

		netheriteSmithing(consumer, Registration.DIAMOND_DRILL_ITEM.get(), RecipeCategory.MISC, Registration.NETHERITE_DRILL_ITEM.get());

		MechanicalCraftingRecipeBuilder.shapedRecipe(Registration.DRILL_BLOCK.get())
		.patternLine("BbtbB")
		.patternLine("beSeb")
		.patternLine("CmDmF")
		.patternLine("bsssb")
		.patternLine("BbbbB")
		.key('B', TagKey.create(Registries.ITEM, ResourceLocation.parse("c:storage_blocks/brass")))
		.key('b', TagKey.create(Registries.ITEM, ResourceLocation.parse("c:plates/brass")))
		.key('e', AllItems.ELECTRON_TUBE.get())
		.key('S', AllBlocks.SPOUT.get())
		.key('C', AllBlocks.BRASS_CASING.get())
		.key('m', AllItems.PRECISION_MECHANISM.get())
		.key('D', AllBlocks.MECHANICAL_DRILL.get())
		.key('s', AllItems.STURDY_SHEET.get())
		.key('F', AllBlocks.BRASS_TUNNEL.get())
		.key('t', AllBlocks.COPPER_CASING.get())
		.build(consumer);

		processing("redstone_milling", AllRecipeTypes.MILLING, consumer, b -> b.withItemIngredients(Ingredient.of(Registration.RAW_REDSTONE.get())).output(new ItemStack(Items.REDSTONE, 3)).duration(250));
		processing("redstone_crushing", AllRecipeTypes.CRUSHING, consumer, b -> b.withItemIngredients(Ingredient.of(Registration.RAW_REDSTONE.get())).output(new ItemStack(Items.REDSTONE, 4)).duration(250));

		processing("diamond_cutting", AllRecipeTypes.CUTTING, consumer, b -> b.withItemIngredients(Ingredient.of(Registration.RAW_DIAMOND.get())).output(Items.DIAMOND).duration(250));
		processing("emerald_cutting", AllRecipeTypes.CUTTING, consumer, b -> b.withItemIngredients(Ingredient.of(Registration.RAW_EMERALD.get())).output(Items.EMERALD).duration(250));
	}

	@SuppressWarnings("unchecked")
	private static <T extends StandardProcessingRecipe<?>> void processing(String name, AllRecipeTypes type, RecipeOutput consumer, Consumer<StandardProcessingRecipe.Builder<?>> f) {
		ResourceLocation id = i(name);
		StandardProcessingRecipe.Builder<T> b = new StandardProcessingRecipe.Builder<>(((StandardProcessingRecipe.Serializer<T>) type.getSerializer()).factory(), id);
		f.accept(b);
		b.build(consumer);
	}

	private static ResourceLocation i(String name) {
		return ResourceLocation.tryBuild(CreateOreExcavation.MODID, name);
	}
}
