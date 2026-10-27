package witchinggadgets;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class EffectsGameTests {
    static List<PlayLevelSoundEvent> hear(GameTestHelper h,Runnable action){var sounds=new ArrayList<PlayLevelSoundEvent>();Consumer<PlayLevelSoundEvent> listener=e->{if(e.getLevel()==h.getLevel())sounds.add(e);};NeoForge.EVENT_BUS.addListener(listener);try{action.run();}finally{NeoForge.EVENT_BUS.unregister(listener);}return sounds;}
    static long count(List<PlayLevelSoundEvent> sounds,SoundEvent sound){return sounds.stream().filter(e->e.getSound()!=null&&e.getSound().value()==sound).count();}
    @GameTest(template="test/platform") public static void extruderCycleHissesThenBreaksStoneOnce(GameTestHelper h){
        var m=PortGameTests.place(h,"stone_extruder");var sounds=hear(h,()->{for(int i=0;i<40;i++)m.serverTick(h.getLevel());});
        h.assertValueEqual(count(sounds,SoundEvents.FIRE_EXTINGUISH),1L,"extruder hiss missing or duplicated");h.assertValueEqual(count(sounds,SoundEvents.STONE_BREAK),1L,"extruder stone sound missing or duplicated");
        h.assertTrue(sounds.getFirst().getSound().value()==SoundEvents.FIRE_EXTINGUISH,"extruder sound order changed");for(var s:sounds)h.assertTrue(s.getOriginalVolume()==.5F&&s.getOriginalPitch()>1.7F,"legacy extruder parameters lost");h.succeed();
    }
    @GameTest(template="test/platform") public static void saunaPourSoundsOnlyWhenBucketFits(GameTestHelper h){
        var m=PortGameTests.place(h,"sauna_stove");var p=h.makeMockPlayer(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        var sounds=hear(h,()->{for(int i=0;i<5;i++)m.use(p,InteractionHand.MAIN_HAND);});h.assertValueEqual(count(sounds,SoundEvents.GENERIC_SWIM),4L,"failed fifth pour made a success sound");h.assertValueEqual(m.water.getFluidAmount(),4000,"pour changed water accounting");h.succeed();
    }
    @GameTest(template="test/platform") public static void furnaceDismantleSoundsOnceDespiteRecursiveRemoval(GameTestHelper h){
        var m=IntegrationGameTests.form(h);var sounds=hear(h,()->h.getLevel().removeBlock(m.getBlockPos(),false));h.assertValueEqual(count(sounds,SoundEvents.FIRE_EXTINGUISH),0L,"dismantling incorrectly plays the rejected-item sound");h.succeed();
    }
    @GameTest(template="test/platform") public static void wandRotationRetainsOriginalToolSound(GameTestHelper h){
        var m=PortGameTests.place(h,"label_library");var p=h.makeMockPlayer(GameType.CREATIVE);p.setShiftKeyDown(true);var sounds=hear(h,()->((MachineBlock)m.getBlockState().getBlock()).onWandUseOn(h.getLevel(),m.getBlockPos(),p,new ItemStack(Content.tc("wand")),net.minecraft.core.Direction.EAST));
        h.assertValueEqual(count(sounds,thaumcraft.registry.ModSounds.TOOL.get()),1L,"wand tool sound missing");h.assertTrue(m.facing()==net.minecraft.core.Direction.EAST,"wand failed to rotate device");h.succeed();
    }
}
