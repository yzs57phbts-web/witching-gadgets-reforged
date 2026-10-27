package witchinggadgets.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import witchinggadgets.EquipmentRules;

/** Only explicitly WG-enchanted held equipment; leaves ordinary wand rendering alone. */
@Mixin(value=ItemRenderer.class,remap=false)
public final class TranslucentItemMixin {
    @Inject(method="renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",at=@At("HEAD"),cancellable=true)
    private void enchantedHeldItem(LivingEntity wearer,ItemStack item,ItemDisplayContext context,boolean left,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,net.minecraft.world.level.Level level,int light,int overlay,int seed,CallbackInfo ci){
        if(wearer!=null&&(context.firstPerson()||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)&&EquipmentRules.hidden(item,wearer,Minecraft.getInstance().player))ci.cancel();
    }
}
