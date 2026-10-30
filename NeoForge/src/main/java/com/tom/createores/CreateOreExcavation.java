package com.tom.createores;

import java.util.function.Supplier;

import org.slf4j.Logger;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tom.createores.client.ClientRegistration;
import com.tom.createores.network.DrillConfigPacket;
import com.tom.createores.network.NetworkHandler;

@Mod(CreateOreExcavation.MODID)
public class CreateOreExcavation {
	public static final String MODID = "createoreexcavation";
	public static final Logger LOGGER = LogUtils.getLogger();

	private static CreateRegistrate registrate;

	private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

	public static final TagKey<Item> DRILL_TAG = TagKey.create(Registries.ITEM, ResourceLocation.tryBuild(MODID, "drills"));

	// Ore veins were removed. Keep the old chunk attachment id so existing worlds load without errors, the data is dropped on save.
	public static final Supplier<AttachmentType<Unit>> LEGACY_ORE_DATA = ATTACHMENT_TYPES.register(
			"ore_vein", () -> AttachmentType.builder(() -> Unit.INSTANCE).serialize(Codec.unit(Unit.INSTANCE), u -> false).build());

	public CreateOreExcavation(ModContainer mc, IEventBus bus) {
		bus.addListener(this::setup);
		bus.addListener(this::doClientStuff);
		bus.addListener(this::registerCapabilities);

		registrate = CreateRegistrate.create(MODID).registerEventListeners(bus);

		bus.register(NetworkHandler.class);

		// Register ourselves for server and other game events we are interested in
		NeoForge.EVENT_BUS.register(this);

		Registration.register();

		ATTACHMENT_TYPES.register(bus);
	}

	private void setup(final FMLCommonSetupEvent event) {
		LOGGER.info("Create Ore Excavation starting");
		event.enqueueWork(DrillConfig::loadFromFileOrLog);
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		event.enqueueWork(ClientRegistration::register);
	}

	@SubscribeEvent
	public void onServerAboutToStart(ServerAboutToStartEvent event) {
		DrillConfig.loadFromFileOrLog();
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			NetworkHandler.sendTo(player, new DrillConfigPacket(DrillConfig.get().getSource()));
	}

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent evt) {
		COECommand.register(evt);
	}

	public static CreateRegistrate registrate() {
		return registrate;
	}

	public static boolean isModLoaded(String id) {
		return ModList.get().isLoaded(id);
	}

	private void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registration.IO_TILE.get(), (be, side) -> be.getCapability(Capabilities.ItemHandler.BLOCK, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Registration.IO_TILE.get(), (be, side) -> be.getCapability(Capabilities.FluidHandler.BLOCK, side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registration.IO_TILE.get(), (be, side) -> be.getCapability(Capabilities.EnergyStorage.BLOCK, side));
	}
}
