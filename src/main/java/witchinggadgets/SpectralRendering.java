package witchinggadgets;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/** Spectral concealment is equipment state, not a vanilla invisibility potion. */
public final class SpectralRendering {
    public static boolean hidden(LivingEntity wearer,Player viewer){return Gear.spectral(wearer)&&viewer!=wearer&&(viewer==null||!Gear.spectral(viewer)&&!EquipmentRules.goggles(viewer)&&Events.enchant(viewer.getItemBySlot(EquipmentSlot.HEAD),"unveiling",viewer)==0);}
    public static int tint(LivingEntity wearer,Player viewer){
        if(viewer==wearer)return 0x807F7F7F;
        return viewer!=null&&Events.enchant(viewer.getItemBySlot(EquipmentSlot.HEAD),"unveiling",viewer)>0?0xBFFFFFFF:0x803F3F3F;
    }
}
