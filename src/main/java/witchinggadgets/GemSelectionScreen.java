package witchinggadgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.component.DataComponents;

/** The same focus key as Thaumcraft selects the five gauntlet sockets. */
final class GemSelectionScreen extends Screen {
    int selected;
    GemSelectionScreen(ItemStack glove){super(glove.getHoverName());selected=Math.floorMod(Data.get(glove).getInt("Affinity"),5);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        var contents=minecraft.player.getMainHandItem().getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        var gems=contents.stream().toList();double dx=mouseX-width/2.0,dy=mouseY-height/2.0;
        if(dx*dx+dy*dy>100){double angle=Math.atan2(dx,-dy);selected=Math.floorMod((int)Math.round(angle*5/(Math.PI*2)),5);}
        for(int i=0;i<5;i++){double angle=i*Math.PI*2/5;int x=width/2+(int)(Math.sin(angle)*48)-8,y=height/2-(int)(Math.cos(angle)*48)-8;float tint=i==selected?1:.65F;g.setColor(1,tint,tint,1);g.blit(WitchingGadgets.id("textures/gui/primordialglove.png"),x-4,y-4,56,2,24,24,256,256);g.setColor(1,1,1,1);if(i<gems.size()&&!gems.get(i).isEmpty()){g.renderItem(gems.get(i),x,y);g.renderItemDecorations(font,gems.get(i),x,y);}}
        g.renderItem(minecraft.player.getMainHandItem(),width/2-8,height/2-8);
        g.drawCenteredString(font,title,width/2,height/2+69,0xE8DBF2);
    }
    void select(){net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(10+selected));onClose();}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0){double dx=x-width/2.0,dy=y-height/2.0;if(dx*dx+dy*dy>100)selected=Math.floorMod((int)Math.round(Math.atan2(dx,-dy)*5/(Math.PI*2)),5);select();return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean keyReleased(int key,int scan,int modifiers){if(thaumcraft.client.wand.WandKeyHandler.CHANGE_FOCUS.matches(key,scan)){select();return true;}return super.keyReleased(key,scan,modifiers);}
}
