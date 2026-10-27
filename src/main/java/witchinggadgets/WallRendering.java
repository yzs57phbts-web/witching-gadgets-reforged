package witchinggadgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

final class WallRendering {
    static void render(MachineEntity wall,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        // Redstone removes collision only. The original wall and its disguise stay visible.
        var mc=Minecraft.getInstance();var state=wall.original==null?wall.getBlockState():wall.original;
        var renderer=mc.getBlockRenderer();var model=renderer.getBlockModel(state);
        int color=mc.getBlockColors().getColor(state,wall.getLevel(),wall.getBlockPos(),0);
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        for(var layer:model.getRenderTypes(state,RandomSource.create(state.getSeed(wall.getBlockPos())),ModelData.EMPTY)){
            renderer.getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderTypeHelper.getEntityRenderType(layer,false)),state,model,r,g,b,light,overlay,ModelData.EMPTY,layer);
        }
    }
}
