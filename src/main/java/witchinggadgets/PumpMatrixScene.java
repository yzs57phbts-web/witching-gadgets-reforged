package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;
import thaumcraft.essentia.*;
import thaumcraft.infusion.*;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchManager;

/** Real item interactions and world ticks; remote sources cannot be reached by the matrix itself. */
final class PumpMatrixScene {
    final ServerLevel level;
    final BlockPos origin,matrix,center;
    final InfusionRecipe recipe;
    final List<BlockPos> pumps=new ArrayList<>(),valves=new ArrayList<>(),tubes=new ArrayList<>(),sources=new ArrayList<>(),banks=new ArrayList<>(),mirrors=new ArrayList<>();
    final List<Aspect> aspects;
    int initialEssentia;
    PumpMatrixScene(ServerLevel level,BlockPos origin){
        this.level=level;this.origin=origin;matrix=origin.offset(34,6,12);center=matrix.below(2);
        recipe=(InfusionRecipe)TcRecipes.get("WG:LABYRINTHSTRING");aspects=recipe.aspects().getAspects();
        check(aspects.size()==3,"expected native three-aspect guiding thread recipe");
    }
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static InteractionResult click(ServerPlayer player,ItemStack stack,BlockPos pos,Direction side){
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        return player.gameMode.useItemOn(player,player.serverLevel(),stack,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(side.getStepX()*.5,side.getStepY()*.5,side.getStepZ()*.5),side,pos,false));
    }
    static void near(ServerPlayer player,BlockPos pos){player.moveTo(pos.getX()+2.5,pos.getY(),pos.getZ()+2.5,-90,80);}
    void place(ServerPlayer player,ItemStack stack,BlockPos pos,Direction side){
        var expected=((BlockItem)stack.getItem()).getBlock();var support=pos.relative(side.getOpposite());
        if(level.isEmptyBlock(support))level.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
        near(player,pos);player.setShiftKeyDown(true);click(player,stack,support,side);player.setShiftKeyDown(false);
        check(level.getBlockState(pos).is(expected),"ordinary placement failed: "+expected+" at "+pos);
    }
    void place(ServerPlayer player,net.minecraft.world.level.block.Block block,BlockPos pos){place(player,new ItemStack(block),pos,Direction.UP);}
    void use(ServerPlayer player,ItemStack stack,BlockPos pos){near(player,pos);click(player,stack,pos,Direction.UP);}
    ItemStack wand(){var wand=thaumcraft.wand.Wands.create(thaumcraft.wand.WandRegistry.cap("gold"),thaumcraft.wand.WandRegistry.rod("greatwood"),false);thaumcraft.wand.Wands.fill(wand);return wand;}
    void build(ServerPlayer player,boolean closed){
        player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
        ResearchManager.complete(player,"INFUSION",false);ResearchManager.complete(player,recipe.research(),false);
        for(int i=0;i<aspects.size();i++){
            int z=10+i*2;var aspect=aspects.get(i);var pump=origin.offset(5,4,z);pumps.add(pump);
            place(player,Content.block("essentia_pump"),pump);check(((MachineEntity)level.getBlockEntity(pump)).facing()==Direction.EAST,"pump did not use actual placement heading");
            var valve=pump.east();valves.add(valve);place(player,ModBlocks.ESSENTIA_VALVE.get(),valve);
            if(closed)toggle(player,valve);
            for(int x=7;x<=29;x++){var pos=origin.offset(x,4,z);tubes.add(pos);place(player,ModBlocks.ESSENTIA_TUBE.get(),pos);}
            var bank=origin.offset(29,3,z);banks.add(bank);level.removeBlock(bank,false);place(player,ModBlocks.WARDED_JAR.get(),bank);
            var label=new ItemStack(Content.tc("jar_label"));AspectKeys.ASPECTS.set(label,new AspectList().add(aspect,0));use(player,label,bank);
            check(((JarBlockEntity)level.getBlockEntity(bank)).getAspectFilter()==aspect,"bank did not accept actual jar label");
            var first=origin.offset(2,4,z);mirrors.add(first);place(player,new ItemStack(ModBlocks.ESSENTIA_MIRROR.get()),first,Direction.EAST);
            player.getInventory().clearContent();near(player,first);click(player,new ItemStack(ModBlocks.ESSENTIA_MIRROR.get()),first,Direction.EAST);
            var bound=player.getInventory().items.stream().filter(s->!s.isEmpty()&&thaumcraft.device.MirrorKeys.LINK.get(s)!=null).findFirst().orElseThrow().copy();
            var remote=origin.offset(2,4,21+i*2);mirrors.add(remote);place(player,bound,remote,Direction.EAST);
            check(((thaumcraft.device.EssentiaMirrorBlockEntity)level.getBlockEntity(remote)).isLinked(),"item-bound mirror link failed");
            var source=remote.east(2);sources.add(source);place(player,ModBlocks.WARDED_JAR.get(),source);
            var phial=new ItemStack(Content.tc("essentia_phial"));AspectKeys.ASPECTS.set(phial,new AspectList().add(aspect,8));use(player,phial,source);
            check(((JarBlockEntity)level.getBlockEntity(source)).getAmount()==8,"ordinary phial did not fill source");initialEssentia+=8;
        }
        place(player,ModBlocks.ARCANE_PEDESTAL.get(),center);place(player,ModBlocks.RUNIC_MATRIX.get(),matrix);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            place(player,ModBlocks.ARCANE_STONE_BRICKS.get(),center.offset(x,0,z));place(player,ModBlocks.ARCANE_STONE.get(),center.offset(x,1,z));
        }
        // Remove the temporary placement scaffold, leaving the altar's required central air gap.
        player.gameMode.destroyBlock(matrix.below());
        // Symmetric, normally placed stabilizers prevent random instability from confounding a transport test.
        var candle=ModBlocks.CANDLES.values().iterator().next().get();int pairs=0;
        for(int x=1;x<=6&&pairs<60;x++)for(int z=-6;z<=6&&pairs<60;z++)if(x>=3||Math.abs(z)>=3){
            var one=center.offset(x,0,z);var two=center.offset(-x,0,-z);
            if(!level.isEmptyBlock(one)||!level.isEmptyBlock(two))continue;
            place(player,candle,one);place(player,candle,two);pairs++;
        }
        check(pairs==60,"stabilizer layout overlaps transport");
        check(InfusionTriggers.fits(level,center.offset(-1,0,-1)),"ordinary altar frame does not match the native blueprint");
        near(player,matrix);click(player,wand(),matrix,Direction.SOUTH);
        check(tile().isActive()&&tile().validLocation(),"charged wand did not form actual altar");
        use(player,recipe.central().displayStacks().getFirst().copyWithCount(1),center);
        var ring=List.of(center.west(2),center.east(2),center.north(2),center.south(2));
        for(int i=0;i<recipe.components().size();i++){place(player,ModBlocks.ARCANE_PEDESTAL.get(),ring.get(i));use(player,recipe.components().get(i).displayStacks().getFirst().copyWithCount(1),ring.get(i));}
        check(mirrors.stream().allMatch(p->Math.abs(p.getX()-matrix.getX())>12),"matrix could bypass the pump through a mirror");
        check(sources.stream().allMatch(p->Math.abs(p.getX()-matrix.getX())>12),"matrix could bypass the pump through a source jar");
        check(banks.stream().allMatch(p->((JarBlockEntity)level.getBlockEntity(p)).getAmount()==0),"local bank was seeded instead of pumped");
        player.setItemInHand(InteractionHand.MAIN_HAND,wand());near(player,matrix);
    }
    void start(ServerPlayer player){near(player,matrix);click(player,wand(),matrix,Direction.SOUTH);check(tile().isCrafting(),"wand did not start real infusion");}
    void toggle(ServerPlayer player,BlockPos pos){near(player,pos);click(player,ItemStack.EMPTY,pos,Direction.UP);}
    void open(ServerPlayer player){for(var pos:valves){toggle(player,pos);check(((ValveTubeBlockEntity)level.getBlockEntity(pos)).isAllowFlow(),"ordinary valve click did not open flow");}}
    InfusionMatrixBlockEntity tile(){return (InfusionMatrixBlockEntity)level.getBlockEntity(matrix);}
    boolean complete(){return ((PedestalBlockEntity)level.getBlockEntity(center)).getStack().is(Content.item("guiding_thread"));}
    int remaining(){int count=0;for(var pos:sources)count+=((JarBlockEntity)level.getBlockEntity(pos)).getAmount();for(var pos:banks)count+=((JarBlockEntity)level.getBlockEntity(pos)).getAmount();for(var pos:pumps)count+=((MachineEntity)level.getBlockEntity(pos)).amount;for(var pos:valves)count+=((TubeBlockEntity)level.getBlockEntity(pos)).getEssentiaAmount(null);for(var pos:tubes)count+=((TubeBlockEntity)level.getBlockEntity(pos)).getEssentiaAmount(null);return count;}
}
