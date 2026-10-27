package witchinggadgets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
public final class Compat {
    public static boolean twilight(){return ModList.get().isLoaded("twilightforest");}
    public static boolean enabled(String mod){return mod.isEmpty()||ModList.get().isLoaded(mod);}
    public static String name(ItemStack s){return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();}
    public static boolean raven(String name){return name.contains("raven");}
    public static boolean visible(String name){if(name.startsWith("native_")&&name.endsWith("_cluster")){String metal=name.substring(7,name.length()-8);return thaumcraft.crafting.TcIngredient.tag("c:ores/"+metal).exists()&&thaumcraft.crafting.TcIngredient.tag("c:ingots/"+metal).exists();}return !raven(name)||twilight();}
}
