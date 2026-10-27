package witchinggadgets;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.aspect.*;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.registry.ModBlocks;
import thaumcraft.wand.*;
import static witchinggadgets.GameplayAuditClient.*;

/** Read-only TC diagnosis: native item interactions, inventory swaps, renderer observations. No TC patch. */
final class DetailReviewClient {
    static final BlockPos NODE=new BlockPos(0,65,4),PUMP=new BlockPos(0,64,0);
    static int screenshots,wandMainFrames,wandOffFrames,canceledFrames;
    static int beamStart,beamTick;
    static Consumer<net.neoforged.neoforge.client.event.RenderHandEvent> observer;
    static void shot(String name){Screenshot.grab(mc().gameDirectory,"wg-port9-"+String.format("%02d",++screenshots)+"-"+name+".png",mc().getMainRenderTarget(),c->System.out.println("WG_PORT9_SCREENSHOT: "+name));}
    @SuppressWarnings("unchecked") static List<GuiMessage> chat(){try{var field=ChatComponent.class.getDeclaredField("allMessages");field.setAccessible(true);return (List<GuiMessage>)field.get(mc().gui.getChat());}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    static List<Component> readings(){return chat().stream().map(GuiMessage::content).filter(c->c.getString().contains("aer: ")).toList();}
    static Object field(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(ReflectiveOperationException e){return e.getClass().getSimpleName();}}
    static void trace(String label){
        var p=mc().player;var renderer=mc().gameRenderer.itemInHandRenderer;String selection;
        try{var m=renderer.getClass().getDeclaredMethod("evaluateWhichHandsToRender",net.minecraft.client.player.LocalPlayer.class);m.setAccessible(true);selection=String.valueOf(m.invoke(null,p));}catch(ReflectiveOperationException e){selection=e.getClass().getSimpleName();}
        System.out.println("WG_PORT9_WAND_STATE: "+label+"; main="+p.getMainHandItem()+"; off="+p.getOffhandItem()+"; using="+p.isUsingItem()+"; usedHand="+p.getUsedItemHand()+"; usingStack="+p.getUseItem()+"; selection="+selection+"; cachedMain="+field(renderer,"mainHandItem")+"; mainHeight="+field(renderer,"mainHandHeight")+"; cachedOff="+field(renderer,"offHandItem")+"; offHeight="+field(renderer,"offHandHeight")+"; attackScale="+p.getAttackStrengthScale(1)+"; attackSpeed="+p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)+"; sameMainRef="+(field(renderer,"mainHandItem")==p.getMainHandItem())+"; mainFrames="+wandMainFrames+"; offFrames="+wandOffFrames+"; canceled="+canceledFrames);
    }
    static void swap(){mc().gameMode.handleInventoryMouseClick(mc().player.inventoryMenu.containerId,36,40,ClickType.SWAP,mc().player);}
    static void release(){mc().options.keyUse.setDown(false);mc().gameMode.releaseUsingItem(mc().player);}
    static ItemStack wand(String rod,boolean partial){var s=Wands.create(WandRegistry.cap("gold"),WandRegistry.rod(rod),false);Wands.fill(s);if(partial)Wands.storeVis(s,Aspect.AIR,Wands.getMaxVis(s)-1);return s;}
    static ItemStack route(int size,boolean active){
        var stack=Content.stack("guiding_thread");var tag=Data.get(stack);var points=new net.minecraft.nbt.ListTag();
        for(int i=0;i<size;i++){var point=new net.minecraft.nbt.CompoundTag();point.putDouble("X",i<2?4.5+i:4000+i);point.putDouble("Y",64);point.putDouble("Z",i<2?6:4000);point.putInt("Heading",0);points.add(point);}
        tag.put("Waypoints",points);tag.putBoolean("Guiding",active);tag.putString("PathDimension","minecraft:overworld");Data.put(stack,tag);return stack;
    }
    static void aimNode(ServerPlayer p){view(p,.5,64,7.5,NODE);}
    static void inspectCase(String name,boolean partial,boolean swapWhileUsing){
        add("prepare "+name,p->{p.closeContainer();p.getInventory().clearContent();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);give(p,wand("greatwood",partial));aimNode(p);p.inventoryMenu.sendAllDataToRemote();},()->{release();wandMainFrames=0;wandOffFrames=0;canceledFrames=0;},p->{});
        add("baseline main "+name,p->{},()->{trace(name+"-baseline");shot(name+"-baseline-main");swap();},p->check(p.getMainHandItem().isEmpty()&&p.getOffhandItem().is(Content.tc("wand")),"normal inventory swap did not move wand to offhand"));
        add("hold full/near-full offhand wand on node "+name,p->{},()->{mc().options.keyUse.setDown(true);mc().gameMode.useItem(mc().player,InteractionHand.OFF_HAND);},p->System.out.println("WG_PORT9_SERVER_USE: "+name+"; using="+p.isUsingItem()+"; hand="+p.getUsedItemHand()+"; air="+Wands.getVis(p.getOffhandItem(),Aspect.AIR)+"/"+Wands.getMaxVis(p.getOffhandItem())));
        add("observe offhand use "+name,p->{},()->{trace(name+"-offhand-use");shot(name+"-offhand-use");if(!swapWhileUsing)release();swap();},p->check(p.getMainHandItem().is(Content.tc("wand")),"normal inventory return did not restore main wand"));
        add("observe returned main wand "+name,p->{},()->{trace(name+"-returned-main");shot(name+"-returned-main");release();},p->{});
        add("observe released main wand "+name,p->{},()->{trace(name+"-after-release");shot(name+"-after-release");},p->{});
        add("switch to a different wand "+name,p->{give(p,wand("silverwood",false));view(p,.5,64,7.5,new BlockPos(0,65,0));},()->{},p->{});
        add("observe different main wand "+name,p->{},()->{trace(name+"-different-wand");shot(name+"-different-wand");},p->{});
    }
    static void plan(){
        add("prepare isolated detail scene",p->{var l=p.serverLevel();p.closeContainer();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.removeAllEffects();p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);for(var pos:BlockPos.betweenClosed(-5,63,-5,12,70,12))l.setBlockAndUpdate(pos,pos.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);l.setBlockAndUpdate(NODE,ModBlocks.AURA_NODE.get().defaultBlockState());((NodeBlockEntity)l.getBlockEntity(NODE)).setAspects(new AspectList().add(Aspect.AIR,41).add(Aspect.EARTH,2).add(Aspect.WATER,3).add(Aspect.ENTROPY,32));give(p,ItemStack.EMPTY);aimNode(p);System.out.println("WG_PORT9_STACK_LIMITS: abacus="+Content.stack("arcane_abacus").getMaxStackSize()+"; cutters="+Content.stack("gemcutters_tools").getMaxStackSize()+"; developed="+Content.stack("developed_iconograph").getMaxStackSize()+"; thread="+Content.stack("guiding_thread").getMaxStackSize());},()->{mc().setScreen(null);mc().options.pauseOnLostFocus=false;mc().options.hideGui=false;mc().options.keyUse.setDown(false);observer=e->{if(e.getItemStack().is(Content.tc("wand"))){if(e.getHand()==InteractionHand.MAIN_HAND)wandMainFrames++;else wandOffFrames++;if(e.isCanceled())canceledFrames++;}};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,true,observer);},p->{});
        if(!Boolean.getBoolean("wg.wandOnly")){
            place("essentia_pump",PUMP);
            add("view bellows nozzle and tangential motion",p->{give(p,ItemStack.EMPTY);view(p,1.6,64.6,-1.4,PUMP);},()->shot("bellows-inward-nozzles-motion-a"),p->{});
            add("view bellows second animation phase",p->{},()->shot("bellows-inward-nozzles-motion-b"),p->{});
            add("view other bellows attachments",p->view(p,-1.7,64.6,-1.3,PUMP),()->shot("bellows-opposite-side"),p->{});
            add("read node once through network",p->{give(p,Content.stack("arcane_abacus"));aimNode(p);},()->{mc().gui.getChat().clearMessages(true);click(NODE,Direction.SOUTH,false);},p->{});
            add("verify single colored node report",p->{},()->{var reports=readings();check(reports.size()==1,"one click produced "+reports.size()+" report entries");for(var a:GemItem.PRIMALS){var line=reports.getFirst().getSiblings().stream().filter(c->c.getString().startsWith(a.getTag()+": ")).findFirst().orElseThrow();check(line.getStyle().getColor().getValue()==AbacusReport.color(a),"wrong aspect color: "+a);}mc().setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));System.out.println("WG_PORT9_ABACUS_OK: one server entry per network click; six correct aspect colors");},p->{});
            add("show colored first report",p->{},()->{shot("abacus-single-colored-report");mc().setScreen(null);},p->((NodeBlockEntity)p.serverLevel().getBlockEntity(NODE)).setAspects(new AspectList().add(Aspect.AIR,22).add(Aspect.EARTH,2).add(Aspect.WATER,3).add(Aspect.ENTROPY,32)));
            add("read node a second time",p->{},()->click(NODE,Direction.SOUTH,false),p->{});
            add("verify separate report entries",p->{},()->{var reports=readings();check(reports.size()==2&&reports.get(0).getString().contains("aer: 22")&&reports.get(1).getString().contains("aer: 41"),"old/new node reports mixed or duplicated");mc().setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));},p->{});
            add("show separated readings",p->{},()->{shot("abacus-separated-readings");mc().setScreen(null);},p->{});
        }
        if(!Boolean.getBoolean("wg.wandOnly")){
            add("record beyond original port route cap",p->{give(p,route(4096,true));p.connection.teleport(4.5,64,8.5,90,0);},()->{},p->{check(Data.get(p.getMainHandItem()).getList("Waypoints",net.minecraft.nbt.Tag.TAG_COMPOUND).size()>4096,"normal world ticks stopped recording at old 4096-point cap");System.out.println("WG_PORT9_GUIDANCE_RECORD_OK: native inventory/world ticks recorded beyond 4096 points");});
            add("show unrestricted recorded route",p->{},()->shot("guidance-long-route-recording"),p->{});
            add("prepare old visible route segment",p->{give(p,route(4096,false));view(p,4.5,64,8.5,new BlockPos(5,65,6));},()->{beamStart=ClientEffects.beams;beamTick=mc().player.tickCount;},p->{});
            add("verify route history outside old last-128 limit",p->{},()->{check(ClientEffects.beams>beamStart,"old nearby route segments were not rendered");shot("guidance-entire-history-visible");System.out.println("WG_PORT9_GUIDANCE_HISTORY_OK: early nearby segment of a 4096-point stopped route is still drawn");},p->{});
            add("prepare stopped route with two points",p->give(p,route(2,false)),()->{beamStart=ClientEffects.beams;beamTick=mc().player.tickCount;},p->{});
            add("verify stopped route has no hand tail",p->{},()->{int elapsed=mc().player.tickCount-beamTick,drawn=ClientEffects.beams-beamStart;check(Math.abs(drawn-elapsed)<=1,"stopped route still draws a hand tail: "+drawn+" beams in "+elapsed+" ticks");shot("guidance-stopped-recording-no-hand-tail");System.out.println("WG_PORT9_GUIDANCE_STOP_OK: one historical segment per tick, no active hand tail");},p->{});
        }
        if(!Boolean.getBoolean("wg.uiOnly")){
        inspectCase("full-release-swap",false,false);
        inspectCase("near-full-release-swap",true,false);
        inspectCase("full-swap-during-use",false,true);
        }
        add("finish read-only wand investigation",p->System.out.println("WG_PORT9_SERVER_OK: normal wand hand swaps and node use completed; no TC state patched"),()->{release();trace("final");net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer);System.out.println("WG_PORT9_CLIENT_OK: "+screenshots+" screenshots; TC renderer inspected without modification");mc().stop();},p->{});
    }
}
