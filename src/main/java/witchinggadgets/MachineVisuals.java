package witchinggadgets;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import thaumcraft.aspect.*;

/** Shared world/item rendering with the draw groups and UVs from release 1.1.10. */
public final class MachineVisuals {
    static final ResourceLocation TERRAFORMER=WitchingGadgets.id("textures/models/terraformer.png");
    static final Map<JsonObject,net.minecraft.client.model.geom.ModelPart> PART_CACHE=new IdentityHashMap<>();
    static final JsonArray PUMP=PortData.read("pump_geometry");
    public static void pump(PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(-.5,0,-.5);
        for(var element:PUMP){var face=element.getAsJsonObject();var b=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(PortData.str(face,"texture"))));var n=face.getAsJsonArray("normal");for(var vertex:face.getAsJsonArray("vertices")){var v=vertex.getAsJsonArray();b.addVertex(pose.last(),LegacyModels.f(v,0),LegacyModels.f(v,1),LegacyModels.f(v,2)).setColor(-1).setUv(LegacyModels.f(v,3),LegacyModels.f(v,4)).setLight(light).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose.last(),LegacyModels.f(n,0),LegacyModels.f(n,1),LegacyModels.f(n,2));}}
        pose.scale(.375F,.375F,.375F);pose.translate(.625,-.0625,-.125);pose.mulPose(Axis.ZP.rotationDegrees(45));pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.translate(0,0,-1.0625);thaumcraft.client.render.AlchemyRendering.bellows(buffers,pose,light,net.minecraft.core.Direction.NORTH,.5F);pose.translate(0,0,1.0625);
        pose.mulPose(Axis.XP.rotationDegrees(-90));pose.translate(0,.9375,.875);thaumcraft.client.render.AlchemyRendering.bellows(buffers,pose,light,net.minecraft.core.Direction.NORTH,.5F);pose.translate(0,-.9375,-.875);
        pose.mulPose(Axis.XP.rotationDegrees(-90));pose.translate(0,-1,1.8125);thaumcraft.client.render.AlchemyRendering.bellows(buffers,pose,light,net.minecraft.core.Direction.NORTH,.5F);pose.translate(0,1,-1.8125);
        pose.mulPose(Axis.XP.rotationDegrees(-90));pose.translate(0,-1.9375,-.125);thaumcraft.client.render.AlchemyRendering.bellows(buffers,pose,light,net.minecraft.core.Direction.NORTH,.5F);pose.popPose();
    }
    public static Aspect focusAspect(String kind){return switch(kind){case "focus_plains"->Aspect.PLANT;case "focus_cold_taiga"->Aspect.COLD;case "focus_desert"->Aspect.FIRE;case "focus_jungle"->Aspect.TREE;case "focus_hell"->Aspect.DARKNESS;case "focus_taint"->Aspect.TAINT;case "focus_mushroom"->Aspect.SLIME;default->null;};}
    public static void terraformer(String kind,PoseStack pose,MultiBufferSource buffers,int light,Aspect essentia){
        var obj=WgObjModel.get(WitchingGadgets.id("models/terraformer.obj"));var b=buffers.getBuffer(RenderType.entityCutoutNoCull(TERRAFORMER));boolean focus=kind.startsWith("focus_");if(focus)pose.translate(0,-1,0);
        obj.renderPart(focus?"focus_03":"main_01",pose.last(),b,-1,light);var a=focus?focusAspect(kind):essentia;obj.renderPart(focus?"focus_crystal_04":"crystals_02",pose.last(),b,a==null?-1:0xFF000000|a.getColor(),light);
    }
    static void modelParts(String kind,String key,int index,int color,PoseStack pose,MultiBufferSource buffers,int light){var list=LegacyModels.machine(kind).getAsJsonArray(key).get(index).getAsJsonArray();for(var cube:list)part(cube.getAsJsonObject(),ResourceLocation.parse(PortData.str(LegacyModels.machine(kind),"texture")),pose,buffers,light,color);}
    static void part(JsonObject o,ResourceLocation texture,PoseStack pose,MultiBufferSource buffers,int light,int color){
        var model=PART_CACHE.computeIfAbsent(o,key->{var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();LegacyModels.add(mesh.getRoot(),"part",o);var size=o.getAsJsonArray("size");return net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,size.get(0).getAsInt(),size.get(1).getAsInt()).bakeRoot();});model.render(pose,buffers.getBuffer(RenderType.entityTranslucent(texture)),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,color);
    }
    public static void table(MachineEntity tile,PoseStack pose,MultiBufferSource buffers,int light){
        for(int i=0;i<3;i++){var s=tile.items.getStackInSlot(i+1);var tags=AspectKeys.ASPECTS.get(s);if(s.isEmpty()||tags==null||tags.isEmpty())continue;var a=tags.getAspects().getFirst();String key=s.is(Content.tc("essentia_phial"))?"flasks":"bowls";var list=LegacyModels.machine("cutting_table").getAsJsonArray(key).get(i).getAsJsonArray();for(int n=0;n<list.size();n++){int color=n==list.size()-1?0xFF000000|a.getColor():-1;part(list.get(n).getAsJsonObject(),WitchingGadgets.id("textures/models/cuttingtable.png"),pose,buffers,light,color);}}
        if(!tile.items.getStackInSlot(0).isEmpty()){pose.pushPose();pose.translate(-.2,.89,-.25);pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.375F,.375F,.375F);Minecraft.getInstance().getItemRenderer().renderStatic(tile.items.getStackInSlot(0),ItemDisplayContext.FIXED,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffers,tile.getLevel(),0);pose.popPose();}
    }
}
