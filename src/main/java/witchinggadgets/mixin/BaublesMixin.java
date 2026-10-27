package witchinggadgets.mixin;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.platform.Baubles;
import witchinggadgets.Gear;
@Mixin(value=Baubles.class,remap=false)
public final class BaublesMixin {
    @Inject(method="stacks",at=@At("RETURN"),cancellable=true)
    private static void includeTravellerGear(Player p,CallbackInfoReturnable<List<ItemStack>> ci){var stacks=new ArrayList<>(ci.getReturnValue());for(var s:Gear.equipped(p))if(s.getItem() instanceof Gear&&stacks.stream().noneMatch(v->v==s))stacks.add(s);ci.setReturnValue(stacks);}
}
