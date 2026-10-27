package witchinggadgets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
public final class Data {
    public static CompoundTag get(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void put(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static void integer(ItemStack s,String key,int value){var t=get(s);t.putInt(key,value);put(s,t);}
    public static void bool(ItemStack s,String key,boolean value){var t=get(s);t.putBoolean(key,value);put(s,t);}
    public static void string(ItemStack s,String key,String value){var t=get(s);t.putString(key,value);put(s,t);}
}
