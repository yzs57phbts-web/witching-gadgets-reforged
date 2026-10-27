package witchinggadgets;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Traveller's Gear's per-item activation, with Curios and modern hand slots. */
public final class GearAbilities {
    public record Entry(int action,ItemStack stack,String slot) {}
    public static boolean activeGear(Gear gear){return gear.kind.startsWith("spectral_")||gear.kind.contains("voluminous")||Compat.raven(gear.kind);}
    public static List<Entry> entries(Player p){
        var result=new ArrayList<Entry>();
        for(var stack:Gear.equipped(p))if(stack.getItem() instanceof Gear gear&&activeGear(gear))result.add(new Entry(1000+BuiltInRegistries.ITEM.getId(stack.getItem()),stack,"curio"));
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND)){
            var stack=p.getItemBySlot(slot);if(stack.getItem() instanceof PrimordialArmor||!slot.isArmor()&&stack.getItem() instanceof PrimordialItem)result.add(new Entry(100+slot.ordinal(),stack,slot.name().toLowerCase(Locale.ROOT)));
        }
        return result;
    }
    public static boolean activate(Player p,int action){
        for(var entry:entries(p))if(entry.action()==action){var stack=entry.stack();if(stack.getItem() instanceof Gear)Gear.activate(stack,p);else Data.integer(stack,"Affinity",(Math.floorMod(Data.get(stack).getInt("Affinity"),6)+1)%6);return true;}
        return false;
    }
    public static boolean quick(Player p){
        var held=p.getMainHandItem();
        if(held.getItem() instanceof PrimordialItem||held.getItem() instanceof PrimordialArmor||held.is(Content.item("primordial_gauntlet"))){int modes=held.is(Content.item("primordial_gauntlet"))?5:6;Data.integer(held,"Affinity",(Math.floorMod(Data.get(held).getInt("Affinity"),modes)+1)%modes);return true;}
        var armor=entries(p).stream().filter(entry->entry.stack().getItem() instanceof PrimordialArmor).toList();return armor.size()==1&&activate(p,armor.getFirst().action());
    }
}
