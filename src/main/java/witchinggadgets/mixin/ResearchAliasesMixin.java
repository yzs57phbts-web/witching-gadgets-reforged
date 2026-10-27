package witchinggadgets.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.research.ResearchManager;
import witchinggadgets.Research;

/** Keep WG's aliases in sync, including completion paths without sibling processing. */
@Mixin(ResearchManager.class)
public abstract class ResearchAliasesMixin {
    @Inject(method="complete(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;Z)Z",at=@At("RETURN"),remap=false)
    private static void wg$completeAlias(ServerPlayer player,String key,boolean notify,CallbackInfoReturnable<Boolean> cir){Research.syncAliases(player);}
}
