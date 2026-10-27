package witchinggadgets;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Shared world/item intake: an original-texture rim and the native golden tube core. */
final class PumpIntakeRendering {
    static void render(PoseStack pose,MultiBufferSource buffers,int light){
        var buffer=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumcraft:textures/model/fluxscrubber.png")));
        float lo=-.45F,hi=.45F,bottom=.05F,top=.95F,back=.20F,front=back+1F/16;
        // The old broad face is covered by the same image, advanced by exactly one block pixel.
        face(pose,buffer,light,new float[][]{{lo,top,front},{lo,bottom,front},{hi,bottom,front},{hi,top,front}},0,0,1,0,.5F,.5F,1);
        // The lower-right texture quadrant is transparent. Sample only the opaque outer border,
        // inset to texel centers so filtering cannot reopen the one-pixel seam.
        float u=.5F/32,v0=16.5F/32,v1=31.5F/32;
        face(pose,buffer,light,new float[][]{{lo,top,back},{lo,top,front},{hi,top,front},{hi,top,back}},0,1,0,u,v0,u,v1);
        face(pose,buffer,light,new float[][]{{hi,bottom,back},{hi,bottom,front},{lo,bottom,front},{lo,bottom,back}},0,-1,0,u,v0,u,v1);
        face(pose,buffer,light,new float[][]{{lo,bottom,back},{lo,bottom,front},{lo,top,front},{lo,top,back}},-1,0,0,u,v0,u,v1);
        face(pose,buffer,light,new float[][]{{hi,top,back},{hi,top,front},{hi,bottom,front},{hi,bottom,back}},1,0,0,u,v0,u,v1);
        pose.pushPose();
        // Native unconnected essentia tube = the requested 4x4x4 golden cube, centered on the intake.
        pose.translate(-.5,0,-.1125);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(thaumcraft.registry.ModBlocks.ESSENTIA_TUBE.get().defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
    static void face(PoseStack pose,VertexConsumer buffer,int light,float[][] points,float nx,float ny,float nz,float u0,float v0,float u1,float v1){
        for(int i=0;i<4;i++){var p=points[i];float u=i<2?u0:u1,v=i==0||i==3?v0:v1;buffer.addVertex(pose.last(),p[0],p[1],p[2]).setColor(-1).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),nx,ny,nz);}
    }
}
