package witchinggadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class PumpMatrixTests {
    static PumpMatrixScene build(GameTestHelper h,boolean closed){var scene=new PumpMatrixScene(h.getLevel(),h.absolutePos(BlockPos.ZERO));var player=GameplayTests.player(h);scene.build(player,closed);scene.start(player);return scene;}
    @GameTest(template="test/pump_matrix",batch="pump_mirror_cache",timeoutTicks=100) public static void exhaustedAspectDoesNotBlockAnotherMirrorRequest(GameTestHelper h){
        var scene=new PumpMatrixScene(h.getLevel(),h.absolutePos(BlockPos.ZERO));scene.build(GameplayTests.player(h),true);
        var mirror=(thaumcraft.device.EssentiaMirrorBlockEntity)h.getLevel().getBlockEntity(scene.mirrors.getFirst());
        var available=scene.aspects.getFirst();var absent=thaumcraft.aspect.Aspect.AIR;
        h.assertTrue(!scene.aspects.contains(absent),"regression needs an absent aspect");
        h.assertTrue(!mirror.takeFromContainer(absent,1),"absent aspect unexpectedly drained");
        h.assertTrue(!mirror.takeFromContainer(available,1),"native shared negative-cache premise changed");
        h.assertTrue(!PumpMirrorAccess.take(mirror,absent),"pump fabricated the absent aspect");
        h.assertTrue(PumpMirrorAccess.take(mirror,available),"absent aspect blocked another pump channel");
        h.assertValueEqual(scene.remaining(),scene.initialEssentia-1,"mirror request did not debit exactly one real unit");
        h.succeed();
    }
    @GameTest(template="test/pump_matrix",batch="pump_matrix",timeoutTicks=2200) public static void nativeInfusionConsumesOnlyPumpedEssentia(GameTestHelper h){
        var scene=build(h,false);h.succeedWhen(()->{
            h.assertTrue(scene.complete(),"native guiding thread infusion has not finished; required="+scene.tile().getAspects().visSize()+", total="+scene.remaining());
            h.assertValueEqual(scene.remaining(),scene.initialEssentia-scene.recipe.aspects().visSize(),"infusion pipeline lost or duplicated essentia");
            h.assertTrue(!scene.tile().isCrafting(),"matrix retained crafting after actual result");
        });
    }
    @GameTest(template="test/pump_matrix",batch="pump_matrix_resume",timeoutTicks=2600) public static void matrixWaitsForClosedPumpLineThenResumes(GameTestHelper h){
        var player=GameplayTests.player(h);var scene=new PumpMatrixScene(h.getLevel(),h.absolutePos(BlockPos.ZERO));scene.build(player,true);scene.start(player);
        h.runAfterDelay(300,()->{
            h.assertTrue(!scene.complete()&&scene.tile().isCrafting(),"matrix completed without pumped supply");
            h.assertValueEqual(scene.tile().getAspects().visSize(),scene.recipe.aspects().visSize(),"matrix bypassed a closed pump line");
            h.assertValueEqual(scene.remaining(),scene.initialEssentia,"closed line lost essentia");scene.open(player);
            h.succeedWhen(()->{h.assertTrue(scene.complete(),"normal valve reopening did not resume infusion");h.assertValueEqual(scene.remaining(),scene.initialEssentia-scene.recipe.aspects().visSize(),"resume duplicated/lost essentia");});
        });
    }
}
