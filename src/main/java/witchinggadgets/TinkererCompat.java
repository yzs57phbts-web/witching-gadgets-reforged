package witchinggadgets;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import thaumcraft.aspect.*;
public final class TinkererCompat {
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void register(){
        if(!Compat.enabled("thaumictinkerer"))return;
        try{
            Class<?> manager=Class.forName("thaumictinkerer.enchanting.EnchantmentManager"),data=Class.forName("thaumictinkerer.enchanting.EnchantmentManager$Data");
            Map registry=(Map)manager.getMethod("data").invoke(null);var ctor=data.getConstructor(ResourceKey.class,ResourceLocation.class,boolean.class,AspectList.class,String.class);
            String[] names={"translucency","unveiling","stealth","backstabbing","throwoff"},research={"ENCH_INVISIBLEGEAR","ENCH_UNVEILING","ENCH_STEALTH","ENCH_BACKSTAB","ENCH_RIDEPROTECT"},textures={"invisgear","unveiling","stealth","backstab","rideprotect"};
            AspectList[] costs={new AspectList().add(Aspect.AIR,25).add(Aspect.ORDER,20).add(Aspect.ENTROPY,15),new AspectList().add(Aspect.AIR,25).add(Aspect.ORDER,20).add(Aspect.WATER,10),new AspectList().add(Aspect.AIR,10).add(Aspect.ORDER,20).add(Aspect.EARTH,10),new AspectList().add(Aspect.AIR,20).add(Aspect.ENTROPY,20).add(Aspect.FIRE,20),new AspectList().add(Aspect.AIR,20).add(Aspect.ENTROPY,20).add(Aspect.ORDER,20)};
            for(int i=0;i<names.length;i++){var key=ResourceKey.create(Registries.ENCHANTMENT,WitchingGadgets.id(names[i]));registry.put(key,ctor.newInstance(key,WitchingGadgets.id("textures/gui/research/icon_ench_"+textures[i]+".png"),false,costs[i],research[i]));}
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Thaumic Tinkerer enchanting API changed",e);}
    }
}
