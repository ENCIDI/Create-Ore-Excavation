package com.tom.createores;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import com.tom.createores.block.DrillBlock;
import com.tom.createores.block.IOBlock;
import com.tom.createores.block.KineticInputBlock;
import com.tom.createores.block.MultiblockBlock;
import com.tom.createores.block.MultiblockPart;
import com.tom.createores.block.entity.DrillBlockEntity;
import com.tom.createores.block.entity.IOBlockEntity;
import com.tom.createores.block.entity.KineticInputBlockEntity;
import com.tom.createores.block.entity.KineticInputVisual;
import com.tom.createores.block.entity.MultiblockBlockEntity;
import com.tom.createores.client.DrillRenderer;
import com.tom.createores.client.KineticInputBlockEntityRenderer;
import com.tom.createores.item.MultiBlockItem;

public class Registration {
	private static final CreateRegistrate REGISTRATE = CreateOreExcavation.registrate();

	public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = REGISTRATE.defaultCreativeTab("create_ore_excavation",
			c -> c.icon(() -> new ItemStack(Registration.DIAMOND_DRILL_ITEM.get()))
			).register();

	public static final BlockEntry<DrillBlock> DRILL_BLOCK = REGISTRATE.block("drilling_machine", DrillBlock::new)
			.initialProperties(SharedProperties::copperMetal)
			.properties(MultiblockPart.props())
			.tag(BlockTags.NEEDS_IRON_TOOL)
			.transform(TagGen.pickaxeOnly())
			.blockstate((ctx, prov) -> prov.horizontalBlock(ctx.getEntry(), prov.models()
					.getExistingFile(prov.modLoc("drill_model"))))
			.item(MultiBlockItem::new)
			.properties(p -> p.stacksTo(1))
			.transform(b -> b.model((c, p) -> {
				p.withExistingParent("drilling_machine",
						p.modLoc("block/drill_model"));
			}).build())
			.lang("Drilling Machine")
			.register();

	public static final BlockEntityEntry<DrillBlockEntity> DRILL_TILE = REGISTRATE
			.blockEntity("drill", DrillBlockEntity::new)
			.validBlocks(DRILL_BLOCK)
			.renderer(() -> DrillRenderer::new)
			.register();

