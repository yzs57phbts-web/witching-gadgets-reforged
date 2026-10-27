package witchinggadgets;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public final class DyeRecipe extends CustomRecipe {
    // EntitySheep.fleeceColorTable in Minecraft 1.7.10, used by all three original WG dye recipes.
    private static final float[][] LEGACY_DYES={{1,1,1},{.85F,.5F,.2F},{.7F,.3F,.85F},{.4F,.6F,.85F},{.9F,.9F,.2F},{.5F,.8F,.1F},{.95F,.5F,.65F},{.3F,.3F,.3F},{.6F,.6F,.6F},{.3F,.5F,.6F},{.5F,.25F,.7F},{.2F,.3F,.7F},{.4F,.3F,.2F},{.4F,.5F,.2F},{.6F,.2F,.2F},{.1F,.1F,.1F}};
    public DyeRecipe(CraftingBookCategory category){super(category);}
    static boolean valid(ItemStack s){return s.getItem() instanceof BagItem||s.getItem() instanceof Gear g&&(g.kind.contains("cloak")||g.kind.contains("mantle")||g.kind.contains("kama"))||s.getItem() instanceof RobeItem;}
    static boolean dyeable(ItemStack s){return s.getItem() instanceof BagItem||s.getItem() instanceof RobeItem||s.getItem() instanceof Gear g&&(g.kind.equals("cloak")||g.kind.equals("kama"));}
    static int originalColor(ItemStack s){if(s.has(DataComponents.DYED_COLOR))return s.get(DataComponents.DYED_COLOR).rgb();if(s.getItem() instanceof RobeItem)return 6961280;if(s.getItem() instanceof BagItem)return switch(Compat.name(s)){case "voidlinked_bag"->4737096;case "enderlinked_bag"->2967361;case "hungry_bag"->8737138;default->9073853;};return 2041173;}
    @Override public boolean matches(CraftingInput in,Level l){int bases=0,dyes=0;boolean bag=false;for(ItemStack s:in.items())if(!s.isEmpty()){if(valid(s)){if(!dyeable(s))return false;bases++;bag=s.getItem() instanceof BagItem;}else if(s.getItem() instanceof DyeItem)dyes++;else return false;}return bases==1&&(dyes>0||bag);}
    @Override public ItemStack assemble(CraftingInput in,HolderLookup.Provider r){
        ItemStack base=ItemStack.EMPTY;int red=0,green=0,blue=0,count=0,bright=0;
        for(ItemStack s:in.items())if(valid(s))base=s.copyWithCount(1);
        if(base.isEmpty())return base;
        {int c=originalColor(base);red+=c>>16&255;green+=c>>8&255;blue+=c&255;bright+=Math.max(c>>16&255,Math.max(c>>8&255,c&255));count++;}
        boolean dyed=false;
        for(ItemStack s:in.items())if(s.getItem() instanceof DyeItem dye){dyed=true;var rgb=LEGACY_DYES[dye.getDyeColor().getId()];int r0=(int)(rgb[0]*255),g0=(int)(rgb[1]*255),b0=(int)(rgb[2]*255);red+=r0;green+=g0;blue+=b0;bright+=Math.max(r0,Math.max(g0,b0));count++;}
        if(!dyed){if(base.getItem() instanceof RobeItem)base.set(DataComponents.DYED_COLOR,new DyedItemColor(6961280,false));else base.remove(DataComponents.DYED_COLOR);return base;}
        red/=count;green/=count;blue/=count;float maximum=Math.max(red,Math.max(green,blue)),average=(float)bright/count;if(maximum>0){red=(int)(red*average/maximum);green=(int)(green*average/maximum);blue=(int)(blue*average/maximum);}base.set(DataComponents.DYED_COLOR,new DyedItemColor(red<<16|green<<8|blue,true));return base;
    }
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=1;}
    @Override public RecipeSerializer<?> getSerializer(){return Content.DYE_RECIPE.get();}
}
