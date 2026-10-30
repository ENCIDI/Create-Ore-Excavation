package com.tom.createores;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.FMLPaths;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;

/**
 * Drop tables for the Drilling Machine, loaded from config/createoreexcavation-drills.toml.
 * The server's copy is sent to clients for JEI and placement checks.
 */
public class DrillConfig {
	public static final String FILE_NAME = "createoreexcavation-drills.toml";
	private static final String DEFAULT_RESOURCE = "/createoreexcavation-drills-default.toml";

	private static DrillConfig current = new DrillConfig("", List.of(), Map.of(), List.of());
	private static Runnable clientUpdateListener;

	private final String source;
	private final List<Tier> tiers;
	private final Map<ResourceLocation, DimensionTable> dimensions;
	private final List<GroundMatcher> ground;
	private final Map<String, Map<ResourceLocation, List<WeightedDrop>>> rollTables = new HashMap<>();

	private DrillConfig(String source, List<Tier> tiers, Map<ResourceLocation, DimensionTable> dimensions, List<GroundMatcher> ground) {
		this.source = source;
		this.tiers = tiers;
		this.dimensions = dimensions;
		this.ground = ground;
		for (Tier tier : tiers) {
			Map<ResourceLocation, List<WeightedDrop>> perDim = new HashMap<>();
			dimensions.forEach((id, table) -> {
				List<WeightedDrop> drops = new ArrayList<>();
				for (Drop d : table.drops()) {
					int w = d.weights().getOrDefault(tier.name(), 0);
					if (w > 0)drops.add(new WeightedDrop(d, w));
				}
				if (!drops.isEmpty())perDim.put(id, drops);
			});
			rollTables.put(tier.name(), perDim);
		}
	}

	public static DrillConfig get() {
		return current;
	}

	public static Path getPath() {
		return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
	}

	/**
	 * Reloads the config from disk, writing the default file first if it doesn't exist.
	 * On a parse error the previous config stays active and the exception is rethrown.
	 */
	public static DrillConfig loadFromFile() throws IOException, ConfigException {
		Path path = getPath();
		if (!Files.exists(path)) {
			try (InputStream in = DrillConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
				if (in == null)throw new IOException("Missing default drill config in mod jar");
				Files.createDirectories(path.getParent());
				Files.copy(in, path);
			}
		}
		String text = Files.readString(path, StandardCharsets.UTF_8);
		current = parse(text);
		CreateOreExcavation.LOGGER.info("Loaded drill config: {} tiers, {} dimensions", current.tiers.size(), current.dimensions.size());
		return current;
	}

	public static void loadFromFileOrLog() {
		try {
			loadFromFile();
		} catch (Exception e) {
			CreateOreExcavation.LOGGER.error("Failed to load " + FILE_NAME + ", drilling machines will not work", e);
		}
	}

	public static void applyFromServer(String text) {
		try {
			current = parse(text);
		} catch (ConfigException e) {
			CreateOreExcavation.LOGGER.error("Failed to parse drill config received from server", e);
			return;
		}
		if (clientUpdateListener != null)clientUpdateListener.run();
	}

	public static void setClientUpdateListener(Runnable listener) {
		clientUpdateListener = listener;
	}

