package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.*;

/** Every cut/aspect enters through ServerPlayerGameMode.useItem, including its returned hand stack. */
@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class OrdinaryGemTests {
    static void point(GameTestHelper h,Aspect aspect){
        var p=OrdinaryPlayTests.player(h);var at=h.absolutePos(new BlockPos(3,2,3));p.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);p.setHealth(2);
        var water=at.west();h.getLevel().setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());var gem=GemItem.create(aspect,0,0,0);int warp=Knowledge.get(p).getWarp(WarpType.TEMPORARY);long day=h.getLevel().getDayTime();
        if(aspect==Aspect.FIRE)h.getLevel().setDayTime(18000);
        try{OrdinaryPlayTests.use(p,gem);}finally{h.getLevel().setDayTime(day);}
        h.assertTrue(gem.isEmpty(),"normal point use did not consume "+aspect);h.assertValueEqual(p.getInventory().countItem(Content.item("gemstone_dust")),1,"point "+aspect+" lost shatter dust through normal item-use return");
        switch(aspect){
            case AIR->h.assertTrue(p.hasEffect(MobEffects.JUMP),"point Aer jump missing");
            case EARTH->h.assertTrue(p.hasEffect(StatusEffects.KNOCKBACK),"point Terra knockback effect missing");
            case FIRE->h.assertTrue(h.getLevel().getBlockState(at.above(3)).is(Content.block("glimmer_of_light")),"point Ignis temporary light missing in darkness");
            case WATER->h.assertTrue(h.getLevel().getBlockState(water).is(Blocks.ICE),"point Aqua did not freeze nearby water");
            case ORDER->h.assertValueEqual(p.getHealth(),8F,"point Ordo heal changed");
            case ENTROPY->h.assertValueEqual(Knowledge.get(p).getWarp(WarpType.TEMPORARY),warp+3,"point Perditio temporary warp missing");
        }
        h.succeed();
    }
    static void oval(GameTestHelper h,Aspect aspect){
        var p=OrdinaryPlayTests.player(h);var target=h.absolutePos(new BlockPos(3,2,1));h.getLevel().setBlockAndUpdate(target,(aspect==Aspect.EARTH?Blocks.DIAMOND_ORE:Blocks.STONE).defaultBlockState());
        BlockPos destination=null,node=null;
        if(aspect==Aspect.ORDER){node=h.absolutePos(new BlockPos(5,3,5));h.getLevel().setBlockAndUpdate(node,ModBlocks.AURA_NODE.get().defaultBlockState());((thaumcraft.node.NodeBlockEntity)h.getLevel().getBlockEntity(node)).setAspects(new AspectList().add(Aspect.AIR,9));}
        if(aspect==Aspect.ENTROPY){
            h.getLevel().removeBlock(target,false);var scene=new PumpMatrixScene(h.getLevel(),h.absolutePos(BlockPos.ZERO));
            scene.place(p,new ItemStack(ModBlocks.ESSENTIA_MIRROR.get()),target,Direction.SOUTH);
            GameplayTests.click(p,new ItemStack(ModBlocks.ESSENTIA_MIRROR.get()),target,Direction.SOUTH);
            var bound=p.getInventory().items.stream().filter(s->thaumcraft.device.MirrorKeys.LINK.get(s)!=null).findFirst().orElseThrow().copy();
            var partner=h.absolutePos(new BlockPos(5,2,5));scene.place(p,bound,partner,Direction.SOUTH);destination=partner.south();
        }
        p.moveTo(target.getX()+.5,target.getY()-1,target.getZ()+3.5,180,0);p.setYHeadRot(180);
        var ray=(net.minecraft.world.phys.BlockHitResult)p.pick(6,1,false);
        h.assertTrue(ray.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&ray.getBlockPos().equals(target),"oval fixture does not face its target: eye="+p.getEyePosition()+", hit="+ray.getBlockPos()+", face="+ray.getDirection()+", expected="+target);
        var placement=ray.getBlockPos().relative(ray.getDirection());var gem=GemItem.create(aspect,1,0,0);OrdinaryPlayTests.use(p,gem);
        h.assertTrue(p.getMainHandItem()==gem&&!gem.isEmpty(),"oval "+aspect+" disappeared through normal item use");h.assertValueEqual(Data.get(gem).getInt("Spent"),GemItem.cost(aspect,1),"oval "+aspect+" did not pay its original charge");
        switch(aspect){
            case AIR->h.assertTrue(p.getDeltaMovement().lengthSqr()>=.99,"oval Aer dash missing");
            case EARTH->h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.DIAMOND_ORE),"oval Terra destroyed marked ore");
            case FIRE->h.assertTrue(h.getLevel().getBlockState(placement).is(Blocks.FIRE),"oval Ignis did not use actual raytrace face "+ray.getDirection());
            case WATER->h.assertTrue(h.getLevel().getBlockState(placement).is(Blocks.WATER),"oval Aqua did not use actual raytrace face "+ray.getDirection());
            case ORDER->h.assertTrue(p.getLookAngle().dot(Vec3.atCenterOf(node).subtract(p.getEyePosition()).normalize())>.99,"oval Ordo did not orient toward actual node");
            case ENTROPY->h.assertTrue(p.position().distanceToSqr(Vec3.atBottomCenterOf(destination))<.01,"oval Perditio did not traverse item-bound mirrors");
        }
        h.succeed();
    }
    @GameTest(template="test/platform") public static void pointAirNormalUse(GameTestHelper h){point(h,Aspect.AIR);}
    @GameTest(template="test/platform") public static void pointEarthNormalUse(GameTestHelper h){point(h,Aspect.EARTH);}
    @GameTest(template="test/platform",batch="gem_night") public static void pointFireNormalUse(GameTestHelper h){h.getLevel().setDayTime(18000);h.runAfterDelay(3,()->point(h,Aspect.FIRE));}
    @GameTest(template="test/platform") public static void pointWaterNormalUse(GameTestHelper h){point(h,Aspect.WATER);}
    @GameTest(template="test/platform") public static void pointOrderNormalUse(GameTestHelper h){point(h,Aspect.ORDER);}
    @GameTest(template="test/platform") public static void pointEntropyNormalUse(GameTestHelper h){point(h,Aspect.ENTROPY);}
    @GameTest(template="test/platform") public static void ovalAirNormalUse(GameTestHelper h){oval(h,Aspect.AIR);}
    @GameTest(template="test/platform") public static void ovalEarthNormalUse(GameTestHelper h){oval(h,Aspect.EARTH);}
    @GameTest(template="test/platform") public static void ovalFireNormalUse(GameTestHelper h){oval(h,Aspect.FIRE);}
    @GameTest(template="test/platform") public static void ovalWaterNormalUse(GameTestHelper h){oval(h,Aspect.WATER);}
    @GameTest(template="test/platform") public static void ovalOrderNormalUse(GameTestHelper h){oval(h,Aspect.ORDER);}
    @GameTest(template="test/platform") public static void ovalEntropyNormalUse(GameTestHelper h){oval(h,Aspect.ENTROPY);}
}
