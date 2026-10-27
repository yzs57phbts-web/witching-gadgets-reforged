package witchinggadgets;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GearAbilityScreen extends Screen {
    final List<GearAbilities.Entry> entries;
    int offset,rows,left,top=36;
    GearAbilityScreen(){super(Component.translatable("wg.ability.title"));entries=GearAbilities.entries(net.minecraft.client.Minecraft.getInstance().player);}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        rows=Math.max(1,Math.min(entries.size(),(height-70)/22));left=(width-Math.min(300,width-64))/2;offset=Math.clamp(offset,0,Math.max(0,entries.size()-rows));
        for(int row=0;row<rows;row++){
            var entry=entries.get(offset+row);var label=Component.translatable("wg.ability.slot."+entry.slot()).append(": ").append(entry.stack().getHoverName());
            if((entry.stack().getItem() instanceof PrimordialArmor||entry.stack().getItem() instanceof PrimordialItem)&&entry.stack().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)){var aspect=GemItem.PRIMALS[Math.floorMod(Data.get(entry.stack()).getInt("Affinity"),6)];label.append(" — ").append(Component.translatable("tc.aspect."+aspect.getTag()).withStyle(style->style.withColor(aspect.getColor())));}
            addRenderableWidget(Button.builder(label,b->{net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(entry.action()));onClose();}).bounds(left,top+row*22,width-left*2,20).build());
        }
        if(entries.size()>rows){addRenderableWidget(Button.builder(Component.literal("▲"),b->scroll(-rows)).bounds(width/2-45,height-26,40,20).build());addRenderableWidget(Button.builder(Component.literal("▼"),b->scroll(rows)).bounds(width/2+5,height-26,40,20).build());}
    }
    void scroll(int amount){offset=Math.clamp(offset+amount,0,Math.max(0,entries.size()-rows));clearWidgets();init();}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll((int)-Math.signum(vertical));return true;}
    @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xA0100818);super.render(g,x,y,partial);g.drawCenteredString(font,title,width/2,14,0xE8DBF2);for(int row=0;row<rows;row++)g.renderItem(entries.get(offset+row).stack(),left-21,top+row*22+2);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(Client.ABILITY.matches(key,scan)||Client.SELECT.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void onClose(){while(Client.ABILITY.consumeClick()){}while(Client.SELECT.consumeClick()){}super.onClose();}
}
