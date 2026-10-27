package witchinggadgets;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import thaumcraft.aspect.Aspect;

final class GemMaterials {
    static final String[] MATERIALS={"diamond","emerald","quartz","certus_quartz","topaz","amber","peridot","malachite","ruby","sapphire","tanzanite","primordial"};
    static String material(ItemStack stack){
        if(stack.isEmpty())return null;
        if(stack.is(Items.DIAMOND))return "diamond";if(stack.is(Items.EMERALD))return "emerald";if(stack.is(Items.QUARTZ))return "quartz";if(stack.is(Content.tc("amber")))return "amber";
        for(String name:MATERIALS)if(stack.is(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","gems/"+name))))return name;
        return null;
    }
    static boolean affinity(String material,Aspect aspect){return switch(material){
        case "certus_quartz"->aspect==Aspect.ORDER||aspect==Aspect.AIR;case "topaz"->aspect==Aspect.AIR||aspect==Aspect.FIRE;
        case "peridot"->aspect==Aspect.EARTH;case "malachite"->aspect==Aspect.EARTH||aspect==Aspect.WATER;
        case "ruby"->aspect==Aspect.FIRE;case "sapphire","tanzanite"->aspect==Aspect.WATER;case "primordial"->aspect.isPrimal();default->GemItem.affinity(material,aspect);
    };}
    static boolean aversion(String material,Aspect aspect){return switch(material){
        case "certus_quartz","peridot","malachite","sapphire","tanzanite"->aspect==Aspect.ENTROPY||aspect==Aspect.FIRE;
        case "topaz","ruby"->aspect==Aspect.ENTROPY||aspect==Aspect.WATER;default->GemItem.aversion(material,aspect);
    };}
}
