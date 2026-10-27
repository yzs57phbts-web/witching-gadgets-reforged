package witchinggadgets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Original OBJ and ModelRenderer geometry, shared by inventory and held items. */
public final class LegacyItemRenderer extends BlockEntityWithoutLevelRenderer {
    public LegacyItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    void part(WgObjModel model,String part,String texture,PoseStack pose,MultiBufferSource buffers,int light,int color){model.renderPart(part,pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(texture))),color,light);}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        String name=Compat.name(stack);pose.pushPose();pose.translate(.5,0,.5);
        if(name.equals("crystal_capsule")){CapsuleRendering.render(stack,pose,buffers,light,overlay);pose.popPose();return;}
        if(name.equals("scan_camera")){
            pose.translate(0,.45,0);pose.scale(1.8F,1.8F,1.8F);pose.mulPose(Axis.YP.rotationDegrees(180));var model=WgObjModel.get(WitchingGadgets.id("models/scancamera.obj"));
            part(model,"Gold_01","thaumcraft:textures/model/scanner.png",pose,buffers,light,-1);part(model,"Leather_02","witchinggadgets:textures/models/cameraleather.png",pose,buffers,light,-1);part(model,"Scanner_03","thaumcraft:textures/model/scanscreen.png",pose,buffers,light,-1);part(model,"Wood_04","thaumcraft:textures/block/planks_greatwood.png",pose,buffers,light,-1);
        }else if(name.equals("primordial_gauntlet")){
            if(context!=ItemDisplayContext.THIRD_PERSON_LEFT_HAND&&context!=ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)ManoplaRendering.item(pose,buffers,light);
        }else if(name.equals("terrestrial_reformer")){BiomeDevices.reformer(pose,buffers,light);}else if(name.startsWith("focus_")){BiomeDevices.focus(name,pose,buffers,light,false);}else if(name.equals("essentia_pump")){BiomeDevices.pump(pose,buffers,light,false);}
        else if(name.equals("spinning_wheel")||name.equals("cutting_table")||name.equals("label_library")){
            if(name.equals("spinning_wheel"))pose.translate(0,0,-.5);var model=LegacyModels.machineModel(name);var texture=ResourceLocation.parse(PortData.str(LegacyModels.machine(name),"texture"));model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,overlay);
        }pose.popPose();
    }
}
