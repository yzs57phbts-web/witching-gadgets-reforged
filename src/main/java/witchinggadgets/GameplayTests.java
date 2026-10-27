package witchinggadgets;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;
import thaumcraft.registry.ModBlocks;

/** Placement, interactions and ordinary world ticks. Never calls machine work methods. */
@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class GameplayTests {
    static ServerPlayer player(GameTestHelper h){var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"wg_gameplay"));player.setGameMode(GameType.SURVIVAL);return player;}
    static InteractionResult click(ServerPlayer player,ItemStack held,BlockPos pos,Direction side){
        player.setItemInHand(InteractionHand.MAIN_HAND,held);
        return player.gameMode.useItemOn(player,player.serverLevel(),held,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(side.getStepX()*.5,side.getStepY()*.5,side.getStepZ()*.5),side,pos,false));
    }
    static void place(GameTestHelper h,ServerPlayer player,ItemStack stack,BlockPos absolute){
        var level=h.getLevel();BlockPos support=absolute.below();
        if(level.isEmptyBlock(support))level.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
        player.moveTo(absolute.getX()+3.5,absolute.getY(),absolute.getZ()+3.5,180,85);
        var item=stack.getItem();player.setShiftKeyDown(true);click(player,stack,support,Direction.UP);player.setShiftKeyDown(false);
        h.assertTrue(level.getBlockState(absolute).getBlock().asItem()==item,"ordinary placement failed: "+item+" at "+absolute);
    }
    static void fillJar(GameTestHelper h,ServerPlayer player,BlockPos pos,Aspect aspect){
        var phial=new ItemStack(Content.tc("essentia_phial"));AspectKeys.ASPECTS.set(phial,new AspectList().add(aspect,8));
        click(player,phial,pos,Direction.UP);
        h.assertValueEqual(((thaumcraft.essentia.JarBlockEntity)h.getLevel().getBlockEntity(pos)).getAmount(),8,"phial did not fill jar through normal interaction");
    }
    @GameTest(template="test/platform") public static void placingWhileLookingDownKeepsAllDevicesHorizontal(GameTestHelper h){
        var p=player(h);int index=0;
        for(String kind:List.of("essentia_pump","terrestrial_reformer","spinning_wheel","stone_extruder","cutting_table","label_library")){
            BlockPos pos=h.absolutePos(new BlockPos(1+index%3*2,2,1+index/3*3));index++;
            place(h,p,Content.stack(kind),pos);
            var machine=(MachineEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(machine.facing().getAxis().isHorizontal(),kind+" points vertically after floor placement");
            h.assertTrue(machine.facing()==(kind.equals("essentia_pump")?Direction.NORTH:Direction.SOUTH),kind+" faces opposite the original placement");
        }h.succeed();
    }
    static void biome(GameTestHelper h,String name){
        var p=player(h);var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(3,4,3));
        place(h,p,new ItemStack(ModBlocks.WARDED_JAR.get()),pos.below(2));
        place(h,p,new ItemStack(ModBlocks.ESSENTIA_TUBE.get()),pos.below());
        place(h,p,Content.stack("terrestrial_reformer"),pos);
        place(h,p,Content.stack(name),pos.above());
        var machine=(MachineEntity)level.getBlockEntity(pos);var focus=Reformer.focus(machine);
        h.assertTrue(focus!=null,"placed focus unrecognized");fillJar(h,p,pos.below(2),focus.aspect());
        var baseline=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse(name.equals("focus_plains")?"minecraft:desert":"minecraft:plains")));
        for(int x=-16;x<=16;x+=4)for(int z=-16;z<=16;z+=4)thaumcraft.platform.BiomeEdit.setColumn(level,pos.offset(x,0,z),baseline);
        // The energized node is a world resource created by TC's transducer, not a placeable item.
        var nodePos=pos.east(3);level.setBlockAndUpdate(nodePos,ModBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState());
        ((thaumcraft.node.EnergizedNodeBlockEntity)level.getBlockEntity(nodePos)).setAspects(new AspectList().add(Aspect.EARTH,100));
        h.runAfterDelay(160,()->{
            var jar=(thaumcraft.essentia.JarBlockEntity)level.getBlockEntity(pos.below(2));
            h.assertTrue(jar.getAmount()<8,"normal pipe network never supplied "+focus.aspect()+" to "+name);
            int converted=0;for(int x=-16;x<=16;x+=4)for(int z=-16;z<=16;z+=4)if(thaumcraft.platform.BiomeEdit.getColumnBiome(level,pos.offset(x,0,z)).is(ResourceLocation.parse(focus.biome())))converted++;
            h.assertTrue(converted>0,"real ticks never converted biome for "+name+"; stored="+machine.amount);
            h.succeed();
        });
    }
    @GameTest(template="test/platform",batch="biome_plains",timeoutTicks=220) public static void plainsFocusWithRealPipes(GameTestHelper h){biome(h,"focus_plains");}
    @GameTest(template="test/platform",batch="biome_taiga",timeoutTicks=220) public static void taigaFocusWithRealPipes(GameTestHelper h){biome(h,"focus_cold_taiga");}
    @GameTest(template="test/platform",batch="biome_desert",timeoutTicks=220) public static void desertFocusWithRealPipes(GameTestHelper h){biome(h,"focus_desert");}
    @GameTest(template="test/platform",batch="biome_jungle",timeoutTicks=220) public static void jungleFocusWithRealPipes(GameTestHelper h){biome(h,"focus_jungle");}
    @GameTest(template="test/platform",batch="biome_hell",timeoutTicks=220) public static void hellFocusWithRealPipes(GameTestHelper h){biome(h,"focus_hell");}
    @GameTest(template="test/platform",batch="biome_taint",timeoutTicks=220) public static void taintFocusWithRealPipes(GameTestHelper h){biome(h,"focus_taint");}
    @GameTest(template="test/platform",batch="biome_mushroom",timeoutTicks=220) public static void mushroomFocusWithRealPipes(GameTestHelper h){biome(h,"focus_mushroom");}
    @GameTest(template="test/platform",batch="mirror_pipeline",timeoutTicks=240) public static void linkedMirrorsFeedRealPipeAndReformer(GameTestHelper h){
        var p=player(h);var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,3,3));
        place(h,p,Content.stack("essentia_pump"),pos);
        place(h,p,new ItemStack(ModBlocks.ESSENTIA_TUBE.get()),pos.north());
        place(h,p,Content.stack("terrestrial_reformer"),pos.north().above());place(h,p,Content.stack("focus_desert"),pos.north().above(2));
        var first=pos.south(3);var second=first.east(5);
        for(var at:List.of(first,second)){
            level.setBlockAndUpdate(at.south(),Blocks.STONE.defaultBlockState());
            click(p,new ItemStack(ModBlocks.ESSENTIA_MIRROR.get()),at.south(),Direction.NORTH);
            h.assertTrue(level.getBlockEntity(at) instanceof thaumcraft.device.EssentiaMirrorBlockEntity,"normal mirror placement failed");
        }
        // Bind an actual mirror item, then place that item at the second location.
        level.removeBlock(second,false);p.getInventory().clearContent();click(p,new ItemStack(ModBlocks.ESSENTIA_MIRROR.get()),first,Direction.NORTH);
        var linked=p.getInventory().items.stream().filter(s->!s.isEmpty()&&thaumcraft.device.MirrorKeys.LINK.get(s)!=null).findFirst().orElseThrow();
        click(p,linked,second.south(),Direction.NORTH);
        h.assertTrue(((thaumcraft.device.EssentiaMirrorBlockEntity)level.getBlockEntity(second)).isLinked(),"mirror item did not establish link");
        var jarPos=second.north(2);place(h,p,new ItemStack(ModBlocks.WARDED_JAR.get()),jarPos);fillJar(h,p,jarPos,Aspect.FIRE);
        h.runAfterDelay(180,()->{
            var jar=(thaumcraft.essentia.JarBlockEntity)level.getBlockEntity(jarPos);var sink=(MachineEntity)level.getBlockEntity(pos.north().above());
            h.assertTrue(jar.getAmount()<8,"pump did not draw from normally linked mirrors");
            h.assertTrue(sink.containerContains(Aspect.FIRE)>0,"essentia never crossed pump -> tube -> reformer");
            int total=jar.getAmount()+sink.containerContains(Aspect.FIRE)+((MachineEntity)level.getBlockEntity(pos)).amount+((thaumcraft.essentia.TubeBlockEntity)level.getBlockEntity(pos.north())).getEssentiaAmount(null);
            h.assertValueEqual(total,8,"mirror pipeline lost or duplicated essentia");h.succeed();
        });
    }
    @GameTest(template="test/platform",timeoutTicks=120) public static void wallRedstoneChangesCollisionAndKeepsDisguise(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(3,2,3));place(h,p,Content.stack("ethereal_wall"),pos);var level=h.getLevel();
        h.assertTrue(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty(),"unpowered wall is passable");
        var bricks=new ItemStack(Items.RED_NETHER_BRICKS,3);click(p,bricks,pos,Direction.NORTH);
        h.assertTrue(((MachineEntity)level.getBlockEntity(pos)).original.is(Blocks.RED_NETHER_BRICKS)&&bricks.getCount()==3,"camouflage consumes or fails to copy block");
        level.setBlockAndUpdate(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(10,()->{
            h.assertTrue(level.getBlockState(pos).getCollisionShape(level,pos).isEmpty(),"powered wall retains cached solid collision");
            h.assertTrue(((MachineEntity)level.getBlockEntity(pos)).original.is(Blocks.RED_NETHER_BRICKS),"activation erased disguise");level.removeBlock(pos.west(),false);
            h.runAfterDelay(10,()->{h.assertTrue(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty(),"wall never closes");h.succeed();});
        });
    }
    @GameTest(template="test/platform") public static void switchingFocusPreservesSeparateEssentiaStocks(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(3,2,3));place(h,p,Content.stack("terrestrial_reformer"),pos);place(h,p,Content.stack("focus_desert"),pos.above());
        var m=(MachineEntity)h.getLevel().getBlockEntity(pos);h.assertValueEqual(m.addEssentia(Aspect.FIRE,3,Direction.DOWN),3,"normal bottom input rejected");
        h.getLevel().removeBlock(pos.above(),false);place(h,p,Content.stack("focus_mushroom"),pos.above());
        h.assertTrue(m.getSuctionType(Direction.DOWN)==Aspect.SLIME,"new focus never updates suction");h.assertValueEqual(m.containerContains(Aspect.FIRE),3,"focus swap destroyed previous stock");
        h.assertValueEqual(m.containerContains(Aspect.SLIME),0,"fire transmuted into slime");
        var copy=new MachineEntity(pos,m.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(m.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertValueEqual(copy.containerContains(Aspect.FIRE),3,"saving lost dormant aspect");h.succeed();
    }
}
