package witchinggadgets;

import java.util.*;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.aspect.*;
import thaumcraft.client.research.*;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.*;
import static witchinggadgets.GameplayAuditClient.*;

/** Client regression for recipe-specific changes; the exhaustive chains run separately on the server. */
final class RecipeReviewClient {
    static final BlockPos CRAFT=new BlockPos(0,64,0),ONE=new BlockPos(3,64,0),TWO=new BlockPos(5,64,0),PEARL=new BlockPos(0,64,5);
    static int shots;
    static void shot(String name){Screenshot.grab(mc().gameDirectory,"wg-port11-"+String.format("%02d",++shots)+"-"+name+".png",mc().getMainRenderTarget(),c->System.out.println("WG_PORT11_SCREENSHOT: "+name));}
    static void insert(int index,int slot){OrdinaryAuditClient.insert(index,slot);}
    static void holdActualTools(ServerPlayer p){for(int i=0;i<p.getInventory().items.size();i++)if(p.getInventory().getItem(i).is(Content.item("gemcutters_tools"))){var stack=p.getInventory().removeItemNoUpdate(i);give(p,stack);p.inventoryMenu.sendAllDataToRemote();return;}throw new IllegalStateException("crafted tools absent");}
    static void release(){mc().options.keyUse.setDown(false);mc().gameMode.releaseUsingItem(mc().player);}
    static void atlas(){
        int checked=0;for(var item:Content.ITEM_MAP.values()){
            var stack=new ItemStack(item.get());var model=mc().getItemRenderer().getModel(stack,mc().level,mc().player,0);check(model!=mc().getModelManager().getMissingModel(),"missing item model "+stack);
            var quads=new ArrayList<>(model.getQuads(null,null,net.minecraft.util.RandomSource.create(0)));for(var side:Direction.values())quads.addAll(model.getQuads(null,side,net.minecraft.util.RandomSource.create(0)));
            for(var quad:quads){check(!quad.getSprite().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()),"unstiched legacy item texture "+stack);checked++;}
        }System.out.println("WG_PORT11_ATLAS_OK: "+checked+" item-model quads have real textures, including minimal dependencies");
    }
    static void plan(){
        add("prepare disposable recipe review",p->{p.closeContainer();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setHealth(20);p.removeAllEffects();p.getFoodData().setFoodLevel(20);for(var pos:BlockPos.betweenClosed(-5,63,-4,8,70,9))p.serverLevel().setBlockAndUpdate(pos,pos.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());for(var e:p.serverLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(-5,63,-4,9,72,10)))e.discard();var k=Knowledge.get(p);k.clearResearch();for(var r:ResearchCategories.allResearch())if(!r.category().equals(Research.CATEGORY))k.addResearch(r.key());for(var a:Aspect.values()){k.discoverAspect(a);k.setAspectPool(a,1000);}ResearchManager.grantAutoUnlock(p);Research.syncAliases(p);Knowledge.sync(p);give(p,ItemStack.EMPTY);view(p,2.5,64,2.5,CRAFT);},()->{mc().options.pauseOnLostFocus=false;mc().options.hideGui=false;mc().setScreen(null);release();atlas();},p->{});
        place(new ItemStack(Blocks.CRAFTING_TABLE),CRAFT,Direction.UP);place(new ItemStack(ModBlocks.TABLE.get()),ONE,Direction.UP);place(new ItemStack(ModBlocks.TABLE.get()),TWO,Direction.UP);
        add("open actual crafting table",p->{give(p,ItemStack.EMPTY);p.getInventory().setItem(9,new ItemStack(Items.QUARTZ));p.getInventory().setItem(10,new ItemStack(Items.FLINT));p.getInventory().setItem(11,new ItemStack(Items.IRON_INGOT));p.getInventory().setItem(12,new ItemStack(Items.STICK,3));p.inventoryMenu.sendAllDataToRemote();view(p,2.5,64,2.5,CRAFT);},()->click(CRAFT,Direction.UP,false),p->check(p.containerMenu instanceof CraftingMenu,"normal crafting table failed"));
        add("insert original cutting-tools pattern",p->{},()->{insert(9,1);insert(10,2);insert(11,3);for(int slot=4;slot<=6;slot++){OrdinaryAuditClient.mouse(OrdinaryAuditClient.inventorySlot(12),ClickType.PICKUP);var m=mc().player.containerMenu;mc().gameMode.handleInventoryMouseClick(m.containerId,slot,1,ClickType.PICKUP,mc().player);OrdinaryAuditClient.mouse(OrdinaryAuditClient.inventorySlot(12),ClickType.PICKUP);}},p->{});
        add("take cutting tools through network",p->{},()->{check(mc().player.containerMenu.getSlot(0).getItem().is(Content.item("gemcutters_tools")),"real client cutting recipe absent");shot("tools-original-crafting-pattern");OrdinaryAuditClient.mouse(0,ClickType.QUICK_MOVE);},p->check(p.getInventory().countItem(Content.item("gemcutters_tools"))==1,"ordinary output click did not give exactly one tool"));
        add("close crafting table",p->{},()->mc().player.closeContainer(),p->{});
        add("form first cutting table with crafted tool",p->{holdActualTools(p);view(p,4.5,64,2.5,ONE);},()->click(ONE,Direction.UP,false),p->{check(p.serverLevel().getBlockState(ONE).is(Content.block("cutting_table")),"crafted tools failed on actual table");check(p.getMainHandItem().getCount()==1,"first table consumed reusable tools");});
        add("form second table with the same tool",p->view(p,6.5,64,2.5,TWO),()->{shot("first-table-tools-retained");click(TWO,Direction.UP,false);},p->{check(p.serverLevel().getBlockState(TWO).is(Content.block("cutting_table"))&&p.getMainHandItem().getCount()==1,"same tool cannot create second table");System.out.println("WG_PORT11_TOOLS_OK: actual crafting and two conversions over network, same reusable item");});
        add("show both formed tables",p->view(p,5.5,64,4.5,ONE),()->shot("two-tables-one-crafted-tool"),p->{});
        add("prepare scan of pearl fixture",p->{var item=new ItemEntity(p.serverLevel(),PEARL.getX()+.5,PEARL.getY()+.45,PEARL.getZ()+.5,Content.stack("powerless_pearl"));item.setNoGravity(true);item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.serverLevel().addFreshEntity(item);give(p,new ItemStack(Content.tc("thaumometer")));view(p,.5,64,8.5,PEARL);},()->{},p->{});
        add("hold thaumometer on pearl",p->{},()->{var tags=AspectHelper.getObjectTags(Content.stack("powerless_pearl"));check(tags!=null&&tags.getAmount(Aspect.CRYSTAL)==4&&tags.getAmount(Aspect.VOID)==4,"original pearl aspects did not sync to client");var hit=ThaumometerClient.lookAt(mc().level,mc().player);check(hit!=null&&hit.target().clueStack().is(Content.item("powerless_pearl")),"ordinary thaumometer aim missed pearl");mc().options.keyUse.setDown(true);mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);},p->{});
        add("allow normal scanner duration",p->{},()->shot("real-thaumometer-pearl-aspects"),p->{check(ResearchManager.isComplete(p,"@EMPOWERPEARL"),"normal held thaumometer did not reveal pearl restoration");System.out.println("WG_PORT11_SCAN_OK: server object tags synced to client; held thaumometer completed a real scan and revealed EMPOWERPEARL");});
        add("release scanner and open ordinary book",p->give(p,new ItemStack(Content.tc("thaumonomicon"))),()->{release();ResearchAuditClient.openBook();},p->{});
        add("select WG tab",p->{},ResearchAuditClient::selectTab,p->{});
        add("point revealed pearl restoration",p->{},()->{ResearchAuditClient.pan(0,3);ResearchAuditClient.point("EMPOWERPEARL");},p->{});
        add("purchase revealed research with normal click",p->{},()->{shot("pearl-restoration-revealed-by-scan");ResearchAuditClient.clickHighlighted("EMPOWERPEARL");},p->{check(ResearchManager.isComplete(p,"EMPOWERPEARL"),"ordinary click did not buy revealed research");System.out.println("WG_PORT11_RESEARCH_OK: real scan -> native book -> paid research");});
        add("prepare focus-page display fixture",p->{Knowledge.get(p).addResearch("TERRAFORMER");Knowledge.get(p).addResearch("TERRAFORMFOCUS_DESERT");Knowledge.sync(p);},()->{var r=ResearchCategories.getResearch("TERRAFORMFOCUS_DESERT");ResearchAuditClient.pan(r.column(),r.row());ResearchAuditClient.point(r.key());},p->{});
        add("open focus research pages",p->{},()->ResearchAuditClient.clickHighlighted("TERRAFORMFOCUS_DESERT"),p->{});
        add("show restored recipe and unchanged focus model",p->{},()->{check(mc().screen instanceof ResearchPageScreen,"focus pages absent");shot("original-focus-cost-new-model");},p->{});
        add("finish recipe client review",p->System.out.println("WG_PORT11_SERVER_OK: native network crafting, reusable tool conversion, genuine held scan, research purchase; focus progress supplied only for page display"),()->{release();System.out.println("WG_PORT11_CLIENT_OK: "+shots+" screenshots");mc().stop();},p->{});
    }
}
