package witchinggadgets;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/** Rules shared by the authoritative server and local movement prediction. */
public final class EquipmentRules {
    private EquipmentRules() {}
    public static void extraJump(Player p) {
        var jump=p.getEffect(MobEffects.JUMP);
        p.setDeltaMovement(p.getDeltaMovement().x,.42+(jump==null?0:(jump.getAmplifier()+1)*.1),p.getDeltaMovement().z);
        p.fallDistance=0;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent(p));
        p.hurtMarked=true;
    }
    public static boolean sameHeading(LivingEntity a,LivingEntity b) {
        var x=a.getLookAngle();var y=b.getLookAngle();
        return Math.signum(x.x)==Math.signum(y.x)&&Math.signum(x.z)==Math.signum(y.z);
    }
    public static boolean hidden(ItemStack stack,LivingEntity wearer,Player viewer) {
        int level=Events.enchant(stack,"translucency",wearer);
        boolean reveal=viewer!=null&&viewer!=wearer&&Events.enchant(viewer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),"unveiling",viewer)>0;
        return !reveal&&(level>1||level>0&&wearer.isInvisible());
    }
    public static boolean goggles(LivingEntity e) {
        var item=e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem();
        return item instanceof thaumcraft.node.Revealer||item instanceof thaumcraft.item.gear.Goggles;
    }
}
