package witchinggadgets;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import thaumcraft.platform.*;
import thaumcraft.aspect.Aspect;
import thaumcraft.wand.VisDiscountGear;
import thaumcraft.item.gear.*;
public final class RobeItem extends ArmorItem implements VisDiscountGear,Repairable,RunicArmor {
    static final TcArmorMaterial MATERIAL=new TcArmorMaterial(25,2,4,3,2,25,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,TagKey.create(Registries.ITEM,WitchingGadgets.id("robe_repair")),WitchingGadgets.id("advanced_robes"));
    final boolean legs;
    static final net.neoforged.neoforge.registries.DeferredHolder<ArmorMaterial,ArmorMaterial> MODERN=Content.ARMOR_MATERIALS.register("advanced_robes",()->new ArmorMaterial(java.util.Map.of(Type.HELMET,2,Type.CHESTPLATE,4,Type.LEGGINGS,3,Type.BOOTS,2,Type.BODY,4),25,SoundEvents.ARMOR_EQUIP_LEATHER,()->net.minecraft.world.item.crafting.Ingredient.of(MATERIAL.repairItems()),java.util.List.of(new ArmorMaterial.Layer(WitchingGadgets.id("advanced_robes"),"",true),new ArmorMaterial.Layer(WitchingGadgets.id("advanced_robes"),"_overlay",false)),0,0));
    public RobeItem(String kind,Properties p){super(MODERN,kind.equals("bewitched_leggings")?Type.LEGGINGS:Type.CHESTPLATE,p);legs=kind.equals("bewitched_leggings");}
    public static Properties properties(String kind,Properties p){return Registration.armor(p.rarity(Rarity.RARE).component(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(6961280,false)),MATERIAL,kind.equals("bewitched_leggings")?ArmorSlot.LEGGINGS:ArmorSlot.CHESTPLATE);}
    @Override public int getVisDiscount(ItemStack s,Player p,Aspect a){return legs?4:5;}
    @Override public int getRunicCharge(ItemStack s){return 0;}
}
