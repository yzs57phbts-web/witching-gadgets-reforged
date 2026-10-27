package witchinggadgets.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.infusion.InfusionHooks;
import witchinggadgets.*;
@Mixin(value=InfusionHooks.class,remap=false)
public final class InfusionHooksMixin {
    @Inject(method="onInfusionCrafted",at=@At("TAIL"))
    private static void returnPowerlessPearls(ServerPlayer p,ItemStack result,CallbackInfo ci){
        int count=result.getItem() instanceof PrimordialArmor?3:result.getItem() instanceof PrimordialItem?2:result.is(Content.item("primordial_gauntlet"))?1:0;
        if(count==0||Data.get(result).getBoolean("WasCrafted"))return;Data.bool(result,"WasCrafted",true);var pearl=new ItemStack(Content.item("powerless_pearl"),count);if(!p.addItem(pearl))p.drop(pearl,false);
    }
}
