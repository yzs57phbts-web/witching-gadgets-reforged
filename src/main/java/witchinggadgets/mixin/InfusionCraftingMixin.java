package witchinggadgets.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.crafting.*;
import witchinggadgets.*;
@Mixin(value=InfusionCrafting.class,remap=false)
public final class InfusionCraftingMixin {
    @Inject(method="matches(Lnet/minecraft/world/level/Level;Lthaumcraft/crafting/InfusionEnchantmentRecipe;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z",at=@At("HEAD"),cancellable=true)
    private static void originalWgEnchantmentRules(Level level,InfusionEnchantmentRecipe recipe,java.util.List<ItemStack> inputs,ItemStack central,net.minecraft.world.entity.player.Player player,CallbackInfoReturnable<Boolean> ci){if(recipe.enchantment().getNamespace().equals(WitchingGadgets.ID))ci.setReturnValue(EquipmentEnchantments.matches(level,recipe,inputs,central,player));}
    @Inject(method="result",at=@At("RETURN"),cancellable=true)
    private static void retainPrimordialHelmetData(Level level,int type,String key,ItemStack central,CallbackInfoReturnable<ItemStack> ci){
        if(!key.startsWith("WG:")||!(TcRecipes.get(key) instanceof InfusionRecipe recipe)||recipe.result().data().keySet().stream().noneMatch(k->k.equals("wg.mask")||k.equals("wg.goggles")))return;
        ItemStack result=central.copy();var data=Data.get(result);var infusion=new java.util.HashMap<>(thaumcraft.infusion.InfusionKeys.TAGS.getOrDefault(result,java.util.Map.of()));recipe.result().data().forEach((k,v)->{if(k.startsWith("wg.")){data.putInt(k.substring(3),Integer.parseInt(v));infusion.put(k.substring(3),v);}});Data.put(result,data);thaumcraft.infusion.InfusionKeys.TAGS.set(result,infusion);ci.setReturnValue(result);
    }
}
