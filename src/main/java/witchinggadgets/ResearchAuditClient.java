package witchinggadgets;

import java.util.*;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import thaumcraft.aspect.*;
import thaumcraft.client.research.*;
import thaumcraft.research.*;
import static witchinggadgets.GameplayAuditClient.*;

/** Native book clicks and item use in a disposable survival world; no completion commands. */
final class ResearchAuditClient {
    static int screenshots;
    static double cursorX,cursorY;
    static Object get(Object object,String name){try{var field=(object instanceof Class<?> c?c:object.getClass()).getDeclaredField(name);field.setAccessible(true);return field.get(object instanceof Class<?>?null:object);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    static void set(Object object,String name,Object value){try{var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    static void cursor(double x,double y){
        cursorX=x;cursorY=y;var window=mc().getWindow();double px=x*window.getWidth()/window.getGuiScaledWidth(),py=y*window.getHeight()/window.getGuiScaledHeight();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.getWindow(),px,py);
        // Deliver the normal cursor callback as well when the audit window is not focused.
        try{var method=mc().mouseHandler.getClass().getDeclaredMethod("onMove",long.class,double.class,double.class);method.setAccessible(true);method.invoke(mc().mouseHandler,window.getWindow(),px,py);method.invoke(mc().mouseHandler,window.getWindow(),px,py);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    static void shot(String name){cursor(1,1);Screenshot.grab(mc().gameDirectory,"wg-port10-"+String.format("%02d",++screenshots)+"-"+name+".png",mc().getMainRenderTarget(),c->System.out.println("WG_PORT10_SCREENSHOT: "+name));}
    static void pan(int column,int row){
        var screen=mc().screen;check(screen instanceof ResearchBrowserScreen,"native browser absent");
        // Camera setup only; research visibility/hit testing remain native and use synced player data.
        for(String f:List.of("prevMapX","mapX","targetX"))set(screen,f,(double)(column*24-100));
        for(String f:List.of("prevMapY","mapY","targetY"))set(screen,f,(double)(row*24-90));cursor(1,1);
    }
    static void point(String key){
        var screen=mc().screen;var item=ResearchCategories.getResearch(key);
        double x=Math.max((double)get(screen,"mapX"),(int)get(screen,"guiMapTop")),y=Math.max((double)get(screen,"mapY"),(int)get(screen,"guiMapLeft"));
        x=Math.min(x,(int)get(screen,"guiMapBottom")-1);y=Math.min(y,(int)get(screen,"guiMapRight")-1);
        cursor((screen.width-256)/2+16+item.column()*24-(int)x+11,(screen.height-230)/2+17+item.row()*24-(int)y+11);
    }
    static void clickHighlighted(String key){var item=(ResearchItem)get(mc().screen,"currentHighlight");check(item!=null&&item.key().equals(key),"native hit test did not highlight "+key+": "+item);mc().screen.mouseClicked(cursorX,cursorY,0);mc().screen.mouseReleased(cursorX,cursorY,0);}
    static void openBook(){mc().setScreen(null);mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);}
    @SuppressWarnings("unchecked") static void selectTab(){
        var screen=mc().screen;check(screen instanceof ResearchBrowserScreen,"ordinary book use failed");
        try{var method=ResearchBrowserScreen.class.getDeclaredMethod("visibleTabs");method.setAccessible(true);var tabs=(List<ResearchCategory>)method.invoke(screen);int index=-1;for(int i=0;i<tabs.size();i++)if(tabs.get(i).key().equals(Research.CATEGORY))index=i;check(index>=0,"WG tab absent");double x=(screen.width-256)/2-12+(index>=9?280:0),y=(screen.height-230)/2+(index%9)*24+12;screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);cursor(1,1);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        check(Research.CATEGORY.equals(get(ResearchBrowserScreen.class,"selectedCategory")),"tab click selected another category");
    }
    static void supplies(ServerPlayer p){give(p,new ItemStack(Content.tc("thaumonomicon")));p.getInventory().setItem(9,new ItemStack(Items.PAPER,8));p.getInventory().setItem(10,new ItemStack(Content.tc("scribing_tools")));p.inventoryMenu.sendAllDataToRemote();}
    static void sync(ServerPlayer p){Knowledge.sync(p);p.inventoryMenu.sendAllDataToRemote();}
    static void overview(String name,int column,int row){add("pan "+name,p->{},()->pan(column,row),p->{});add("show "+name,p->{},()->shot(name),p->{});}
    static void showPages(String key,int column,int row){
        add("point completed "+key,p->{},()->{if(!(mc().screen instanceof ResearchBrowserScreen)){mc().screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0);}pan(column,row);point(key);},p->{});
        add("open native pages "+key,p->{},()->clickHighlighted(key),p->{});
        add("show native pages "+key,p->{},()->{check(mc().screen instanceof ResearchPageScreen,"native book page did not open "+key);shot("pages-"+key.toLowerCase(Locale.ROOT));},p->{});
        add("return from pages "+key,p->{},()->mc().screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0),p->{});
    }
    static void plan(){
        add("prepare isolated research survival world",p->{p.closeContainer();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setHealth(p.getMaxHealth());p.removeAllEffects();p.getFoodData().setFoodLevel(20);p.connection.teleport(.5,64,.5,0,0);var k=Knowledge.get(p);k.clearResearch();ResearchManager.grantAutoUnlock(p);for(var a:Aspect.values())k.setAspectPool(a,100);supplies(p);sync(p);},()->{mc().options.pauseOnLostFocus=false;mc().options.hideGui=false;openBook();},p->{});
        add("select WG through native tab button",p->{},ResearchAuditClient::selectTab,p->{});
        overview("fresh-bag-spinning-potions",8,2);
        add("point native bag research",p->{},()->point("BAGOFTRICKS"),p->{});
        add("request bag note through real book click",p->{},()->clickHighlighted("BAGOFTRICKS"),p->{check(ResearchNotes.findNoteSlot(p,"BAGOFTRICKS")>=0,"native GUI did not create bag note");check(p.getInventory().countItem(Items.PAPER)==7,"note did not charge one paper");check(!ResearchManager.isComplete(p,"BAGOFTRICKS"),"note request learned research without puzzle");System.out.println("WG_PORT10_NOTE_OK: native browser click created real BAGOFTRICKS puzzle and paid paper/ink");});
        add("show native note popup",p->{},()->shot("bag-note-native-popup"),p->{});
        add("prepare solved bag puzzle fixture",p->{int slot=ResearchNotes.findNoteSlot(p,"BAGOFTRICKS");var stack=p.getInventory().removeItemNoUpdate(slot);var n=ResearchNoteKeys.NOTE.get(stack);ResearchNoteKeys.NOTE.set(stack,new ResearchNote(n.key(),n.color(),true,n.copies(),n.hexes()));give(p,stack);sync(p);},()->{mc().setScreen(null);mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);},p->check(ResearchManager.isComplete(p,"BAGOFTRICKS"),"normal solved-note use did not learn bag"));
        add("reopen ordinary book",ResearchAuditClient::supplies,ResearchAuditClient::openBook,p->{});
        overview("bag-learned-hungry-still-gated",8,2);
        overview("devices-starting-progress",0,-5);
        add("prepare solved native essentia-mirror note fixture",p->{var r=ResearchCategories.getResearch("MIRRORESSENTIA");ResearchParityTests.prerequisites(p,r);var stack=ResearchNotes.createNote(r,p.getRandom());var n=ResearchNoteKeys.NOTE.get(stack);ResearchNoteKeys.NOTE.set(stack,new ResearchNote(n.key(),n.color(),true,n.copies(),n.hexes()));give(p,stack);sync(p);},()->{mc().setScreen(null);mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);},p->{check(ResearchManager.isComplete(p,"MIRRORESSENTIA")&&ResearchManager.isComplete(p,"WGFAKEMIRRORESSENTIA"),"native TC note did not activate WG essentia-mirror alias");check(ResearchManager.hasRequisites(p,"MIRRORPUMP"),"WG mirror pump stayed locked");System.out.println("WG_PORT10_ALIAS_OK: native TC essentia-mirror note use activated WG mirror root and pump prerequisites immediately");});
        add("reopen after native parent learning",ResearchAuditClient::supplies,ResearchAuditClient::openBook,p->{});
        overview("devices-after-essentia-mirror-with-links",0,-5);
        add("point age stone",p->{},()->point("AGEINGSTONE"),p->{});
        add("purchase age stone through native click",p->{},()->clickHighlighted("AGEINGSTONE"),p->{check(ResearchManager.isComplete(p,"AGEINGSTONE"),"native GUI purchase failed");check(Knowledge.get(p).getAspectPool(Aspect.LIFE)==97&&Knowledge.get(p).getAspectPool(Aspect.MECHANISM)==97,"native GUI purchase charged wrong aspect costs");System.out.println("WG_PORT10_PURCHASE_OK: native GUI spent VICTUS 3 + MACHINA 3 once");});
        overview("age-stone-purchased",0,-5);
        showPages("AGEINGSTONE",0,-5);
        add("prepare broad native progress fixture",p->{for(var r:ResearchCategories.allResearch())if(!r.category().equals(Research.CATEGORY))Knowledge.get(p).addResearch(r.key());Research.syncAliases(p);sync(p);},()->{},p->{});
        overview("all-native-parents-devices",0,-5);
        overview("all-native-parents-metallurgy",-5,-2);
        overview("all-native-parents-enchantments",-8,2);
        overview("all-native-parents-cloaks",8,-3);
        overview("all-native-parents-bags",8,2);
        overview("all-native-parents-primordial",0,3);
        add("prepare hidden clue fixture",p->{Knowledge.get(p).addResearch("@ENDERBAG");Knowledge.get(p).addResearch("@EMPOWERPEARL");sync(p);},()->{},p->{});
        overview("discovered-hidden-bag",8,2);
        overview("discovered-hidden-pearl",0,3);
        add("prepare fully learned WG display fixture",p->{for(var r:ResearchCategories.get(Research.CATEGORY).research())Knowledge.get(p).addResearch(r.key());sync(p);},()->{},p->{});
        overview("complete-devices-custom-models",0,-5);
        overview("complete-metallurgy",-5,-2);
        overview("complete-cloaks",8,-3);
        overview("complete-bags-correct-icons",8,2);
        overview("complete-enchantments",-8,2);
        overview("complete-primordial",0,3);
        showPages("BAGOFTRICKS",8,2);showPages("SCANCAMERA",0,-5);
        add("open camera next spread",p->{},()->{pan(0,-5);point("SCANCAMERA");},p->{});
        add("click camera for developing spread",p->{},()->clickHighlighted("SCANCAMERA"),p->{});
        add("turn native camera page",p->{},()->{var s=mc().screen;check(s instanceof ResearchPageScreen,"camera page absent");int before=(int)get(s,"page");s.mouseClicked((s.width-256)/2+267,(s.height-181)/2+194,0);check((int)get(s,"page")==before+2,"native page arrow did not turn the spread");},p->{});
        add("show camera developing recipe",p->{},()->shot("camera-developing-recipe"),p->{});
        add("finish research audit",p->System.out.println("WG_PORT10_SERVER_OK: native book note request, exact paid purchase, solved-note use and cross-tab unlocks; other progress states explicitly supplied as fixtures"),()->{System.out.println("WG_PORT10_CLIENT_OK: "+screenshots+" screenshots; native WG tab, icons, pages and real network learning");mc().stop();},p->{});
    }
}
