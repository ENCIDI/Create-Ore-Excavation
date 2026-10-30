package com.tom.createores.client;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import com.tom.createores.Registration;

public class PonderScenes {

	public static void drillingMachine(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("drilling", "Start drilling");
		scene.configureBasePlate(0, 0, 9);
		scene.scaleSceneView(.65f);
		scene.setSceneOffsetY(-1);

		scene.world().showSection(util.select().layer(0), Direction.UP);

		BlockPos ground = util.grid().at(5, 0, 5);
		BlockPos drill = util.grid().at(5, 2, 5);

		scene.overlay().showText(70)
		.attachKeyFrame()
		.text("Place a Drilling Machine on stone, the whole area below it must be stone 5 blocks deep")
		.pointAt(util.vector().blockSurface(ground, Direction.WEST))
		.placeNearTarget();
		scene.idle(20);

		scene.world().showSection(util.select().fromTo(3, 1, 4, 5, 2, 6), Direction.DOWN);
		scene.world().setKineticSpeed(util.select().position(4, 1, 4), 0);
		scene.idle(60);

		scene.overlay().showText(40)
		.text("Put in a Drill")
		.pointAt(util.vector().blockSurface(drill, Direction.WEST))
		.placeNearTarget();
		scene.idle(10);

		scene.overlay().showControls(util.vector().topOf(drill), Pointing.DOWN, 30).rightClick()
		.withItem(new ItemStack(Registration.NORMAL_DRILL_ITEM.get()));
		scene.idle(30);

		scene.overlay().showText(70)
		.attachKeyFrame()
		.text("The machines require Rotational Force to operate")
		.pointAt(util.vector().blockSurface(util.grid().at(5, 0, 2), Direction.WEST))
		.placeNearTarget();
		scene.idle(20);

		scene.world().showSection(util.select().fromTo(4, 1, 0, 4, 1, 3), Direction.DOWN);
		scene.world().showSection(util.select().position(5, 0, 0), Direction.NORTH);
		scene.idle(20);
		scene.world().setKineticSpeed(util.select().position(4, 1, 4), -32);
		scene.idle(40);

		scene.overlay().showText(70)
		.attachKeyFrame()
		.text("Extract the results")
		.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 5), Direction.WEST))
		.placeNearTarget();
		scene.idle(20);

		scene.world().showSection(util.select().fromTo(0, 1, 5, 2, 2, 5), Direction.DOWN);
		scene.world().showSection(util.select().fromTo(2, 1, 6, 2, 1, 10), Direction.DOWN);
		scene.world().showSection(util.select().position(1, 0, 10), Direction.SOUTH);

		scene.idle(40);
		scene.world().flapFunnel(util.grid().at(2, 2, 5), true);
		scene.world().createItemOnBelt(util.grid().at(2, 1, 5), Direction.EAST, new ItemStack(Items.RAW_COPPER));

		scene.idle(60);

		scene.overlay().showText(70)
		.attachKeyFrame()
		.text("Drilling needs fluid: water in the Overworld, lava in the Nether")
		.pointAt(util.vector().blockSurface(util.grid().at(6, 1, 5), Direction.WEST))
		.placeNearTarget();
		scene.idle(10);

		scene.rotateCameraY(70);
		scene.idle(10);

		scene.world().showSection(util.select().fromTo(6, 1, 5, 7, 1, 10), Direction.DOWN);
		scene.world().showSection(util.select().position(6, 0, 10), Direction.SOUTH);

		scene.idle(60);
	}
}