	public static DrillConfig parse(String text) throws ConfigException {
		UnmodifiableConfig root;
		try {
			root = new TomlParser().parse(text);
		} catch (Exception e) {
			throw new ConfigException("Invalid TOML: " + e.getMessage());
		}

		List<GroundMatcher> ground = new ArrayList<>();
		for (Object o : getList(root, "drillable_ground")) {
			String s = String.valueOf(o);
			if (s.startsWith("#")) {
				ground.add(new GroundMatcher(TagKey.create(Registries.BLOCK, parseId(s.substring(1), "drillable_ground")), null));
			} else {
				ResourceLocation id = parseId(s, "drillable_ground");
				Block b = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
				if (b == null || b == Blocks.AIR) {
					CreateOreExcavation.LOGGER.warn("Drill config: unknown block '{}' in drillable_ground, skipping", s);
					continue;
				}
				ground.add(new GroundMatcher(null, b));
			}
		}

		List<Tier> tiers = new ArrayList<>();
		UnmodifiableConfig tiersCfg = getTable(root, "tiers", true);
		for (Map.Entry<String, Object> e : tiersCfg.valueMap().entrySet()) {
			String name = e.getKey();
			if (!(e.getValue() instanceof UnmodifiableConfig t))throw new ConfigException("tiers." + name + " must be a table");
			ResourceLocation itemId = parseId(getString(t, "item", "tiers." + name), "tiers." + name + ".item");
			Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
			if (item == Items.AIR) {
				CreateOreExcavation.LOGGER.warn("Drill config: unknown item '{}' for tier '{}', skipping tier", itemId, name);
				continue;
			}
			int ticks = getInt(t, "ticks", "tiers." + name, 1);
			int stress = getInt(t, "stress", "tiers." + name, 0);
			tiers.add(new Tier(name, item, ticks, stress));
		}

		Map<ResourceLocation, DimensionTable> dims = new LinkedHashMap<>();
		UnmodifiableConfig dimsCfg = getTable(root, "dimensions", true);
		for (Map.Entry<String, Object> e : dimsCfg.valueMap().entrySet()) {
			String path = "dimensions.\"" + e.getKey() + "\"";
			ResourceLocation dimId = parseId(e.getKey(), path);
			if (!(e.getValue() instanceof UnmodifiableConfig d))throw new ConfigException(path + " must be a table");

			Fluid fluid = null;
			int fluidAmount = 0;
			Object fluidName = d.valueMap().get("fluid");
			if (fluidName != null) {
				ResourceLocation fluidId = parseId(String.valueOf(fluidName), path + ".fluid");
				fluid = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(Fluids.EMPTY);
				if (fluid == Fluids.EMPTY)throw new ConfigException(path + ".fluid: unknown fluid '" + fluidId + "'");
				fluidAmount = getInt(d, "fluid_amount", path, 1);
			}

			List<Drop> drops = new ArrayList<>();
			UnmodifiableConfig dropsCfg = getTable(d, "drops", false);
			if (dropsCfg != null) {
				for (Map.Entry<String, Object> de : dropsCfg.valueMap().entrySet()) {
					String dpath = path + ".drops.\"" + de.getKey() + "\"";
					ResourceLocation itemId = parseId(de.getKey(), dpath);
					Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
					if (item == Items.AIR) {
						CreateOreExcavation.LOGGER.warn("Drill config: unknown item '{}' in {}, skipping", itemId, path);
						continue;
					}
					if (!(de.getValue() instanceof UnmodifiableConfig dc))throw new ConfigException(dpath + " must be an inline table");
					int min = 1, max = 1;
					Map<String, Integer> weights = new HashMap<>();
					for (Map.Entry<String, Object> f : dc.valueMap().entrySet()) {
						switch (f.getKey()) {
						case "min" -> min = toInt(f.getValue(), dpath + ".min");
						case "max" -> max = toInt(f.getValue(), dpath + ".max");
						default -> {
							int w = toInt(f.getValue(), dpath + "." + f.getKey());
							if (w < 0)throw new ConfigException(dpath + "." + f.getKey() + " must not be negative");
							weights.put(f.getKey(), w);
						}
						}
					}
					if (min < 1)throw new ConfigException(dpath + ".min must be at least 1");
					if (max < min)throw new ConfigException(dpath + ".max must be >= min");
					for (String tierName : weights.keySet()) {
						if (tiers.stream().noneMatch(t -> t.name().equals(tierName)))
							CreateOreExcavation.LOGGER.warn("Drill config: {} references unknown tier '{}'", dpath, tierName);
					}
					drops.add(new Drop(item, min, max, Map.copyOf(weights)));
				}
			}
			dims.put(dimId, new DimensionTable(dimId, Optional.ofNullable(fluid), fluidAmount, List.copyOf(drops)));
		}

		return new DrillConfig(text, List.copyOf(tiers), Collections.unmodifiableMap(dims), List.copyOf(ground));
	}

	public String getSource() {
		return source;
	}

	public List<Tier> getTiers() {
		return tiers;
	}

	public Map<ResourceLocation, DimensionTable> getDimensions() {
		return dimensions;
	}

