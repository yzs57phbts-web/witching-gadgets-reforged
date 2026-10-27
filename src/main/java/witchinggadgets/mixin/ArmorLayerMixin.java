package witchinggadgets.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import witchinggadgets.Events;
@Mixin(value=HumanoidArmorLayer.class,remap=false)
public final class ArmorLayerMixin {
    @org.spongepowered.asm.mixin.Unique private LivingEntity wg$wearer;
    @Inject(method="renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",at=@At("HEAD"),cancellable=true)
    private void hideEnchantedArmor(PoseStack pose,MultiBufferSource buffers,LivingEntity wearer,EquipmentSlot slot,int light,HumanoidModel<?> model,float swing,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci){wg$wearer=wearer;var viewer=Minecraft.getInstance().player;if(witchinggadgets.EquipmentRules.hidden(wearer.getItemBySlot(slot),wearer,viewer)||witchinggadgets.SpectralRendering.hidden(wearer,viewer))ci.cancel();}
    @Inject(method="renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",at=@At("HEAD"),cancellable=true)
    private void spectralArmor(PoseStack pose,MultiBufferSource buffers,int light,net.minecraft.client.model.Model model,int color,net.minecraft.resources.ResourceLocation texture,CallbackInfo ci){if(wg$wearer!=null&&witchinggadgets.Gear.spectral(wg$wearer)){model.renderToBuffer(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texture)),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,witchinggadgets.SpectralRendering.tint(wg$wearer,Minecraft.getInstance().player));ci.cancel();}}
}

