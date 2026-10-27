package witchinggadgets;

import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Actual network right-click, block-entity ticks and menu synchronization in an isolated save. */
final class Port5AuditClient {
    static final BlockPos ROOT=new BlockPos(0,64,0),SAUNA=new BlockPos(0,64,8),TABLE=new BlockPos(2,64,10);
    static int ticks;
    static int picks;
    static volatile boolean formed,smelted,sauna;
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"wg-port5-"+name+".png",mc.getMainRenderTarget(),c->System.out.println("WG_PORT5_SCREENSHOT: "+name));}
    static BlockHitResult hit(){var at=ROOT.west();return new BlockHitResult(Vec3.atCenterOf(at).add(-.5,0,0),Direction.WEST,at,false);}
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static void tick(){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(mc.player==null||mc.level==null||server==null||mc.getOverlay()!=null)return;
        check(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("wg-visual-audit"),"Furnace audit requires wg-visual-audit");
        int now=ticks++;
        if(now==0){mc.setScreen(null);mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.setCameraType(CameraType.FIRST_PERSON);server.execute(()->{
            var l=server.overworld();l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(var entity:l.getEntitiesOfClass(ItemEntity.class,new AABB(-12,60,-12,24,80,14)))entity.discard();
            for(BlockPos at:BlockPos.betweenClosed(-12,63,-8,24,73,14))l.setBlockAndUpdate(at,at.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
            for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++)Port4ReviewClient.BlockStateDummy.put(l,ROOT.offset(x,y,z),x,y,z);
            for(int y=0;y<2;y++){var at=new BlockPos(5,64+y,0);l.setBlockAndUpdate(at.north(),Blocks.STONE_BRICKS.defaultBlockState());var rose=EffectsAuditClient.place(l,at,"rose_vines");rose.progress=5;rose.sync();}
            var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);
            var wand=new ItemStack(Content.tc("wand"));for(var a:GemItem.PRIMALS)thaumcraft.wand.Wands.storeVis(wand,a,1000);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
            thaumcraft.research.ResearchManager.complete(p,"INFERNALBLASTFURNACE",false);p.connection.teleport(-3.5,64,.5,-90,15);
        });}
        // This is the normal client interaction, sent as ServerboundUseItemOnPacket.
        if(now==50){check(mc.player.getMainHandItem().is(Content.tc("wand")),"charged wand not synchronized");mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit());}
        if(now==80){check(mc.screen==null,"wand activation opened a screen");check(mc.level.getBlockEntity(ROOT) instanceof MachineEntity,"formed furnace did not reach client");server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var m=(MachineEntity)server.overworld().getBlockEntity(ROOT);
            check(m!=null&&m.master.equals(ROOT)&&m.facing()==Direction.WEST,"network activation did not save clicked west face");
            check(p.containerMenu==p.inventoryMenu,"formation opened server menu");formed=true;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        });shot("01-survival-wand-formation");}
        if(now==100)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit());
        if(now==110){check(mc.screen==null,"empty-hand use reopened furnace GUI");server.execute(()->server.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.tc("wand"))));}
        if(now==125)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit());
        if(now==140){check(mc.screen==null,"formed furnace wand use reopened GUI");server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.connection.teleport(-4.5,64,4.5,-135,15);
            var input=new ItemEntity(l,.5,67.15,.5,new ItemStack(Content.tc("native_iron_cluster")));input.setDeltaMovement(0,0,0);l.addFreshEntity(input);
        });}
        if(now==200)shot("02-processing-dropped-cluster");
        if(now==650){check(formed&&mc.screen==null,"formation or no-GUI verification failed");server.execute(()->{
            var l=server.overworld();var output=l.getEntitiesOfClass(ItemEntity.class,new AABB(ROOT).inflate(4,3,4),e->e.getItem().is(Items.IRON_INGOT));
            check(output.stream().mapToInt(e->e.getItem().getCount()).sum()==3,"real ticks did not smelt dropped cluster into three ingots");
            check(output.stream().allMatch(e->e.getX()<ROOT.getX()-1),"ingots are on the wrong side of the furnace");
            check(((MachineEntity)l.getBlockEntity(ROOT)).items.getStackInSlot(2).isEmpty(),"result remained in old GUI buffer");smelted=true;
        });shot("03-ingots-ejected-west");}
        if(now==675)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.connection.teleport(.5,64,10.5,180,8);
            var m=EffectsAuditClient.place(l,SAUNA,"sauna_stove");m.water.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,2000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        });
        if(now==740){check(ClientEffects.inSauna()&&ClientEffects.sweat>0,"sauna particles stopped with overlay removal");check(mc.player.hasEffect(thaumcraft.registry.ModEffects.WARP_WARD),"sauna lost protective effect");sauna=true;shot("04-sauna-clear-view-with-particles");}
        // Opening immediately after placement previously risked a null client block entity.
        if(now==755)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var m=EffectsAuditClient.place(l,TABLE,"cutting_table");m.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,3));m.use(p,InteractionHand.MAIN_HAND);
        });
        if(now==790){check(mc.screen instanceof MachineScreen,"immediate machine screen failed to open");check(mc.player.containerMenu instanceof MachineMenu menu&&menu.machine.kind.equals("cutting_table")&&menu.getSlot(0).getItem().getCount()==3,"detached menu model did not synchronize items");shot("05-immediate-menu-synchronization");}
        if(now==805){mc.player.closeContainer();server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(5.5,64,3.5,180,15));}
        if(now>=815&&now<846){long start=System.nanoTime();for(int i=0;i<50;i++){mc.player.pick(6,0,false);picks++;}check(System.nanoTime()-start<500_000_000L,"mature vine selection stalled");}
        if(now==850)shot("06-mature-vines-and-lighting");
        if(now==860){check(formed&&smelted&&sauna&&picks==1550,"incomplete furnace audit");System.out.println("WG_PORT5_CLIENT_OK: survival network wand activation; clicked WEST face persisted; empty hand and wand open no furnace GUI; dropped cluster processed by real ticks; three ingots ejected west; sauna protection and particles without white overlay; immediate cutting-table menu synchronized without client block entity dependency; 1550 mature-vine picks and live lighting updates");mc.stop();}
    }
}
