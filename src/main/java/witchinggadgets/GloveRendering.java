package witchinggadgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.aspect.*;
import thaumcraft.node.*;

final class GloveRendering {
    static AspectList aspects(net.minecraft.world.item.ItemStack glove){var list=new AspectList();var tag=Data.get(glove).getCompound("Node");for(var aspect:GemItem.PRIMALS)if(tag.getInt(aspect.name())>0)list.add(aspect,tag.getInt(aspect.name()));return list;}
    static void node(GuiGraphics graphics,StorageMenu menu,int left,int top){
        var aspects=aspects(menu.holder);if(aspects.isEmpty())return;
        var data=Data.get(menu.holder);NodeType type;NodeModifier modifier=null;
        try{type=NodeType.valueOf(data.getString("NodeType"));}catch(IllegalArgumentException e){type=NodeType.NORMAL;}
        try{modifier=NodeModifier.valueOf(data.getString("NodeModifier"));}catch(IllegalArgumentException ignored){}
        graphics.flush();graphics.pose().pushPose();graphics.pose().translate(left+88,top+34,150);graphics.pose().scale(70,-70,70);
        var mc=Minecraft.getInstance();var buffers=mc.renderBuffers().bufferSource();thaumcraft.client.render.NodeRendering.submit(buffers,graphics.pose(),aspects,type,modifier,mc.player.tickCount+mc.getTimer().getGameTimeDeltaPartialTick(true));buffers.endBatch();graphics.pose().popPose();
    }
}
