package com.tom.createores;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.tom.createores.network.DrillConfigPacket;
import com.tom.createores.network.NetworkHandler;

public class COECommand {

	public static void register(RegisterCommandsEvent evt) {
		LiteralArgumentBuilder<CommandSourceStack> l = Commands.literal("coe");
		l.then(Commands.literal("reload").requires(s -> s.hasPermission(2)).
				executes(c -> {
					DrillConfig cfg;
					try {
						cfg = DrillConfig.loadFromFile();
					} catch (Exception e) {
						CreateOreExcavation.LOGGER.error("Failed to reload " + DrillConfig.FILE_NAME, e);
						c.getSource().sendFailure(Component.translatable("command.coe.reload.failed", String.valueOf(e.getMessage())));
						return 0;
					}
					NetworkHandler.sendToAll(new DrillConfigPacket(cfg.getSource()));
					c.getSource().sendSuccess(() -> Component.translatable("command.coe.reload.success", cfg.getTiers().size(), cfg.getDimensions().size()), true);
					return 1;
				})
				);
		evt.getDispatcher().register(l);
	}
}
