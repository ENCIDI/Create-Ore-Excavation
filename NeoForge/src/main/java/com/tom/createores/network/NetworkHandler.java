package com.tom.createores.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {

	@SubscribeEvent
	public static void onPayloadRegister(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("2");
		registrar.playToClient(DrillConfigPacket.ID, DrillConfigPacket.STREAM_CODEC, NetworkHandler::handlePacketClient);
	}

	public static void handlePacketClient(Packet packet, IPayloadContext context) {
		context.enqueueWork(packet::handleClient);
	}

	public static void sendTo(ServerPlayer player, Packet packet) {
		PacketDistributor.sendToPlayer(player, packet);
	}

	public static void sendToAll(Packet packet) {
		PacketDistributor.sendToAllPlayers(packet);
	}
}
