package witchinggadgets;
import com.google.gson.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;

public final class Recipes {
    public static final Map<String,List<TcRecipe>> BY_RESEARCH=new LinkedHashMap<>();
    public static void init(){
        DynamicRecipes.init();
        TcStack.registerDataHandler("wg.potion",(s,v)->s.set(DataComponents.POTION_CONTENTS,new PotionContents(switch(v){case "swiftness"->Potions.SWIFTNESS;case "long_invisibility"->Potions.LONG_INVISIBILITY;default->Potions.INVISIBILITY;})));
        TcStack.registerDataMatcher("wg.potion",(s,v)->s.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().map(p->p.is(ResourceLocation.withDefaultNamespace(v))).orElse(false));
        for(String key:List.of("mask","goggles","lucky")){
            TcStack.registerDataHandler("wg."+key,(s,v)->Data.integer(s,key,Integer.parseInt(v)));
            TcStack.registerDataMatcher("wg."+key,(s,v)->Data.get(s).getInt(key)==Integer.parseInt(v));
        }
        TcStack.registerDataHandler("wg.lucky",(s,v)->LuckyCoin.apply(s,Integer.parseInt(v)));
        TcStack.registerDataMatcher("wg.lucky",(s,v)->LuckyCoin.matches(s));
        // Static representative for the native book. Real photos are handled by DynamicRecipes.
        // Requiring recorded data also prevents blank plates from matching this fallback.
        TcStack.registerDataHandler("wg.photo",(s,v)->{var tag=Data.get(s);tag.putString("PhotoKey","minecraft:stone");tag.putString("PhotoKind","block");Data.put(s,tag);});
        TcStack.registerDataMatcher("wg.photo",(s,v)->Data.get(s).contains("PhotoKey"));
        register(new ShapelessArcaneRecipe("WG:SCANCAMERA_DEVELOP","SCANCAMERA",TcStack.withData("witchinggadgets:developed_iconograph",1,Map.of("wg.photo","recorded")),new AspectList().add(Aspect.AIR,5).add(Aspect.WATER,5).add(Aspect.ORDER,5),List.of(TcIngredient.of(TcStack.withData("witchinggadgets:iconographic_plate",1,Map.of("wg.photo","recorded"))),TcIngredient.of(TcStack.of("minecraft:paper")),TcIngredient.of(TcStack.of("minecraft:ink_sac")))));
        for(var element:PortData.read("recipes")){
            var o=element.getAsJsonObject();if(!Compat.enabled(PortData.str(o,"requires")))continue;
            String key="WG:"+PortData.str(o,"key"),research=PortData.str(o,"research"),type=PortData.str(o,"type");
            var aspects=PortData.aspects(o.getAsJsonObject("aspects"));TcRecipe r;
            switch(type){
                case "arcane":case "shapedore":{
                    List<String> pattern=PortData.strings(o,"pattern");var symbols=o.getAsJsonObject("symbols");List<TcIngredient> grid=new ArrayList<>();
                    for(String row:pattern)for(char c:row.toCharArray())grid.add(c==' '?TcIngredient.EMPTY:ingredient(symbols.getAsJsonObject(String.valueOf(c))));
                    r=type.equals("shapedore")?new CraftingRecipeRef(key,research,WitchingGadgets.id(PortData.str(o,"key").toLowerCase(Locale.ROOT)),output(o.getAsJsonObject("output"))):new ShapedArcaneRecipe(key,research,output(o.getAsJsonObject("output")),aspects,pattern.getFirst().length(),pattern.size(),grid);break;
                }
                case "shapelessarcane":r=new ShapelessArcaneRecipe(key,research,output(o.getAsJsonObject("output")),aspects,ingredients(o));break;
                case "shapelessore":r=new CraftingRecipeRef(key,research,WitchingGadgets.id(PortData.str(o,"key").toLowerCase(Locale.ROOT)),output(o.getAsJsonObject("output")));break;
                case "alchemy":r=new CrucibleRecipe(key,research,output(o.getAsJsonObject("output")),ingredient(o.getAsJsonObject("central")),aspects);break;
                case "infusion":r=new InfusionRecipe(key,research,output(o.getAsJsonObject("output")),PortData.num(o,"instability"),aspects,ingredient(o.getAsJsonObject("central")),ingredients(o));break;
                case "infusionenchantment":r=new InfusionEnchantmentRecipe(key,research,WitchingGadgets.id(PortData.str(o,"enchantment")),PortData.num(o,"instability"),aspects,ingredients(o));break;
                default:throw new IllegalStateException("Unknown legacy recipe "+type);
            }
            register(r);
        }
        register(new CompoundRecipe("WG:GEMCUTTING_BUILD","GEMCUTTING",new AspectList(),1,2,1,List.of(TcStack.of("witchinggadgets:gemcutters_tools"),TcStack.of("thaumcraft:table"))));
        List<TcStack> furnace=new ArrayList<>();
        for(int y=2;y>=0;y--)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++)furnace.add(TcStack.of(y==2?x==0&&z==0?"minecraft:lava_bucket":"minecraft:nether_brick_stairs":BuiltInRegistries.BLOCK.getKey(Multiblock.expectedBrick(y*9+(z+1)*3+x+1)).toString()));
        register(new CompoundRecipe("WG:INFERNALBLASTFURNACE_BUILD","INFERNALBLASTFURNACE",new AspectList().add(Aspect.FIRE,50).add(Aspect.EARTH,50).add(Aspect.ENTROPY,50),3,3,3,furnace));
    }
    static void register(TcRecipe r){TcRecipes.register(r);var list=BY_RESEARCH.computeIfAbsent(r.research(),k->new ArrayList<>());list.removeIf(old->old.key().equals(r.key()));list.add(r);}
    public static TcStack output(JsonObject o){
        String id=PortData.str(o,"id");if(id.startsWith("#"))throw new IllegalArgumentException("Tag cannot be output "+id);
        Map<String,String> data=new LinkedHashMap<>();if(o.has("data"))o.getAsJsonObject("data").entrySet().forEach(e->data.put(e.getKey(),e.getValue().getAsString()));
        return TcStack.withData(id,o.has("count")?o.get("count").getAsInt():1,data);
    }
    public static TcIngredient ingredient(JsonObject o){
        if(o.has("alternatives")){
            var base=o.deepCopy();base.remove("alternatives");var options=new ArrayList<TcIngredient>();options.add(ingredient(base));
            for(var alternative:o.getAsJsonArray("alternatives"))options.add(ingredient(alternative.getAsJsonObject()));
            return TcIngredient.anyOf(options.toArray(TcIngredient[]::new));
        }
        String fallbackTag=PortData.str(o,"fallbackTag");if(!fallbackTag.isEmpty()){var preferred=TcIngredient.tag(fallbackTag);if(preferred.exists())return preferred;}String id=PortData.str(o,"id");if(id.startsWith("#"))return TcIngredient.tag(id.substring(1));return TcIngredient.of(output(o));
    }
    private static List<TcIngredient> ingredients(JsonObject o){List<TcIngredient> result=new ArrayList<>();if(o.has("ingredients"))o.getAsJsonArray("ingredients").forEach(v->result.add(ingredient(v.getAsJsonObject())));return result;}
    public static void optional(){
        // The original chooses silver when an ingot is available, otherwise iron. Tags load after init.
        for(var element:PortData.read("recipes")){
            var o=element.getAsJsonObject();if(!PortData.str(o,"type").equals("infusion")||!o.has("ingredients"))continue;
            boolean conditional=false;for(var input:o.getAsJsonArray("ingredients"))conditional|=input.getAsJsonObject().has("fallbackTag");
            if(conditional)register(new InfusionRecipe("WG:"+PortData.str(o,"key"),PortData.str(o,"research"),output(o.getAsJsonObject("output")),PortData.num(o,"instability"),PortData.aspects(o.getAsJsonObject("aspects")),ingredient(o.getAsJsonObject("central")),ingredients(o)));
        }
        for(var element:PortData.read("content")){
            var o=element.getAsJsonObject();String id=PortData.str(o,"id");if(!PortData.str(o,"group").equals("ItemCluster"))continue;
            String metal=id.substring(7,id.length()-8);var ore=TcIngredient.tag("c:ores/"+metal);var ingot=TcIngredient.tag("c:ingots/"+metal);var nugget=TcIngredient.tag("c:nuggets/"+metal);
            if(ore.exists()&&ingot.exists())register(new CrucibleRecipe("WG:CLUSTER_"+metal,"METALLURGICPERFECTION_CLUSTERS",TcStack.of("witchinggadgets:"+id),ore,new AspectList().add(Aspect.METAL,1).add(Aspect.ORDER,1)));
            if(nugget.exists()&&ingot.exists()){var n=nugget.displayStacks().getFirst();register(new CrucibleRecipe("WG:TRANSMUTE_"+metal,"METALLURGICPERFECTION_TRANSMUTATION",TcStack.of(BuiltInRegistries.ITEM.getKey(n.getItem()).toString(),3),nugget,new AspectList().add(Aspect.METAL,2)));}
        }
        Research.refreshRecipePages();
    }
    public static boolean spendVis(net.minecraft.world.entity.player.Player p,AspectList cost){
        if(p.isCreative())return true;
        for(var stack:Gear.equipped(p))if(stack.getItem() instanceof thaumcraft.item.bauble.VisAmuletItem){
            boolean enough=true;for(var aspect:cost.getAspects())if(thaumcraft.wand.VisStorage.getVis(stack,aspect)<cost.getAmount(aspect)*100)enough=false;
            if(enough){for(var aspect:cost.getAspects())thaumcraft.wand.VisStorage.storeVis(stack,aspect,thaumcraft.wand.VisStorage.getVis(stack,aspect)-cost.getAmount(aspect)*100);return true;}
        }
        for(int i=p.getInventory().items.size()-1;i>=0;i--){var stack=p.getInventory().getItem(i);if(stack.getItem() instanceof thaumcraft.wand.WandItem wand&&wand.consumeAllVisCrafting(stack,p,cost,false))return wand.consumeAllVisCrafting(stack,p,cost,true);}
        var offhand=p.getOffhandItem();if(offhand.getItem() instanceof thaumcraft.wand.WandItem wand&&wand.consumeAllVisCrafting(offhand,p,cost,false))return wand.consumeAllVisCrafting(offhand,p,cost,true);
        return false;
    }
}
