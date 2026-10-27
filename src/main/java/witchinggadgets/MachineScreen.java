package witchinggadgets;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.aspect.AspectRenderer;
public final class MachineScreen extends AbstractContainerScreen<MachineMenu>{
    public MachineScreen(MachineMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=menu.machine.kind.equals("spinning_wheel")?184:176;imageHeight=menu.machine.kind.equals("spinning_wheel")?233:166;}
    java.util.List<Aspect> known(){return java.util.Arrays.stream(Aspect.values()).filter(a->minecraft.player.isCreative()||thaumcraft.research.Knowledge.get(minecraft.player).hasDiscoveredAspect(a)).toList();}
    private void send(int action){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    void image(GuiGraphics g,String name,int x,int y,int u,int v,int w,int h){g.blit(WitchingGadgets.id("textures/gui/"+name+".png"),leftPos+x,topPos+y,u,v,w,h,256,256);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        switch(menu.machine.kind){
            case "spinning_wheel"->{image(g,"spinningwheel",0,144,0,144,184,90);image(g,"spinningwheel",0,0,0,0,38,144);image(g,"spinningwheel",126,0,126,0,48,144);int width=Math.clamp(menu.machine.progress*88/121,0,88);if(width>0)image(g,"spinningwheel",38,0,38,0,width,144);}
            case "cutting_table"->{image(g,"cuttingtable",0,0,0,0,176,166);int uv=menu.machine.gemCut==0?0:80;g.blit(WitchingGadgets.id("textures/gui/research/gemcuts_transparent.png"),leftPos+73,topPos+17,30,30,uv,uv,80,80,256,256);Aspect a=menu.machine.essentia;if(a!=null)AspectRenderer.drawTag(g,leftPos+118,topPos+41,a,0,0,1,false);}
            case "label_library"->{image(g,"labellibrary",0,-5,0,0,194,171);int i=0;for(Aspect a:known()){int x=leftPos+36+(i%12)*11,y=topPos+8+(i/12)*11;if(a==menu.machine.essentia)g.fill(x-1,y-1,x+11,y+11,0xA0776688);g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(.65F,.65F,1);AspectRenderer.drawTag(g,0,0,a,0,0,1,false);g.pose().popPose();i++;}if(menu.machine.essentia!=null)AspectRenderer.drawTag(g,leftPos+8,topPos+30,menu.machine.essentia,0,0,1,false);}

            default->{g.fill(leftPos-2,topPos-2,leftPos+imageWidth+2,topPos+imageHeight+2,0xFF2A2033);g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFB6A39B);for(var slot:menu.slots)g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xFF75616E);}
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public boolean mouseClicked(double mx,double my,int button){double x=mx-leftPos,y=my-topPos;if(button==0&&menu.machine.kind.equals("cutting_table")&&y>=12&&y<24&&((x>=59&&x<70)||(x>=106&&x<117))){send(1-menu.machine.gemCut);return true;}if(button==0&&menu.machine.kind.equals("label_library")&&x>=36&&x<168&&y>=8&&y<63){int i=((int)y-8)/11*12+((int)x-36)/11;if(i<known().size()){send(100+known().get(i).ordinal());return true;}}return super.mouseClicked(mx,my,button);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);if(menu.machine.kind.equals("label_library")&&x>=leftPos+36&&x<leftPos+168&&y>=topPos+8&&y<topPos+63){int i=(y-topPos-8)/11*12+(x-leftPos-36)/11;if(i<known().size())g.renderTooltip(font,Component.translatable("tc.aspect."+known().get(i).getTag()),x,y);}if(hoveredSlot!=null&&!hoveredSlot.hasItem()&&hoveredSlot.index==menu.count-1&&(menu.machine.kind.equals("label_library")||menu.machine.kind.equals("cutting_table")))g.renderTooltip(font,Component.translatable(menu.machine.kind.equals("label_library")?"wg.label.apply":"wg.cut.apply"),x,y);}
}

