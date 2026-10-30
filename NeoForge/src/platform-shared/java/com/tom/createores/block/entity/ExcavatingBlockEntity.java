package com.tom.createores.block.entity;

import static net.minecraft.ChatFormatting.GOLD;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import java.util.List;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import com.tom.createores.DrillConfig;
import com.tom.createores.DrillConfig.DimensionTable;
import com.tom.createores.DrillConfig.Tier;
import com.tom.createores.client.ClientUtil;
import com.tom.createores.util.TooltipUtil;

public abstract class ExcavatingBlockEntity extends SmartBlockEntity implements MultiblockCapHandler, IDrill {
	protected float progress;
	protected Kinetic kinetic;
	protected boolean hasRotation;
	protected ItemStack drillStack;
	protected Tier tier;
	protected DimensionTable dimension;
	protected ExcavatorState state = ExcavatorState.NO_DRILL;
	protected boolean completedOneCycle;

	protected ExcavatingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		drillStack = ItemStack.EMPTY;
		setLazyTickRate(20);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if(!hasRotation) {
			TooltipUtil.forGoggles(tooltip, CreateLang.translateDirect("tooltip.speedRequirement")
					.withStyle(GOLD));
			Component hint =
					CreateLang.translateDirect("gui.contraptions.not_fast_enough", I18n.get(getBlockState().getBlock()
							.getDescriptionId()));
			List<Component> cutString = TooltipHelper.cutTextComponent(hint, Palette.GRAY_AND_WHITE);
			for (int i = 0; i < cutString.size(); i++)
				TooltipUtil.forGoggles(tooltip, cutString.get(i).copy());
		}
		if(!drillStack.isEmpty())
			TooltipUtil.forGoggles(tooltip, Component.translatable("info.coe.drill.installed", drillStack.getHoverName()));

		Tier tier = DrillConfig.get().getTier(drillStack);
		if(state == ExcavatorState.NO_ERROR && tier != null) {
			TooltipUtil.forGoggles(tooltip, Component.translatable("info.coe.drill.progress").append(": [").append(ClientUtil.makeProgressBar(progress / tier.ticks())).append("]"));
		}
		if(state != ExcavatorState.NO_ERROR) {
			TooltipUtil.forGoggles(tooltip, Component.translatable("info.coe.drill.err_" + state.name().toLowerCase()));
		}

		return true;
	}

	protected abstract boolean canExtract(DimensionTable dimension);
	protected abstract void onFinished(Tier tier, DimensionTable dimension);

	@Override
	public void tick() {
		super.tick();
		if(!level.isClientSide && state == ExcavatorState.NO_ERROR) {
			if(kinetic != null)kinetic.setStress(tier.stress());
			if(kinetic != null && kinetic.getRotationSpeed() >= SpeedLevel.MEDIUM.getSpeedValue() && canExtract(dimension)) {
				progress += kinetic.getRotationSpeed() / SpeedLevel.MEDIUM.getSpeedValue();
				if(progress >= tier.ticks()) {
					updateState();
					if(state == ExcavatorState.NO_ERROR) {
						onFinished(tier, dimension);
						completedOneCycle = true;
						setChanged();
					}
					progress = 0;
				}
			}
		}
	}

	protected void updateState() {
		DrillConfig cfg = DrillConfig.get();
		tier = cfg.getTier(drillStack);
		dimension = cfg.getDimension(level.dimension());
		ExcavatorState old = state;
		if(tier == null) {
			state = ExcavatorState.NO_DRILL;
		} else if(dimension == null || cfg.getDrops(tier, dimension).isEmpty()) {
			state = ExcavatorState.NO_DIMENSION;
		} else if(!cfg.isGroundValid(level, worldPosition)) {
			state = ExcavatorState.BAD_GROUND;
		} else {
			state = ExcavatorState.NO_ERROR;
		}
		if(state != ExcavatorState.NO_ERROR)progress = 0;
		if(old != state)notifyUpdate();
	}

	@Override
	public void lazyTick() {
		if(!level.isClientSide) {
			updateState();
			notifyUpdate();
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		progress = tag.getFloat("progress");
		drillStack = ItemStack.parseOptional(registries, tag.getCompound("drill"));
		completedOneCycle = tag.getBoolean("finishedOnce");
		if(clientPacket) {
			int st = tag.getByte("state");
			state = st >= 0 && st < ExcavatorState.VALUES.length ? ExcavatorState.VALUES[st] : ExcavatorState.NO_DRILL;
			hasRotation = tag.getBoolean("hasRot");
		}
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putFloat("progress", progress);
		if (!drillStack.isEmpty())tag.put("drill", drillStack.save(registries));
		tag.putBoolean("finishedOnce", completedOneCycle);
		if(clientPacket) {
			tag.putByte("state", (byte) state.ordinal());
			tag.putBoolean("hasRot", kinetic != null && kinetic.getRotationSpeed() >= SpeedLevel.MEDIUM.getSpeedValue());
		}
	}

	@Override
	public void initialize() {
		super.initialize();
		if(!level.isClientSide)updateState();
	}

	@Override
	public ItemStack getDrill() {
		return drillStack;
	}

	@Override
	public BlockPos getBelow() {
		return worldPosition.below(2);
	}

	@Override
	public void addKinetic(Kinetic k) {
		this.kinetic = k;
	}

	@Override
	public void dropInv() {
		dropItemStack(drillStack);
	}

	public ItemInteractionResult onClick(Player player, InteractionHand hand) {
		ItemStack item = player.getItemInHand(hand);
		if(drillStack.isEmpty() && DrillConfig.get().getTier(item) != null) {
			if(!level.isClientSide) {
				drillStack = item.split(1);
				updateState();
				notifyUpdate();
			}
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	public InteractionResult onClick(Player player) {
		if(!drillStack.isEmpty()) {
			if(!level.isClientSide) {
				if(player.addItem(drillStack)) {
					drillStack = ItemStack.EMPTY;
					updateState();
					notifyUpdate();
					return InteractionResult.CONSUME;
				}
				return InteractionResult.SUCCESS;
			} else return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	protected void dropItemStack(ItemStack stackInSlot) {
		Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stackInSlot);
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return AABB.encapsulatingFullBlocks(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 0, 1));
	}

	@Override
	public Direction getFacing() {
		return getBlockState().getValue(HORIZONTAL_FACING);
	}

	@Override
	public boolean shouldRenderRubble() {
		return completedOneCycle || (state == ExcavatorState.NO_ERROR && progress > 0);
	}

	@Override
	public float getYOffset() {
		return 0.8f;
	}

	@Override
	public float getDrillOffset() {
		return isActive() ? 0.2f : 0.0f;
	}

	private boolean isActive() {
		return state == ExcavatorState.NO_ERROR && hasRotation;
	}

	@Override
	public float getRotation() {
		if (isActive()) {
			long ticks = getLevel().getGameTime();
			float rot = (ticks * 20) % 360;
			return rot;
		}
		return 0f;
	}

	@Override
	public float getPrevRotation() {
		if (isActive()) {
			long ticks = getLevel().getGameTime() - 1;
			float rot = (ticks * 20) % 360;
			return rot;
		}
		return 0f;
	}

	@Override
	public boolean shouldRenderShaft() {
		return true;
	}

	public static enum ExcavatorState {
		NO_ERROR,
		NO_DRILL,
		NO_DIMENSION,
		BAD_GROUND,
		;
		public static final ExcavatorState[] VALUES = values();
	}
}
