package com.tom.createores.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import com.tom.createores.DrillConfig;
import com.tom.createores.DrillConfig.DimensionTable;
import com.tom.createores.DrillConfig.Tier;
import com.tom.createores.DrillConfig.WeightedDrop;

/**
 * One JEI page: the drops of a drill tier in a dimension.
 * generation keeps pages of an old config unequal to new ones, JEI hides old pages by equality.
 */
public record DrillDisplay(Tier tier, ResourceLocation dimension, Optional<Fluid> fluid, int fluidAmount, List<Chance> drops, int page, int pages, int generation) {
	public static final int PER_PAGE = 27;
	private static int nextGeneration;

	public static record Chance(ItemStack stack, int min, int max, double chance) {}

	public static List<DrillDisplay> create(DrillConfig cfg) {
		List<DrillDisplay> out = new ArrayList<>();
		int generation = nextGeneration++;
		for (Tier tier : cfg.getTiers()) {
			for (DimensionTable dim : cfg.getDimensions().values()) {
				List<WeightedDrop> drops = cfg.getDrops(tier, dim);
				if (drops.isEmpty())continue;
				double total = drops.stream().mapToInt(WeightedDrop::weight).sum();
				List<Chance> chances = drops.stream().
						sorted(Comparator.comparingInt(WeightedDrop::weight).reversed()).
						map(d -> new Chance(new ItemStack(d.drop().item(), d.drop().max()), d.drop().min(), d.drop().max(), d.weight() / total)).
						toList();
				int pages = (chances.size() + PER_PAGE - 1) / PER_PAGE;
				for (int i = 0; i < pages; i++) {
					out.add(new DrillDisplay(tier, dim.id(), dim.fluid(), dim.fluidAmount(),
							chances.subList(i * PER_PAGE, Math.min(chances.size(), (i + 1) * PER_PAGE)), i, pages, generation));
				}
			}
		}
		return out;
	}
}