	public Tier getTier(ItemStack stack) {
		if (stack.isEmpty())return null;
		for (Tier t : tiers) {
			if (stack.is(t.item()))return t;
		}
		return null;
	}

	public DimensionTable getDimension(ResourceKey<Level> dim) {
		return dimensions.get(dim.location());
	}

	/**
	 * Weighted drops for the tier in this dimension, empty if the tier can't drill here.
	 */
	public List<WeightedDrop> getDrops(Tier tier, DimensionTable dim) {
		return rollTables.getOrDefault(tier.name(), Map.of()).getOrDefault(dim.id(), List.of());
	}

	public ItemStack roll(Tier tier, DimensionTable dim, RandomSource rng) {
		List<WeightedDrop> drops = getDrops(tier, dim);
		int total = 0;
		for (WeightedDrop d : drops)total += d.weight();
		if (total <= 0)return ItemStack.EMPTY;
		int r = rng.nextInt(total);
		for (WeightedDrop d : drops) {
			r -= d.weight();
			if (r < 0) {
				Drop drop = d.drop();
				return new ItemStack(drop.item(), drop.min() + rng.nextInt(drop.max() - drop.min() + 1));
			}
		}
		return ItemStack.EMPTY;
	}

	public boolean isDrillableGround(BlockState state) {
		for (GroundMatcher m : ground) {
			if (m.matches(state))return true;
		}
		return false;
	}

	/**
	 * The machine stands on a 3x3 footprint; every block in the 5 layers below it must be drillable.
	 * @param controller position of the controller block (top layer, center)
	 */
	public boolean isGroundValid(Level level, BlockPos controller) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int y = 2; y <= 6; y++) {
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					p.set(controller.getX() + x, controller.getY() - y, controller.getZ() + z);
					if (!isDrillableGround(level.getBlockState(p)))return false;
				}
			}
		}
		return true;
	}

	public static record Tier(String name, Item item, int ticks, int stress) {}
	public static record DimensionTable(ResourceLocation id, Optional<Fluid> fluid, int fluidAmount, List<Drop> drops) {}
	public static record Drop(Item item, int min, int max, Map<String, Integer> weights) {}
	public static record WeightedDrop(Drop drop, int weight) {}

	private static record GroundMatcher(TagKey<Block> tag, Block block) {
		public boolean matches(BlockState state) {
			return tag != null ? state.is(tag) : state.is(block);
		}
	}

	public static class ConfigException extends Exception {
		private static final long serialVersionUID = 1L;

		public ConfigException(String msg) {
			super(msg);
		}
	}

	private static ResourceLocation parseId(String s, String path) throws ConfigException {
		ResourceLocation rl = ResourceLocation.tryParse(s);
		if (rl == null)throw new ConfigException(path + ": invalid id '" + s + "'");
		return rl;
	}

	private static UnmodifiableConfig getTable(UnmodifiableConfig cfg, String key, boolean required) throws ConfigException {
		Object o = cfg.valueMap().get(key);
		if (o == null) {
			if (required)throw new ConfigException("Missing table [" + key + "]");
			return null;
		}
		if (o instanceof UnmodifiableConfig c)return c;
		throw new ConfigException(key + " must be a table");
	}

	private static List<?> getList(UnmodifiableConfig cfg, String key) throws ConfigException {
		Object o = cfg.valueMap().get(key);
		if (o == null)throw new ConfigException("Missing " + key);
		if (o instanceof List<?> l)return l;
		throw new ConfigException(key + " must be a list");
	}

	private static String getString(UnmodifiableConfig cfg, String key, String path) throws ConfigException {
		Object o = cfg.valueMap().get(key);
		if (o == null)throw new ConfigException("Missing " + path + "." + key);
		return String.valueOf(o);
	}

	private static int getInt(UnmodifiableConfig cfg, String key, String path, int min) throws ConfigException {
		Object o = cfg.valueMap().get(key);
		if (o == null)throw new ConfigException("Missing " + path + "." + key);
		int v = toInt(o, path + "." + key);
		if (v < min)throw new ConfigException(path + "." + key + " must be at least " + min);
		return v;
	}

	private static int toInt(Object o, String path) throws ConfigException {
		if (o instanceof Number n)return n.intValue();
		throw new ConfigException(path + " must be a number");
	}
}