	public static final BlockEntry<KineticInputBlock> KINETIC_INPUT = REGISTRATE.block("kinetic_input", KineticInputBlock::new)
			.initialProperties(SharedProperties::copperMetal)
			.properties(MultiblockPart.propsGhost())
			//.transform(BlockStressDefaults.setImpact(16))
			.tag(BlockTags.NEEDS_IRON_TOOL)
			.transform(TagGen.pickaxeOnly())
			.blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models().getBuilder("kinetic_in").texture("particle", ResourceLocation.tryParse("create:block/brass_casing"))))
			.lang("Multiblock Rotational Input")
			.register();

	public static final BlockEntityEntry<KineticInputBlockEntity> KINETIC_INPUT_TILE = REGISTRATE
			.blockEntity("kinetic_input", KineticInputBlockEntity::new)
			.visual(() -> KineticInputVisual::new)
			.validBlocks(KINETIC_INPUT)
			.renderer(() -> KineticInputBlockEntityRenderer::new)
			.register();

	public static final BlockEntry<MultiblockBlock> GHOST_BLOCK = REGISTRATE.block("multiblock", MultiblockBlock::new)
			.initialProperties(SharedProperties::copperMetal)
			.properties(MultiblockPart.propsGhost())
			.tag(BlockTags.NEEDS_IRON_TOOL)
			.transform(TagGen.pickaxeOnly())
			.blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models().getBuilder("multiblock_ghost").texture("particle", ResourceLocation.tryParse("create:block/brass_casing"))))
			.lang("Multiblock")
			.register();

	public static final BlockEntityEntry<MultiblockBlockEntity> GHOST_TILE = REGISTRATE
			.blockEntity("multiblock", MultiblockBlockEntity::new)
			.validBlocks(GHOST_BLOCK)
			.register();

	public static final BlockEntry<IOBlock> IO_BLOCK = REGISTRATE.block("io_block", IOBlock::new)
			.initialProperties(SharedProperties::copperMetal)
			.properties(MultiblockPart.propsGhost())
			.tag(BlockTags.NEEDS_IRON_TOOL)
			.transform(TagGen.pickaxeOnly())
			.blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models().getBuilder("io_block").texture("particle", ResourceLocation.tryParse("create:block/brass_casing"))))
			.lang("Multiblock IO")
			.register();

	public static final BlockEntityEntry<IOBlockEntity> IO_TILE = REGISTRATE
			.blockEntity("io", IOBlockEntity::new)
			.validBlocks(IO_BLOCK)
			.register();

	public static final ItemEntry<Item> NORMAL_DRILL_ITEM = REGISTRATE
			.item("drill", Item::new)
			.tag(CreateOreExcavation.DRILL_TAG)
			.properties(Registration::tool)
			.lang("Iron Drill")
			.model(item2d("item/normal_drill"))
			.register();

	public static final ItemEntry<Item> DIAMOND_DRILL_ITEM = REGISTRATE
			.item("diamond_drill", Item::new)
			.tag(CreateOreExcavation.DRILL_TAG)
			.properties(Registration::tool)
			.lang("Diamond Drill")
			.model(item2d("item/diamond_drill"))
			.register();

	public static final ItemEntry<Item> NETHERITE_DRILL_ITEM = REGISTRATE
			.item("netherite_drill", Item::new)
			.tag(CreateOreExcavation.DRILL_TAG)
			.properties(Registration::tool)
			.lang("Netherite Drill")
			.model(item2d("item/netherite_drill"))
			.register();

	public static final ItemEntry<Item> RAW_DIAMOND = REGISTRATE
			.item("raw_diamond", Item::new)
			.properties(Registration::item)
			.lang("Raw Diamond")
			.model(item2d("item/raw_diamond"))
			.register();

	public static final ItemEntry<Item> RAW_EMERALD = REGISTRATE
			.item("raw_emerald", Item::new)
			.properties(Registration::item)
			.lang("Raw Emerald")
			.model(item2d("item/raw_emerald"))
			.register();

	public static final ItemEntry<Item> RAW_REDSTONE = REGISTRATE
			.item("raw_redstone", Item::new)
			.properties(Registration::item)
			.lang("Raw Redstone")
			.model(item2d("item/raw_redstone"))
			.register();

	public static void register() {
		add("chat.coe.place.badGround", "The Drilling Machine needs drillable stone under its whole 3x3 area, 5 blocks deep");
		add("chat.coe.place.overlap", "Another Drilling Machine is already working above or below this area");
		add("command.coe.reload.success", "Drill config reloaded: %s tiers, %s dimensions");
		add("command.coe.reload.failed", "Failed to reload the drill config: %s");
		add("info.coe.drill.installed", "Installed drill: %s");
		add("info.coe.drill.progress", "Progress");
		add("info.coe.drill.fluidInfo", "Drilling Fluid:");
		add("info.coe.drill.needsFluid", "Requires %s mB of %s per cycle");
		add("info.coe.drill.err_no_drill", "The machine needs a drill");
		add("info.coe.drill.err_no_dimension", "This drill can't excavate anything in this dimension");
		add("info.coe.drill.err_bad_ground", "The machine needs drillable stone under its whole 3x3 area, 5 blocks deep");
		add("jei.coe.recipe.drilling", "Drilling Machine");
		add("jei.coe.chance", "Chance: %s%%");
		add("jei.coe.amount", "Amount: %s");
		add("jei.coe.page", "Page %s/%s");
		add("tooltip.coe.variableImpact", "Variable Impact");
		add("tooltip.coe.processTime", "Ticks: %s");
		add("tag.item.createoreexcavation.drills", "All Drills");
		add("dimension.minecraft.overworld", "Overworld");
		add("dimension.minecraft.the_nether", "The Nether");
		add("dimension.minecraft.the_end", "The End");
	}

	public static void add(String key, String value) {
		REGISTRATE.addRawLang(key, value);
	}

	private static Item.Properties tool(Item.Properties p) {
		return p.stacksTo(1);
	}

	private static Item.Properties item(Item.Properties p) {
		return p;
	}

	private static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> item2d(String texture) {
		return (c, p) -> p.singleTexture(c.getName(), p.mcLoc("item/generated"), "layer0", p.modLoc(texture));
	}

	private static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> itemTool(String texture) {
		return (c, p) -> p.singleTexture(c.getName(), p.mcLoc("item/handheld"), "layer0", p.modLoc(texture));
	}
}
