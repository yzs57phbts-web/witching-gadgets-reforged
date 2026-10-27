package witchinggadgets;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid=WitchingGadgets.ID,value=Dist.CLIENT)
public final class ClientSmoke {
    private static int ticks;
    private static boolean started;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("wg.clientSmoke"))return;var mc=Minecraft.getInstance();
        if(!started){if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
            for(var entry:Content.ITEM_MAP.entrySet()){var s=new ItemStack(entry.getValue().get());var model=mc.getItemRenderer().getModel(s,null,null,0);if(model==mc.getModelManager().getMissingModel()||model.getParticleIcon().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing model or texture "+entry.getKey());if(!net.minecraft.client.resources.language.I18n.exists(s.getDescriptionId()))throw new IllegalStateException("Missing translation "+s.getDescriptionId());}
            new LegacyModels.Armor("primordial");new LegacyModels.Armor("advanced_robes");LegacyModels.machineModel("spinning_wheel");LegacyModels.machineModel("cutting_table");
            started=true;mc.setScreen(new Preview());System.out.println("WG_CLIENT_SMOKE: "+Content.ITEM_MAP.size()+" baked models and translations; original armor and machine geometry loaded");
        }
        if(++ticks==50)Screenshot.grab(mc.gameDirectory,"wg-items.png",mc.getMainRenderTarget(),m->System.out.println("WG_SCREENSHOT: "+m.getString()));
        if(ticks>=75)mc.stop();
    }
    static final class Preview extends Screen {
        final List<String> names=Content.ITEM_MAP.keySet().stream().filter(ReleaseContent::creative).toList();
        Preview(){super(Component.literal("Witching Gadgets — 1.21.1"));}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xFF15101E);g.drawCenteredString(font,title,width/2,8,0xFFF3DFAE);g.drawCenteredString(font,Component.translatable("tc.research_category.WITCHGADG"),width/2,23,0xFFC69BE8);
            int cols=6,rows=(names.size()+cols-1)/cols,cw=width/cols,ch=Math.max(25,(height-42)/rows);
            for(int i=0;i<names.size();i++){int x=i%cols*cw,y=40+i/cols*ch;var s=Content.stack(names.get(i));g.fill(x+2,y,x+cw-2,y+ch-2,0xFF30213E);g.renderItem(s,x+4,y+5);int line=0;for(var t:font.split(s.getHoverName(),cw-28))g.drawString(font,t,x+24,y+3+(line++)*9,0xFFF1E5CE,false);}
        }
    }
}
