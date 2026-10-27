package witchinggadgets;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

/** Native clusters plus the release's optional steel/tungsten/rutile integrations. */
public final class BlastRecipes {
    public record Recipe(ItemStack output,ItemStack bonus,int ticks,boolean special){}
    private static final Map<Item,Optional<Recipe>> CACHE=new HashMap<>();
    static void clear(){CACHE.clear();}
    static ItemStack tagged(String tag,int count){var stacks=thaumcraft.crafting.TcIngredient.tag("c:"+tag).displayStacks();return stacks.isEmpty()?ItemStack.EMPTY:stacks.getFirst().copyWithCount(count);}
    static boolean tagged(ItemStack s,String tag){return s.is(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c",tag)));}
    public static Recipe resolve(ItemStack s){return s.isEmpty()?null:CACHE.computeIfAbsent(s.getItem(),item->Optional.ofNullable(find(s))).orElse(null);}
    static Recipe find(ItemStack s){
        String name=Compat.name(s);
        if(name.endsWith("_cluster")){String metal=name.replace("native_","").replace("_cluster","");ItemStack out=metal.equals("cinnabar")?new ItemStack(Content.tc("quicksilver"),3):tagged("ingots/"+metal,3);if(out.isEmpty()){Item item=BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(metal+"_ingot"));if(item==Items.AIR)item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",metal+"_ingot"));if(item!=Items.AIR)out=new ItemStack(item,3);}return out.isEmpty()?null:new Recipe(out,tagged("nuggets/"+(metal.equals("cinnabar")?"quicksilver":metal),1),440,false);}
        if(s.is(Items.IRON_INGOT)||tagged(s,"ingots/iron")){var out=tagged("ingots/steel",1);if(!out.isEmpty())return new Recipe(out,ItemStack.EMPTY,1200,false);}
        if(s.is(Items.IRON_BLOCK)||tagged(s,"storage_blocks/iron")){var out=tagged("storage_blocks/steel",1);if(out.isEmpty())out=tagged("ingots/steel",9);if(!out.isEmpty())return new Recipe(out,ItemStack.EMPTY,1200,false);}
        for(String ore:List.of("tungsten","rutile"))if(tagged(s,"ores/"+ore)){var out=tagged("ingots/"+(ore.equals("rutile")?"titanium":ore),1);if(!out.isEmpty())return new Recipe(out,ItemStack.EMPTY,1200,true);}
        return null;
    }
}
