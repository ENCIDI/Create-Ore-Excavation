package com.tom.createores.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.tom.createores.CreateOreExcavation;
import com.tom.createores.DrillConfig;

public class DrillConfigPacket implements Packet {
	public static final CustomPacketPayload.Type<DrillConfigPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.tryBuild(CreateOreExcavation.MODID, "drill_config"));
	public static final StreamCodec<FriendlyByteBuf, DrillConfigPacket> STREAM_CODEC = CustomPacketPayload.codec(DrillConfigPacket::toBytes, DrillConfigPacket::new);
	private static final int MAX_LENGTH = 1 << 18;

	private final String config;

	public DrillConfigPacket(String config) {
		this.config = config;
	}

	public DrillConfigPacket(FriendlyByteBuf pb) {
		config = pb.readUtf(MAX_LENGTH);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void toBytes(FriendlyByteBuf pb) {
		pb.writeUtf(config, MAX_LENGTH);
	}

	@Override
	public void handleClient() {
		DrillConfig.applyFromServer(config);
	}

	@Override
	public void handleServer(ServerPlayer p) {
	}
}
