package witchinggadgets;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.aspect.*;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class Gear extends Item implements ICurioItem,thaumcraft.item.gear.RunicArmor {
    public final String kind;
    public Gear(String kind,Properties p){super(p);this.kind=kind;}
    @Override public int getRunicCharge(ItemStack s){return 0;}
    public static void registerCurios(){Content.ITEM_MAP.forEach((n,v)->{if(v.get() instanceof Gear g)CuriosApi.registerCurio(g,g);});}
    public static List<ItemStack> equipped(LivingEntity entity){List<ItemStack> result=new ArrayList<>();CuriosApi.getCuriosInventory(entity).ifPresent(inv->{var h=inv.getEquippedCurios();for(int i=0;i<h.getSlots();i++)if(!h.getStackInSlot(i).isEmpty())result.add(h.getStackInSlot(i));});return result;}
    public static boolean has(LivingEntity e,String name){return equipped(e).stream().anyMatch(s->s.is(Content.item(name)));}
    public static boolean spectral(LivingEntity e){return equipped(e).stream().anyMatch(s->s.getItem() instanceof Gear g&&g.kind.startsWith("spectral_")&&Data.get(s).getBoolean("Active"));}
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return Compat.visible(kind)&&(kind.endsWith("_ring")||!has(context.entity(),kind));}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(!(context.entity() instanceof Player p))return;
        if(Compat.raven(kind)&&!p.onGround()){
            var v=p.getDeltaMovement();
            if(p.getAbilities().flying||thaumcraft.item.armor.Hover.isHovering(thaumcraft.item.armor.Hover.getHarness(p))){if(thaumcraft.item.armor.ArmorEvents.movingForward(p))p.moveRelative(.05F,new net.minecraft.world.phys.Vec3(0,0,1));p.setDeltaMovement(p.getDeltaMovement().multiply(1,1.125,1));}
            else if(v.y<0&&!Data.get(stack).getBoolean("NoGlide")){double factor=p.isShiftKeyDown()?.75:.5;double yaw=Math.toRadians(p.getYHeadRot()+90),boost=p.isShiftKeyDown()?.1:.05;p.setDeltaMovement(v.x+Math.cos(yaw)*boost,v.y*factor,v.z+Math.sin(yaw)*boost);}
            p.fallDistance=0;
        }
        if(!p.level().isClientSide()){
            if((kind.equals("wolven_cloak")||kind.equals("wolven_kama"))&&Data.get(stack).contains("WolfPotion")){int amp=Data.get(stack).getInt("WolfPotion");for(var effect:List.of(MobEffects.DAMAGE_BOOST,MobEffects.MOVEMENT_SPEED,MobEffects.DAMAGE_RESISTANCE))p.addEffect(new MobEffectInstance(effect,60,amp));var tag=Data.get(stack);tag.remove("WolfPotion");Data.put(stack,tag);}
            if(kind.startsWith("spectral_")&&Data.get(stack).getBoolean("Active")&&p.tickCount%100==0&&!Recipes.spendVis(p,new AspectList().add(Aspect.AIR,1)))Data.bool(stack,"Active",false);
            if(kind.equals("knockback_shoulders"))modifier(p,Attributes.KNOCKBACK_RESISTANCE,"shoulder_knockback",.6);
            if(kind.equals("wolven_vambraces"))modifier(p,Attributes.ATTACK_DAMAGE,"vambrace_strength",2);
        }
        if(kind.equals("haste_vambraces")&&p.onClimbable()){if(p.horizontalCollision)p.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(0,.25,0));else if(!p.isShiftKeyDown())p.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(0,-.1875,0));}
        if(p.onGround())p.getPersistentData().remove("WGDoubleJump");
    }
    static void modifier(LivingEntity e,net.minecraft.core.Holder<Attribute> attr,String key,double amount){var instance=e.getAttribute(attr);var id=WitchingGadgets.id(key);if(instance!=null&&!instance.hasModifier(id))instance.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));}
    @Override public void onEquip(SlotContext context,ItemStack previous,ItemStack stack){if(context.entity() instanceof Player p)thaumcraft.item.gear.RunicShield.markDirty(p);if(kind.equals("crimson_title"))refreshTitle(context.entity());}
    static void refreshTitle(LivingEntity entity){if(entity instanceof net.minecraft.server.level.ServerPlayer player)player.server.execute(()->{player.refreshDisplayName();player.refreshTabListName();});}
    @Override public void onUnequip(SlotContext context,ItemStack next,ItemStack stack){
        if(context.entity() instanceof Player p)thaumcraft.item.gear.RunicShield.markDirty(p);
        if(kind.equals("crimson_title"))refreshTitle(context.entity());
        if(kind.equals("knockback_shoulders"))context.entity().getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(WitchingGadgets.id("shoulder_knockback"));
        if(kind.equals("wolven_vambraces"))context.entity().getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(WitchingGadgets.id("vambrace_strength"));
        if(kind.startsWith("spectral_"))Data.bool(stack,"Active",false);
    }
    public static void activate(ItemStack s,Player p){
        if(!(s.getItem() instanceof Gear g))return;
        if(g.kind.startsWith("spectral_")){boolean active=!Data.get(s).getBoolean("Active");if(!Recipes.spendVis(p,new AspectList().add(Aspect.AIR,1)))return;Data.bool(s,"Active",active);if(active)for(var mob:p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(16)))if(mob.getTarget()==p&&!seesSpectral(mob))mob.setTarget(null);p.displayClientMessage(Component.translatable("wg.spectral",Component.translatable(active?"wg.state.on":"wg.state.off")),true);}
        else if(Compat.raven(g.kind))Data.bool(s,"NoGlide",!Data.get(s).getBoolean("NoGlide"));
        else if(g.kind.contains("voluminous"))BagItem.open(p,s,27);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){if(GearAbilities.activeGear(this))text.add(Component.translatable("wg.tooltip.ability"));if(kind.equals("crimson_title")&&Data.get(s).contains("Title"))text.add(Component.translatable(Data.get(s).getString("Title")));}
    static boolean seesSpectral(LivingEntity mob){var helmet=mob.getItemBySlot(EquipmentSlot.HEAD);return mob instanceof net.minecraft.world.entity.monster.Spider||!(mob instanceof net.minecraft.world.entity.PathfinderMob)||mob instanceof thaumcraft.entity.boss.CultistLeader||mob instanceof thaumcraft.entity.EldritchMob||mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss||mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon||EquipmentRules.goggles(mob);}
}
