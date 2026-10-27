package witchinggadgets;

import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Opt-in pump appearance and actual native infusion review in an isolated save. */
final class PumpReviewClient {
    static volatile PumpMatrixScene scene;
    static volatile Throwable failure;
    static boolean started,finished;
    static int ticks,shots,finishTick;
    static Minecraft mc(){return Minecraft.getInstance();}
    static void server(Consumer<ServerPlayer> action){mc().getSingleplayerServer().execute(()->{try{action.accept(mc().getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable error){failure=error;}});}
    static void check(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
    static void held(ServerPlayer p,ItemStack item){p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,item);p.inventoryMenu.broadcastChanges();p.connection.send(new ClientboundSetCarriedItemPacket(0));p.connection.send(new ClientboundContainerSetSlotPacket(-2,0,0,item.copy()));}
    static void view(ServerPlayer p,double x,double y,double z,BlockPos target){var floor=BlockPos.containing(x,y-1,z);if(p.serverLevel().isEmptyBlock(floor))p.serverLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());var d=Vec3.atCenterOf(target).subtract(x,y+p.getEyeHeight(),z);p.connection.teleport(x,y,z,(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    static void click(BlockPos pos){var mc=mc();mc.player.getInventory().selected=0;mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));}
    static void shot(String name){Screenshot.grab(mc().gameDirectory,"wg-port7-"+String.format("%02d",++shots)+"-"+name+".png",mc().getMainRenderTarget(),c->System.out.println("WG_PORT7_SCREENSHOT: "+name));}
    static void tick(){
        var mc=mc();var game=mc.getSingleplayerServer();if(mc.player==null||mc.level==null||game==null||mc.getOverlay()!=null)return;
        check(game.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("wg-pump-audit"),"Pump review requires its isolated save");
        if(failure!=null)throw new IllegalStateException("Pump review failed",failure);
        if(!started){started=true;mc.setScreen(null);mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.particles().set(ParticleStatus.ALL);server(p->{var level=p.serverLevel();level.setDayTime(1000);level.setWeatherParameters(100000,0,false,false);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,game);p.getFoodData().setFoodLevel(20);p.removeAllEffects();for(BlockPos at:BlockPos.betweenClosed(-3,63,-3,60,78,35))level.setBlockAndUpdate(at,at.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());var built=new PumpMatrixScene(level,new BlockPos(0,62,0));built.build(p,true);held(p,ItemStack.EMPTY);view(p,2,67,7,built.pumps.getFirst());scene=built;});return;}
        if(scene==null)return;int now=ticks++;
        if(now==20)shot("intake-cube-and-one-pixel-rim");
        if(now==40)server(p->view(p,7.5,67,7.5,scene.pumps.getFirst()));
        if(now==60)shot("bellows-without-extra-connectors");
        if(now==80)server(p->view(p,1.5,67,10.5,scene.pumps.getFirst()));
        if(now==100)shot("centered-intake-front");
        if(now==120)server(p->{view(p,scene.matrix.getX()+.5,scene.center.getY(),scene.matrix.getZ()+3.5,scene.matrix);held(p,scene.wand());});
        if(now==150){check(mc.player.getMainHandItem().is(Content.tc("wand")),"wand fixture not synchronized");click(scene.matrix);}
        if(now==180)server(p->{check(scene.tile().isCrafting(),"network wand click failed to start native infusion");check(scene.tile().getAspects().visSize()==scene.recipe.aspects().visSize(),"closed lines were bypassed");});
        if(now==220)shot("matrix-waits-for-pump-supply");
        for(int index=0;index<3;index++){final int i=index;
            if(now==240+i*60)server(p->{var valve=scene.valves.get(i);view(p,valve.getX()+2.5,valve.getY(),valve.getZ()-2.5,valve);held(p,ItemStack.EMPTY);});
            if(now==260+i*60)click(scene.valves.get(i));
            if(now==280+i*60)server(p->check(((thaumcraft.essentia.ValveTubeBlockEntity)p.serverLevel().getBlockEntity(scene.valves.get(i))).isAllowFlow(),"ordinary valve click did not open pump channel "+i));
        }
        if(now==430)server(p->view(p,scene.matrix.getX()+5.5,scene.center.getY(),scene.matrix.getZ()+7.5,scene.matrix));
        if(now==470)shot("native-infusion-from-pumped-essentia");
        if(now>480&&!finished)server(p->{if(now%200==0)System.out.println("WG_PORT7_MATRIX_PROGRESS: required="+scene.tile().getAspects().visSize()+"; stored="+scene.remaining()+"; gameTime="+p.serverLevel().getGameTime());if(scene.complete()){check(scene.remaining()==scene.initialEssentia-scene.recipe.aspects().visSize(),"native infusion lost or duplicated essentia");finished=true;System.out.println("WG_PORT7_MATRIX_OK: WG:LABYRINTHSTRING completed through 3 item-bound mirror/pump/tube/bank channels; source total="+scene.initialEssentia+"; consumed="+scene.recipe.aspects().visSize()+"; remaining="+scene.remaining()+"; mirrors and remote jars all outside matrix range");view(p,scene.center.getX()+.5,scene.center.getY()+1,scene.center.getZ()+3.5,scene.center);}});
        if(finished){if(finishTick==0)finishTick=now;if(now==finishTick+15)shot("completed-native-guiding-thread-recipe");if(now>=finishTick+30){System.out.println("WG_PORT7_CLIENT_OK: "+shots+" screenshots; actual network altar activation and valve controls; real infusion result and conserved essentia");mc.stop();}}
        check(now<2400,"native infusion failed to finish within actual world ticks");
    }
}
