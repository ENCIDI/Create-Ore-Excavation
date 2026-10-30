package com.tom.createores.block.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import com.tom.createores.DrillConfig;
import com.tom.createores.DrillConfig.DimensionTable;
import com.tom.createores.DrillConfig.Tier;
import com.tom.createores.util.IOBlockType;
import com.tom.createores.util.TooltipUtil;

public abstract class ExcavatingBlockEntityImpl extends ExcavatingBlockEntity {
	protected FluidTank fluidTank;

	protected ExcavatingBlockEntityImpl(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		fluidTank = new FluidTank(16000) {

			@Override
			protected void onContentsChanged() {
				notifyUpdate();
			}
		};
	}

	@Override
	public <T> T getCaps(BlockCapability<T, Direction> cap, IOBlockType type) {
		if(type == IOBlockType.FLUID_IN && cap == Capabilities.FluidHandler.BLOCK) {
			return (T) fluidTank;
		}
		return null;
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		fluidTank.readFromNBT(registries, tag.getCompound("tank"));
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("tank", fluidTank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		DimensionTable dim = DrillConfig.get().getDimension(level.dimension());
		if(dim != null && dim.fluid().isPresent()) {
			FluidStack required = new FluidStack(dim.fluid().get(), dim.fluidAmount());
			TooltipUtil.forGoggles(tooltip, Component.translatable("info.coe.drill.fluidInfo"));
			TooltipUtil.forGoggles(tooltip, Component.translatable("info.coe.drill.needsFluid", dim.fluidAmount(), required.getHoverName()));
			containedFluidTooltip(tooltip, isPlayerSneaking, fluidTank);
		}
		return true;
	}

	@Override
	protected boolean canExtract(DimensionTable dimension) {
		return dimension.fluid().isEmpty() ||
				(fluidTank.getFluid().is(dimension.fluid().get()) && fluidTank.getFluidAmount() >= dimension.fluidAmount());
	}

	@Override
	protected void onFinished(Tier tier, DimensionTable dimension) {
		if(dimension.fluid().isPresent())
			fluidTank.drain(dimension.fluidAmount(), FluidAction.EXECUTE);
	}
}
