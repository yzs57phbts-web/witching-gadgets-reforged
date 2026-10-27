package witchinggadgets;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import thaumcraft.crafting.*;

/** WG's released enchantment predicates, including undamageable traveller equipment. */
public final class EquipmentEnchantments {
    private EquipmentEnchantments() {}
    public static boolean matches(Level level,InfusionEnchantmentRecipe recipe,List<ItemStack> inputs,ItemStack central,Player player) {
        if(central.isEmpty()||central.getCount()!=1||!thaumcraft.research.ResearchGate.isComplete(player,recipe.research()))return false;
        var holder=InfusionCrafting.enchantment(level,recipe.enchantment());
        if(holder==null||!holder.value().isSupportedItem(central))return false;
        var existing=EnchantmentHelper.getEnchantmentsForCrafting(central);
        for(var other:existing.keySet()){
            if(other.equals(holder)){if(existing.getLevel(other)>=holder.value().getMaxLevel())return false;}
            else if(!Enchantment.areCompatible(holder,other))return false;
        }
        return InfusionCrafting.matchesComponents(recipe.components(),inputs);
    }
}
