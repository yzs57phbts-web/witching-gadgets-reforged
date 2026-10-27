package witchinggadgets;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.platform.*;
import thaumcraft.item.gear.*;
import thaumcraft.wand.VisDiscountGear;
import thaumcraft.aspect.Aspect;
public final class PrimordialArmor extends thaumcraft.item.armor.FortressArmorItem implements Repairable,RunicArmor,VisDiscountGear {
    static final TcArmorMaterial MATERIAL=new TcArmorMaterial(40,3,7,6,3,30,SoundEvents.ARMOR_EQUIP_IRON,0,0,TagKey.create(Registries.ITEM,WitchingGadgets.id("primordial_repair")),WitchingGadgets.id("primordial"));
    static ArmorSlot slot(String s){return s.endsWith("helm")?ArmorSlot.HELMET:s.endsWith("cuirass")?ArmorSlot.CHESTPLATE:s.endsWith("greaves")?ArmorSlot.LEGGINGS:ArmorSlot.BOOTS;}
    public PrimordialArmor(String id,Properties p){super(MATERIAL,slot(id),p);}
    @Override public boolean showNodes(ItemStack s,net.minecraft.world.entity.LivingEntity e){return getEquipmentSlot()==net.minecraft.world.entity.EquipmentSlot.HEAD&&Data.get(s).getInt("goggles")>0;}
    @Override public boolean showIngamePopups(ItemStack s,net.minecraft.world.entity.LivingEntity e){return showNodes(s,e);}
    static Properties properties(String id,Properties p){Registration.armorMaterial(MATERIAL);return Registration.armor(p.rarity(Rarity.EPIC),MATERIAL,slot(id));}
    @Override public int getRunicCharge(ItemStack s){return 0;}
    @Override public int getVisDiscount(ItemStack s,Player p,Aspect a){return 0;}
    @Override public Protection protection(ItemStack stack,net.minecraft.world.entity.LivingEntity wearer,net.minecraft.world.damagesource.DamageSource source,net.minecraft.world.entity.EquipmentSlot slot){
        int mode=Math.floorMod(Data.get(stack).getInt("Affinity"),6),priority=0;
        double defense=thaumcraft.item.armor.ArmorSupport.defense(stack,slot),ratio=defense/25;
        boolean magic=source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)||source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
        boolean fire=source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)||source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        if(magic){priority=1;ratio=defense/35;}
        else if(fire){priority=1;ratio=mode==2?.75:defense/20;if(mode==2&&source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE))wearer.clearFire();}
        else if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)){priority=mode==1?1:0;ratio=mode==1?defense/50:0;}
        if(wearer instanceof Player){double set=.875;for(var piece:wearer.getArmorSlots())if(piece.getItem() instanceof PrimordialArmor){set+=.125;if(thaumcraft.item.armor.FortressArmorItem.hasMask(piece))set+=.05;}ratio*=set;}
        return new Protection(priority,ratio,(stack.getMaxDamage()+1.0-stack.getDamageValue())/25);
    }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack){var aspect=GemItem.PRIMALS[Math.floorMod(Data.get(stack).getInt("Affinity"),6)];return super.getName(stack).copy().append(net.minecraft.network.chat.Component.literal(" — ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)).append(net.minecraft.network.chat.Component.translatable("tc.aspect."+aspect.getTag()).withStyle(style->style.withColor(aspect.getColor())));}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<net.minecraft.network.chat.Component> text,TooltipFlag f){super.appendHoverText(s,c,text,f);text.add(net.minecraft.network.chat.Component.translatable("wg.tooltip.affinity",net.minecraft.network.chat.Component.translatable("tc.aspect."+GemItem.PRIMALS[Math.floorMod(Data.get(s).getInt("Affinity"),6)].getTag())));text.add(net.minecraft.network.chat.Component.translatable("wg.tooltip.affinity_controls",net.minecraft.network.chat.Component.keybind("key.witchinggadgets.ability"),net.minecraft.network.chat.Component.keybind("key.witchinggadgets.select")));}
    @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(l.isClientSide()||!s.isDamaged())return;int repair=e.tickCount%40==0?1:0;if(e.tickCount%20==0&&e instanceof net.minecraft.world.entity.LivingEntity wearer)for(var equipped:wearer.getArmorSlots())if(equipped==s){repair++;break;}if(repair>0)s.setDamageValue(Math.max(0,s.getDamageValue()-repair));}
}
