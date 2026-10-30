package com.tom.createores.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;

import com.tom.createores.DrillConfig;
import com.tom.createores.DrillConfig.DimensionTable;
import com.tom.createores.DrillConfig.Tier;
import com.tom.createores.util.IOBlockType;
import com.tom.createores.util.QueueInventory;

public class DrillBlockEntity extends ExcavatingBlockEntityImpl {
	private QueueInventory inventory;

	public DrillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new QueueInventory();
	}

	@Override
	public <T> T getCaps(BlockCapability<T, Direction> cap, IOBlockType type) {
		if(type == IOBlockType.ITEM_OUT && cap == Capabilities.ItemHandler.BLOCK) {
			return inventory.asCap();
		}
		return super.getCaps(cap, type);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if(!clientPacket) {
			inventory.load(tag.getList("inv", Tag.TAG_COMPOUND), registries);
		}
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if(!clientPacket) {
			tag.put("inv", inventory.toTag(registries));
		}
	}

	@Override
	public void dropInv() {
		super.dropInv();
		for(int i = 0; i < inventory.getSlots(); ++i) {
			dropItemStack(inventory.getStackInSlot(i));
		}
	}

	@Override
	protected boolean canExtract(DimensionTable dimension) {
		return inventory.hasSpace() && super.canExtract(dimension);
	}

	@Override
	protected void onFinished(Tier tier, DimensionTable dimension) {
		ItemStack drop = DrillConfig.get().roll(tier, dimension, level.random);
		if(!drop.isEmpty())inventory.add(drop);
		super.onFinished(tier, dimension);
	}
}
