package witchinggadgets;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
public final class StorageScreen extends AbstractContainerScreen<StorageMenu>{
    StorageScreen(StorageMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){String texture=menu.count==5?"primordialglove":menu.kind.equals("voidlinked_bag")?"voidbag":menu.size==27?"bagcloak":"bag";g.blit(WitchingGadgets.id("textures/gui/"+texture+".png"),leftPos,topPos,0,0,imageWidth,imageHeight,256,256);if(menu.count==5)GloveRendering.node(g,menu,leftPos,topPos);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){if(menu.count!=5)g.drawString(font,Component.translatable("container.inventory"),4,74,0x404040,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);if(menu.count==5&&x>=leftPos+76&&x<leftPos+100&&y>=topPos+22&&y<topPos+47){var aspects=GloveRendering.aspects(menu.holder);if(!aspects.isEmpty()){var text=new java.util.ArrayList<Component>();text.add(Component.translatable("wg.gui.visSize").append(": "+aspects.visSize()));for(var aspect:aspects.getAspects())text.add(Component.literal(aspect.getTag()+": "+aspects.getAmount(aspect)));g.renderComponentTooltip(font,text,x,y);}}}
}
