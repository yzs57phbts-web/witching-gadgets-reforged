package witchinggadgets.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import witchinggadgets.*;

@Mixin(value=LivingEntityRenderer.class,remap=false)
public abstract class SpectralLayerMixin {
    @Unique private LivingEntity wg$spectralEntity;
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
    private void rememberSpectral(LivingEntity entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,CallbackInfo ci){wg$spectralEntity=entity;}
    @Inject(method="getRenderType",at=@At("HEAD"),cancellable=true)
    private void spectralBody(LivingEntity entity,boolean visible,boolean translucent,boolean glowing,CallbackInfoReturnable<RenderType> ci){
        if(!Gear.spectral(entity))return;
        if(SpectralRendering.hidden(entity,Minecraft.getInstance().player)){ci.setReturnValue(null);return;}
        @SuppressWarnings("unchecked") var renderer=(net.minecraft.client.renderer.entity.EntityRenderer<LivingEntity>)(Object)this;
        ci.setReturnValue(RenderType.entityTranslucent(renderer.getTextureLocation(entity)));
    }
    @ModifyArg(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),index=4)
    private int spectralColor(int color){return wg$spectralEntity!=null&&Gear.spectral(wg$spectralEntity)?SpectralRendering.tint(wg$spectralEntity,Minecraft.getInstance().player):color;}
}
