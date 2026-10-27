package witchinggadgets.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=ItemInHandRenderer.class,remap=false)
public interface ItemInHandInvoker {
    @Invoker("renderPlayerArm") void wg$renderPlayerArm(PoseStack pose,MultiBufferSource buffers,int light,float equip,float swing,HumanoidArm arm);
}
