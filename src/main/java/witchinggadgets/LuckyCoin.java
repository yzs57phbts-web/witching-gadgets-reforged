package witchinggadgets;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import thaumcraft.research.ScanClientHooks;

/** The original WGBAUBLES_COIN result carries real Fortune I and Looting I enchantments. */
public final class LuckyCoin {
    private static RegistryAccess registries(){var server=ServerLifecycleHooks.getCurrentServer();if(server!=null)return server.registryAccess();var player=ScanClientHooks.localPlayer();return player==null?null:player.registryAccess();}
    public static void apply(ItemStack stack,int value){
        Data.integer(stack,"lucky",value);var access=registries();if(value<=0||access==null)return;
        var registry=access.registryOrThrow(Registries.ENCHANTMENT);var fortune=registry.getHolderOrThrow(Enchantments.FORTUNE);var looting=registry.getHolderOrThrow(Enchantments.LOOTING);
        EnchantmentHelper.updateEnchantments(stack,ench->{ench.set(fortune,Math.max(1,ench.getLevel(fortune)));ench.set(looting,Math.max(1,ench.getLevel(looting)));});
        Data.bool(stack,"LuckyEnchantmentsPort11",true);
    }
    public static boolean enchanted(ItemStack stack){var access=registries();if(access==null)return false;var registry=access.registryOrThrow(Registries.ENCHANTMENT);var enchantments=EnchantmentHelper.getEnchantmentsForCrafting(stack);return enchantments.getLevel(registry.getHolderOrThrow(Enchantments.FORTUNE))>=1&&enchantments.getLevel(registry.getHolderOrThrow(Enchantments.LOOTING))>=1;}
    public static boolean matches(ItemStack stack){var data=Data.get(stack);return data.getInt("lucky")>0&&!data.getBoolean("LuckyEnchantmentsPort11")||enchanted(stack);}
    public static void restoreInventory(ServerPlayer player){boolean changed=false;for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(stack.is(Content.tc("gold_coin"))&&Data.get(stack).getInt("lucky")>0&&!Data.get(stack).getBoolean("LuckyEnchantmentsPort11")){apply(stack,1);changed=true;}}if(changed)player.inventoryMenu.broadcastChanges();}
}
