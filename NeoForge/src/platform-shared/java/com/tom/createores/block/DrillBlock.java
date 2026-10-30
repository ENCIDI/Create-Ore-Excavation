package com.tom.createores.block;

import static com.tom.createores.block.MultiblockPart.MultiblockMainPart.MultiblockPartType.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import com.tom.createores.DrillConfig;
import com.tom.createores.Registration;
import com.tom.createores.block.entity.DrillBlockEntity;

public class DrillBlock extends MultiblockController implements IBE<DrillBlockEntity> {
	public static final MapCodec<DrillBlock> CODEC = simpleCodec(DrillBlock::new);

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	private static final MultiblockPartType[][][] LAYOUT = new MultiblockPartType[][][] {
		{
			{BLANK,   FLUID_IN, BLANK},
			{KINETIC, BLANK,    BLANK},
			{BLANK,   BLANK,    BLANK}
		},
		{
			{BLANK, BLANK,    BLANK},
			{BLANK, MAIN,     BLANK},
			{BLANK, ITEM_OUT, BLANK}
		}
	};

	public DrillBlock(Properties pr) {
		super(pr);
	}

	@Override
	public Class<DrillBlockEntity> getBlockEntityClass() {
		return DrillBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DrillBlockEntity> getBlockEntityType() {
		return Registration.DRILL_TILE.get();
	}

	@Override
	public MultiblockPartType[][][] getMultiblockLayout() {
		return LAYOUT;
	}

	@Override
	public Vec3i getStart(Direction facing) {
		return new Vec3i(-1, 0, -1);
	}

	@Override
	public boolean canPlaceAt(BlockPlaceContext context, BlockPos controller) {
		Level level = context.getLevel();
		String error = null;
		if(!DrillConfig.get().isGroundValid(level, controller))error = "chat.coe.place.badGround";
		else if(hasOverlappingDrill(level, controller))error = "chat.coe.place.overlap";
		if(error != null) {
			if(!level.isClientSide && context.getPlayer() != null)
				context.getPlayer().displayClientMessage(Component.translatable(error), true);
			return false;
		}
		return true;
	}

	/**
	 * Drills can't share any column of their 3x3 footprint, at any height.
	 */
	public static boolean hasOverlappingDrill(Level level, BlockPos controller) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for(int x = -2;x <= 2;x++) {
			for(int z = -2;z <= 2;z++) {
				for(int y = level.getMinBuildHeight();y < level.getMaxBuildHeight();y++) {
					p.set(controller.getX() + x, y, controller.getZ() + z);
					if(p.equals(controller))continue;
					if(level.getBlockState(p).getBlock() instanceof DrillBlock)return true;
				}
			}
		}
		return false;
	}

	@Override
	public Direction getBlockRotation(Direction facing, MultiblockPartType part) {
		if(part == KINETIC)return facing.getCounterClockWise();
		return facing;
	}

	@Override
	public ItemInteractionResult onActivate(BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult pHit) {
		return level.getBlockEntity(pos, Registration.DRILL_TILE.get()).map(te -> te.onClick(player, hand)).orElse(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
	}

	@Override
	public InteractionResult onActivate(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult pHit) {
		return level.getBlockEntity(pos, Registration.DRILL_TILE.get()).map(te -> te.onClick(player)).orElse(InteractionResult.PASS);
	}
}
