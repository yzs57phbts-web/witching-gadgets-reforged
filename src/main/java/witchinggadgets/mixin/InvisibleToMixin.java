package witchinggadgets.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import witchinggadgets.Events;
@Mixin(value=Entity.class,remap=false)
public final class InvisibleToMixin {
    @Inject(method="isInvisibleTo",at=@At("HEAD"),cancellable=true)
    private void revealInvisible(Player viewer,CallbackInfoReturnable<Boolean> ci){if(Events.enchant(viewer.getItemBySlot(EquipmentSlot.HEAD),"unveiling",viewer)>0)ci.setReturnValue(false);}
}
