package witchinggadgets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
public final class ManoplaRendering {
    static boolean gauntlet(ItemStack s){return s.is(Content.item("primordial_gauntlet"));}
    static HumanoidArm arm(AbstractClientPlayer p,InteractionHand hand){return hand==InteractionHand.MAIN_HAND?p.getMainArm():p.getMainArm().getOpposite();}
    static void mesh(HumanoidArm arm,PoseStack pose,MultiBufferSource buffers,int light){int frame=Minecraft.getInstance().level==null?1:1+(int)(Minecraft.getInstance().level.getGameTime()/2%4);var model=WgObjModel.get(WitchingGadgets.id("models/manopla.obj"));model.renderPart(arm==HumanoidArm.RIGHT?"rightarm":"leftarm",pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(WitchingGadgets.id("textures/manopla/manopla"+frame+".png"))),-1,light);}
    static void item(PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.translate(0,.75,0);pose.mulPose(Axis.XP.rotationDegrees(180));mesh(HumanoidArm.RIGHT,pose,buffers,light);pose.popPose();}
    public static final class Layer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>>{
        public Layer(PlayerRenderer renderer){super(renderer);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer p,float limb,float amount,float partial,float age,float yaw,float pitch){for(var hand:InteractionHand.values())if(gauntlet(p.getItemInHand(hand))){var arm=arm(p,hand);pose.pushPose();(arm==HumanoidArm.RIGHT?getParentModel().rightArm:getParentModel().leftArm).translateAndRotate(pose);mesh(arm,pose,buffers,light);pose.popPose();}}
    }
    @EventBusSubscriber(modid=WitchingGadgets.ID,value=Dist.CLIENT)
    public static final class Hands{
        static boolean renderingSkin;
        @SubscribeEvent public static void hand(net.neoforged.neoforge.client.event.RenderHandEvent e){if(!gauntlet(e.getItemStack()))return;var mc=Minecraft.getInstance();e.setCanceled(true);((witchinggadgets.mixin.ItemInHandInvoker)mc.gameRenderer.itemInHandRenderer).wg$renderPlayerArm(e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight(),e.getEquipProgress(),e.getSwingProgress(),ManoplaRendering.arm(mc.player,e.getHand()));}
        @SubscribeEvent public static void arm(net.neoforged.neoforge.client.event.RenderArmEvent e){if(renderingSkin)return;var p=e.getPlayer();InteractionHand hand=e.getArm()==p.getMainArm()?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;if(!gauntlet(p.getItemInHand(hand)))return;e.setCanceled(true);var renderer=(PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p);renderingSkin=true;try{if(e.getArm()==HumanoidArm.RIGHT)renderer.renderRightHand(e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight(),p);else renderer.renderLeftHand(e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight(),p);}finally{renderingSkin=false;}e.getPoseStack().pushPose();(e.getArm()==HumanoidArm.RIGHT?renderer.getModel().rightArm:renderer.getModel().leftArm).translateAndRotate(e.getPoseStack());mesh(e.getArm(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());e.getPoseStack().popPose();}
    }
}

