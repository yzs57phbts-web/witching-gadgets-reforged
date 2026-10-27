package witchinggadgets;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;
import thaumcraft.research.ResearchManager;
public final class DynamicRecipes implements DynamicArcaneRecipe {
    public static void init(){ArcaneCrafting.registerDynamic(new DynamicRecipes());}
    @Override public boolean matches(List<ItemStack> grid,Player p){
        List<ItemStack> list=grid.stream().filter(s->!s.isEmpty()).toList();
        if(list.size()==2&&ResearchManager.isComplete(p,"CLOAKKAMA")&&list.stream().anyMatch(s->s.is(Content.tc("mundane_belt")))&&list.stream().anyMatch(s->s.getItem() instanceof Gear g&&g.kind.contains("cloak")||s.is(Content.item("spectral_mantle"))||s.is(Content.item("mantle_of_the_raven"))))return true;
        if(!ResearchManager.isComplete(p,"SCANCAMERA"))return false;int paper=0,ink=0,plate=0;
        for(ItemStack s:list)if(s.is(Content.item("iconographic_plate"))&&Data.get(s).contains("PhotoKey"))plate++;else if(s.is(Items.PAPER))paper++;else if(s.is(Items.INK_SAC)||s.is(Items.BLACK_DYE))ink++;else return false;
        return plate==1&&paper>0&&paper==ink;
    }
    @Override public ItemStack result(List<ItemStack> grid){
        for(ItemStack s:grid)if(s.is(Content.item("iconographic_plate"))){var result=Content.stack("developed_iconograph");int papers=(int)grid.stream().filter(v->v.is(Items.PAPER)).count();result.setCount(papers);Data.put(result,Data.get(s));return result;}
        for(ItemStack s:grid)if(s.getItem() instanceof Gear g){String name=switch(g.kind){case "cloak"->"kama";case "spectral_mantle"->"spectral_kama";case "cloak_of_voluminous_pockets"->"kama_of_voluminous_pockets";case "wolven_cloak"->"wolven_kama";case "mantle_of_the_raven"->"kama_of_the_raven";default->null;};if(name!=null){var result=new ItemStack(Content.item(name));result.applyComponents(s.getComponents());result.setCount(1);return result;}}
        return ItemStack.EMPTY;
    }
    @Override public AspectList aspects(List<ItemStack> grid){int paper=(int)grid.stream().filter(s->s.is(Items.PAPER)).count();return paper>0?new AspectList().add(Aspect.AIR,5*paper).add(Aspect.WATER,5*paper).add(Aspect.ORDER,5*paper):new AspectList().add(Aspect.AIR,5).add(Aspect.ORDER,5);}
}
