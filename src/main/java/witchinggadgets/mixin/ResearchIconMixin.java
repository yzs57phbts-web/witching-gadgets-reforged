package witchinggadgets.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.research.ResearchBrowserScreen;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import witchinggadgets.Research;

/** In the WG tab, inaccessible item icons use the dark silhouette of the original browser. */
@Mixin(value=ResearchBrowserScreen.class,remap=false)
public abstract class ResearchIconMixin {
    @Inject(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"),cancellable=true,require=1,allow=1)
    private static void wg$lockedIcon(ItemStack stack,GuiGraphics graphics,int x,int y,ResearchItem item,int tint,CallbackInfo ci){
        var player=Minecraft.getInstance().player;
        if(stack.isEmpty()||!item.category().equals(Research.CATEGORY)||player==null||ResearchManager.hasRequisites(player,item.key()))return;
        graphics.flush();RenderSystem.setShaderColor(.1F,.1F,.1F,1F);
        try{graphics.renderItem(stack,x,y);graphics.flush();}finally{RenderSystem.setShaderColor(1F,1F,1F,1F);}
        ci.cancel();
    }
}
