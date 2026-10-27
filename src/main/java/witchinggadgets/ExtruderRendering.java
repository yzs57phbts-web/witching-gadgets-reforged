package witchinggadgets;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

final class ExtruderRendering {
    static void render(MachineEntity machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(machine.getLevel().hasNeighborSignal(machine.getBlockPos()))return;
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(machine.facing()){case SOUTH->180;case WEST->90;case EAST->270;default->0;}));pose.translate(-.5,0,-.5);
        float scroll=(machine.progress+partial)/160;
        box(pose,buffers,"block/lava_flow",.1875F,.3125F,.375F,.3125F,.8125F,.625F,0xFFFFFFFF,LightTexture.FULL_BRIGHT,scroll);
        int water=0xFF000000|net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(machine.getLevel(),machine.getBlockPos());
        box(pose,buffers,"block/water_flow",.6875F,.3125F,.375F,.8125F,.8125F,.625F,water,light,scroll);
        if(machine.progress>32){pose.translate(.3125,.3125,.3125);pose.scale(.375F,.375F,.375F);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.COBBLESTONE.defaultBlockState(),pose,buffers,light,overlay);}
        pose.popPose();
    }
    static void box(PoseStack pose,MultiBufferSource buffers,String texture,float x0,float y0,float z0,float x1,float y1,float z1,int color,int light,float scroll){
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace(texture));
        var vertices=buffers.getBuffer(Sheets.translucentCullBlockSheet());
        float[][] points={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] faces={{0,3,2,1},{5,6,7,4},{4,7,3,0},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        for(int side=0;side<6;side++)for(int i=0;i<4;i++){
            var point=points[faces[side][i]];float u=sprite.getU(i<2?.25F:.75F),v=sprite.getV(scroll+(i==0||i==3?.25F:0));
            vertices.addVertex(pose.last(),point[0],point[1],point[2]).setColor(color).setUv(u,v).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose.last(),normals[side][0],normals[side][1],normals[side][2]);
        }
    }
}
