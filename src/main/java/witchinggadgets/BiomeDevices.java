package witchinggadgets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import thaumcraft.aspect.Aspect;
/** Pack-specific designs using the installed Thaumcraft geometry and textures. */
public final class BiomeDevices {
    static final ResourceLocation STABILIZER=ResourceLocation.parse("thaumcraft:models/obj/node_stabilizer.obj"),CAP=ResourceLocation.parse("thaumcraft:models/obj/obelisk_cap.obj");
    static float time(){var mc=Minecraft.getInstance();return mc.level==null?0:(mc.level.getGameTime()%24000)+mc.getTimer().getGameTimeDeltaPartialTick(true);}
    static void part(ResourceLocation obj,String group,String texture,PoseStack pose,MultiBufferSource buffers,int light,int tint){WgObjModel.get(obj).renderPart(group,pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(texture))),tint,light);}
    static void reformer(PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(-90));part(STABILIZER,"lock","thaumcraft:textures/model/node_stabilizer.png",pose,buffers,light,-1);
        for(int i=0;i<4;i++){pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(90*i));pose.mulPose(Axis.YP.rotationDegrees(45));pose.translate(0,0,.37);part(STABILIZER,"piston","thaumcraft:textures/model/node_stabilizer.png",pose,buffers,light,-1);part(STABILIZER,"piston","thaumcraft:textures/model/node_stabilizer_over.png",pose,buffers,LightTexture.FULL_BRIGHT,0xFF000000|Aspect.PLANT.getColor());pose.popPose();}pose.popPose();
    }
    static net.minecraft.world.level.block.state.BlockState sample(String kind){return (switch(kind){case "focus_cold_taiga"->Blocks.SNOW_BLOCK;case "focus_desert"->Blocks.SAND;case "focus_jungle"->Blocks.JUNGLE_LOG;case "focus_hell"->Blocks.NETHERRACK;case "focus_taint"->thaumcraft.registry.ModBlocks.TAINTED_SOIL.get();case "focus_mushroom"->Blocks.MYCELIUM;default->Blocks.GRASS_BLOCK;}).defaultBlockState();}
    static final net.minecraft.client.model.geom.ModelPart BRINE=brine();
    static net.minecraft.client.model.geom.ModelPart brine(){var m=new net.minecraft.client.model.geom.builders.MeshDefinition();m.getRoot().addOrReplaceChild("brine",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,0).mirror().addBox(-4,-11,-4,8,10,8),net.minecraft.client.model.geom.PartPose.ZERO);return net.minecraft.client.model.geom.builders.LayerDefinition.create(m,64,32).bakeRoot();}
    static void focus(String kind,PoseStack pose,MultiBufferSource buffers,int light,boolean animate){
        pose.pushPose();pose.translate(0,.43+(animate?Math.sin(time()/14)*.025:0),0);if(animate)pose.mulPose(Axis.YP.rotationDegrees(time()*1.2F));pose.scale(.34F,.34F,.34F);pose.translate(-.5,-.5,-.5);var state=sample(kind);var mc=Minecraft.getInstance();int tint=mc.getBlockColors().getColor(state,null,null,0);mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),state,mc.getBlockRenderer().getBlockModel(state),(tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();
        pose.pushPose();pose.translate(0,.01,0);pose.mulPose(Axis.XP.rotationDegrees(180));BRINE.render(pose,buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/model/jarbrine.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();pose.pushPose();pose.translate(-.5,0,-.5);thaumcraft.client.render.EssentiaJarRendering.glass(buffers,pose,light,false);pose.popPose();
    }
    static void pump(PoseStack pose,MultiBufferSource buffers,int light,boolean animate){
        // The alchemical block's front ends exactly at the NORTH tube connection plane.
        pose.pushPose();pose.translate(0,.5,.25);pose.mulPose(Axis.XP.rotationDegrees(-90));
        // The scrubber's fixed Cap is retained; its moving Tip is absent.
        pose.pushPose();pose.translate(0,.05,0);pose.mulPose(Axis.XP.rotationDegrees(-90));pose.scale(.9F,.9F,.5F);part(CAP,"Cap","thaumcraft:textures/model/fluxscrubber.png",pose,buffers,light,-1);pose.popPose();
        pose.pushPose();pose.translate(-.25,.25,-.25);pose.scale(.5F,.5F,.5F);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(thaumcraft.registry.ModBlocks.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(),pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();
        for(int i=0;i<4;i++){
            pose.pushPose();pose.translate(0,.5,0);pose.mulPose(Axis.YP.rotationDegrees(90*i));
            // Native nozzle ends 0.5 model blocks from the center: flush against the construct.
            pose.translate(0,0,-(.25+.5*.27));
            // Nozzle +Z faces inward; the moving plates travel tangentially, clear of the pyramid.
            pose.mulPose(Axis.ZP.rotationDegrees(90));pose.scale(.27F,.27F,.27F);pose.translate(-.5,-.5,-.5);
            float inflation=animate?.45F+.3F*(float)Math.sin(time()/5+i*Math.PI/2):.5F;
            thaumcraft.client.render.AlchemyRendering.bellows(buffers,pose,light,Direction.SOUTH,inflation);pose.popPose();
        }
        pose.popPose();
        PumpIntakeRendering.render(pose,buffers,light);
    }
}
