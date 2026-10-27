package witchinggadgets;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
public final class MachineRenderer implements BlockEntityRenderer<MachineEntity>{
    public MachineRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(MachineEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(tile.kind.equals("stone_extruder")){ExtruderRendering.render(tile,partial,pose,buffers,light,overlay);return;}
        if(tile.kind.equals("rose_vines")){var mc=net.minecraft.client.Minecraft.getInstance();for(var face:RoseGeometry.faces(tile.getLevel(),tile.getBlockPos())){pose.pushPose();if(face!=net.minecraft.core.Direction.DOWN){pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(face){case SOUTH->180;case WEST->90;case EAST->270;default->0;}));pose.translate(-.5,0,-.5);}var model=mc.getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(WitchingGadgets.id("block/rose_"+(face==net.minecraft.core.Direction.DOWN?"floor":"wall")+"_"+RoseGeometry.stage(tile.getLevel(),tile.getBlockPos()))));mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(Sheets.cutoutBlockSheet()),tile.getBlockState(),model,1,1,1,light,overlay);pose.popPose();}return;}
        if(tile.kind.equals("ethereal_wall")){WallRendering.render(tile,pose,buffers,light,overlay);return;}
        if(!Set.of("spinning_wheel","cutting_table","terrestrial_reformer","label_library","essentia_pump").contains(tile.kind)&&!tile.kind.startsWith("focus_"))return;
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(tile.facing()){case SOUTH->180;case EAST->270;case WEST->90;default->0;}));
        if(tile.kind.equals("terrestrial_reformer"))BiomeDevices.reformer(pose,buffers,light);
        else if(tile.kind.startsWith("focus_"))BiomeDevices.focus(tile.kind,pose,buffers,light,true);
        else if(tile.kind.equals("essentia_pump"))BiomeDevices.pump(pose,buffers,light,!tile.getLevel().hasNeighborSignal(tile.getBlockPos()));
        else{var model=LegacyModels.machineModel(tile.kind);if(tile.kind.equals("spinning_wheel"))pose.translate(0,0,-.5);var texture=ResourceLocation.parse(PortData.str(LegacyModels.machine(tile.kind),"texture"));model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,overlay);if(tile.kind.equals("cutting_table"))MachineVisuals.table(tile,pose,buffers,light);}
        pose.popPose();
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(MachineEntity e){return new net.minecraft.world.phys.AABB(e.getBlockPos()).inflate(.15,.4,.15);}
}
