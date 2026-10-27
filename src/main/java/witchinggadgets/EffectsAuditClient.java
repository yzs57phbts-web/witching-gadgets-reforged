package witchinggadgets;

import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.aspect.*;
import thaumcraft.registry.ModBlocks;

/** Opt-in runtime regression; mutations restricted to the disposable visual audit save. */
@EventBusSubscriber(modid=WitchingGadgets.ID,value=Dist.CLIENT)
public final class EffectsAuditClient {
    static int ticks;
    static final Set<String> sounds=java.util.concurrent.ConcurrentHashMap.newKeySet();
    static final BlockPos SAUNA=new BlockPos(2,64,0),REFORMER=new BlockPos(5,64,0),EXTRUDER=new BlockPos(-2,64,0),PUMP=new BlockPos(-5,64,0);
    static volatile boolean converted,pumped;
    @SubscribeEvent public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent e){if(Boolean.getBoolean("wg.effectsAudit")){sounds.add(e.getSound().getLocation().toString());System.out.println("WG_PORT4_AUDIO_CHANNEL: "+e.getSound().getLocation());}}
    static MachineEntity place(net.minecraft.server.level.ServerLevel l,BlockPos pos,String kind){l.setBlockAndUpdate(pos,Content.block(kind).defaultBlockState());return (MachineEntity)l.getBlockEntity(pos);}
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"wg-effects-"+name+".png",mc.getMainRenderTarget(),c->System.out.println("WG_PORT4_SCREENSHOT: "+name));}
    static void tick(){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null||mc.level==null||server==null||mc.getOverlay()!=null)return;
        if(!server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("wg-visual-audit"))throw new IllegalStateException("Effects audit requires wg-visual-audit");int now=ticks++;
        if(now>=400){Port4ReviewClient.tick(now-400);return;}
        if(now==0){mc.setScreen(null);mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.particles().set(ParticleStatus.ALL);for(var source:net.minecraft.sounds.SoundSource.values())mc.options.getSoundSourceOptionInstance(source).set(source==net.minecraft.sounds.SoundSource.MUSIC?0D:1D);
            server.execute(()->{var l=server.overworld();l.setDayTime(1000);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);for(var entity:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(-12,60,-12,35,80,12)))entity.discard();for(BlockPos at:BlockPos.betweenClosed(-10,63,-7,12,73,10))l.setBlockAndUpdate(at,at.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                place(l,EXTRUDER,"stone_extruder");place(l,SAUNA,"sauna_stove");place(l,new BlockPos(3,65,-1),"glimmer_of_light");place(l,new BlockPos(0,64,-3),"label_library");
                l.setBlockAndUpdate(new BlockPos(0,65,0),Blocks.STONE_BRICKS.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();for(var slot:net.minecraft.world.entity.EquipmentSlot.values())if(slot.isArmor())p.setItemSlot(slot,ItemStack.EMPTY);p.connection.teleport(.5,64,4.5,180,0);
            });}
        if(now==35)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().add(new ItemStack(Content.tc("mirrored_glass"),2));p.setItemInHand(InteractionHand.MAIN_HAND,Content.stack("scan_camera"));Tools.photo(p);});
        if(now==40)shot("01-camera-runes");
        if(now==60)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();if(!GemItem.effect(p,Aspect.FIRE,1,0))throw new IllegalStateException("fire gem failed");});
        if(now==70)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.serverLevel().setBlockAndUpdate(new BlockPos(0,65,1),Blocks.AIR.defaultBlockState());var m=(MachineEntity)p.serverLevel().getBlockEntity(new BlockPos(0,64,-3));p.setShiftKeyDown(true);((MachineBlock)m.getBlockState().getBlock()).onWandUseOn(p.serverLevel(),m.getBlockPos(),p,new ItemStack(Content.tc("wand")),Direction.WEST);p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));((MachineEntity)p.serverLevel().getBlockEntity(SAUNA)).use(p,InteractionHand.MAIN_HAND);});
        if(now==100)shot("02-sauna-steam-overlay");
        if(now==110)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.serverLevel().setBlockAndUpdate(new BlockPos(0,65,0),Blocks.IRON_ORE.defaultBlockState());GemItem.effect(p,Aspect.EARTH,1,0);var nPos=new BlockPos(3,65,-3);p.serverLevel().setBlockAndUpdate(nPos,ModBlocks.AURA_NODE.get().defaultBlockState());((thaumcraft.node.NodeBlockEntity)p.serverLevel().getBlockEntity(nPos)).setAspects(new AspectList().add(Aspect.ORDER,20).add(Aspect.AIR,10));GemItem.effect(p,Aspect.ORDER,1,0);});
        if(now==118)shot("03-node-and-ore-wisps");
        if(now==140)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var s=Content.stack("guiding_thread");Tools.toggleGuidance(p,s);p.setPos(-1,64,3);p.setYRot(90);Tools.tickGuidance(p,s,true);p.setPos(-3,64,2);p.setYRot(180);Tools.tickGuidance(p,s,true);p.setItemInHand(InteractionHand.MAIN_HAND,s);p.connection.teleport(.5,65,6,180,20);});
        if(now==160)shot("04-guiding-thread-beams");
        if(now==170)server.execute(()->{var l=server.overworld();var m=place(l,REFORMER,"terrestrial_reformer");place(l,REFORMER.above(),"focus_desert");m.amount=4;m.sync();var pos=REFORMER.offset(0,2,3);l.setBlockAndUpdate(pos,ModBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState());var node=(thaumcraft.node.EnergizedNodeBlockEntity)l.getBlockEntity(pos);node.setAspects(new AspectList().add(Aspect.EARTH,2500));node.setupNode();thaumcraft.node.EnergizedNodeBlockEntity.serverTick(l,pos,node.getBlockState(),node);
            var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.connection.teleport(5.5,65,5,180,25);
        });
        if(now==205)server.execute(()->{var m=(MachineEntity)server.overworld().getBlockEntity(REFORMER);converted=m.amount<4;if(!converted)throw new IllegalStateException("reformer never spent essentia");server.overworld().removeBlock(REFORMER.offset(0,2,3),false);});
        if(now==210)shot("05-reformer-vent");
        if(now==220)server.execute(()->{var l=server.overworld();var pump=place(l,PUMP,"essentia_pump");var sink=place(l,PUMP.north(),"terrestrial_reformer");sink.essentia=Aspect.FIRE;place(l,PUMP.north().above(),"focus_desert");
            var a=PUMP.south(4);var b=a.west(2);for(var at:List.of(a,b)){l.setBlockAndUpdate(at.south(),Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(at,ModBlocks.ESSENTIA_MIRROR.get().defaultBlockState().setValue(thaumcraft.device.MirrorBlock.FACING,Direction.NORTH));}
            var mirror=(thaumcraft.device.EssentiaMirrorBlockEntity)l.getBlockEntity(a);mirror.setLinkTarget(GlobalPos.of(l.dimension(),b));mirror.restoreLink(true);
            l.setBlockAndUpdate(b.north(2),ModBlocks.WARDED_JAR.get().defaultBlockState());((thaumcraft.essentia.JarBlockEntity)l.getBlockEntity(b.north(2))).addToContainer(Aspect.FIRE,16);
            var p=server.getPlayerList().getPlayers().getFirst();p.connection.teleport(-2,65,5,130,20);
        });
        if(now==240)server.execute(()->{var m=(MachineEntity)server.overworld().getBlockEntity(PUMP);pumped=m.amount==1;if(!pumped)throw new IllegalStateException("mirror pump failed");});
        if(now==245)shot("06-mirror-essentia-trail");
        if(now==260)server.execute(()->{var l=server.overworld();var at=PUMP.east(2);var state=Content.block("infernal_blast_furnace").defaultBlockState().setValue(FurnaceBlock.PART,22);l.setBlockAndUpdate(at,state);var item=new net.minecraft.world.entity.item.ItemEntity(l,at.getX()+.5,at.getY()+.5,at.getZ()+.5,new ItemStack(Items.DIRT));((FurnaceBlock)state.getBlock()).entityInside(state,l,at,item);});
        if(now==265)shot("07-furnace-break-lava");
        if(now==280)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var l=server.getLevel(Level.NETHER);for(BlockPos at:BlockPos.betweenClosed(-2,63,0,2,68,6))l.setBlockAndUpdate(at,at.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(new BlockPos(0,65,0),Blocks.STONE_BRICKS.defaultBlockState());p.teleportTo(l,.5,64,4.5,Set.of(),180,0);});
        if(now==360)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();if(!GemItem.effect(p,Aspect.WATER,1,0))throw new IllegalStateException("nether water gem failed");});
        if(now==364)shot("08-nether-water-smoke");
        if(now==390){
            for(String key:List.of("thaumcraft:cameraticks","thaumcraft:tool","minecraft:entity.generic.swim","minecraft:block.fire.extinguish","minecraft:block.stone.break","minecraft:item.firecharge.use"))if(!sounds.contains(key))throw new IllegalStateException("No actual sound channel: "+key+" heard="+sounds);
            for(var kind:Effects.Kind.values())if(kind!=Effects.Kind.GEM_RECHARGE&&!ClientEffects.received.containsKey(kind))throw new IllegalStateException("No client effect received: "+kind+" got="+ClientEffects.received);
            if(ClientEffects.beams==0||ClientEffects.sweat==0||ClientEffects.ambientWisps==0||!converted||!pumped)throw new IllegalStateException("Missing continuing particle effect");
            System.out.println("WG_PORT4_EFFECTS_OK: actual sound channels="+sounds+"; effect packets="+ClientEffects.received+"; beams="+ClientEffects.beams+"; sweat="+ClientEffects.sweat+"; wisps="+ClientEffects.ambientWisps+"; real reformer and mirror pump verified");
        }
    }
}
