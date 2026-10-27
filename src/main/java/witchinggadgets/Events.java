package witchinggadgets;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class Events {
    private static boolean cleaving;
    public static void init(){NeoForge.EVENT_BUS.register(Events.class);}
    static net.minecraft.network.chat.Component titled(Player player,net.minecraft.network.chat.Component name){for(var item:Gear.equipped(player))if(item.is(Content.item("crimson_title"))&&Data.get(item).contains("Title"))return net.minecraft.network.chat.Component.translatable(Data.get(item).getString("Title")).withStyle(net.minecraft.ChatFormatting.DARK_RED).append(" ").append(name);return name;}
    @SubscribeEvent public static void name(PlayerEvent.NameFormat e){e.setDisplayname(titled(e.getEntity(),e.getDisplayname()));}
    @SubscribeEvent public static void tabName(PlayerEvent.TabListNameFormat e){e.setDisplayName(titled(e.getEntity(),e.getDisplayName()==null?e.getEntity().getName():e.getDisplayName()));}
    @SubscribeEvent public static void tags(net.neoforged.neoforge.event.TagsUpdatedEvent e){BlastRecipes.clear();Recipes.optional();}
    @SubscribeEvent public static void researchAliases(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player){Research.syncAliases(player);LuckyCoin.restoreInventory(player);}}
    @SubscribeEvent public static void legacyCoins(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e){if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player&&player.tickCount%20==0)LuckyCoin.restoreInventory(player);}
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent e){if(e.getCrafting().is(Content.item("developed_iconograph")))for(int i=0;i<Math.min(9,e.getInventory().getContainerSize());i++){var s=e.getInventory().getItem(i);if(s.is(Content.item("iconographic_plate")))s.grow(1);}}
    @SubscribeEvent public static void loot(net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent e){if(e.getEnchantment().is(Enchantments.LOOTING)&&e.getDamageSource().getEntity() instanceof Player p&&Gear.has(p,"covetous_coin_ring"))e.setEnchantmentLevel(e.getEnchantmentLevel()+2);}
    @SubscribeEvent public static void fortune(net.neoforged.neoforge.event.enchanting.EnchantedBlockLootEvent e){if(!e.getEnchantment().is(Enchantments.FORTUNE))return;var player=HarvestContext.player();if(player!=null&&Gear.has(player,"covetous_coin_ring"))e.setEnchantmentLevel(e.getEnchantmentLevel()+2);}
    @SubscribeEvent public static void riding(net.neoforged.neoforge.event.entity.EntityMountEvent e){if(e.isMounting()&&e.getEntityBeingMounted() instanceof LivingEntity wearer&&enchant(wearer.getItemBySlot(EquipmentSlot.HEAD),"throwoff",wearer)>0){e.setCanceled(true);if(e.getEntityMounting() instanceof LivingEntity rider){rider.hurt(wearer.damageSources().mobAttack(wearer),1);rider.push(wearer.getRandom().nextFloat()*.4,.1,wearer.getRandom().nextFloat()*.4);}}}
    @SubscribeEvent public static void pickup(ItemEntityPickupEvent.Pre event){
        var player=event.getPlayer();var entity=event.getItemEntity();if(entity.hasPickUpDelay()||entity.getItem().isEmpty()||entity.getTarget()!=null&&!entity.getTarget().equals(player.getUUID()))return;
        for(var bag:player.getInventory().items){if(!(bag.getItem() instanceof BagItem type))continue;
            if(type.kind.equals("voidlinked_bag")){
                boolean matched=bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().anyMatch(sample->ItemStack.isSameItem(sample,entity.getItem()));if(!matched)continue;
                var tags=thaumcraft.aspect.AspectHelper.getBonusTags(entity.getItem());if(tags!=null){var primals=thaumcraft.aspect.AspectHelper.reduceToPrimals(tags);if(!primals.isEmpty()){var aspect=primals.getAspects().get(player.getRandom().nextInt(primals.size()));for(int i=0;i<9;i++){var wand=player.getInventory().getItem(i);if(wand.getItem() instanceof thaumcraft.wand.WandItem&&thaumcraft.wand.Wands.addVis(wand,aspect,primals.getAmount(aspect),false)>0){thaumcraft.wand.Wands.addVis(wand,aspect,primals.getAmount(aspect),true);break;}}}}
                entity.discard();event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);return;
            }
            if(type.kind.equals("hungry_bag")&&BagItem.canStore(entity.getItem())){
                var storage=BagItem.liveStorage(player,bag,18);var rest=entity.getItem().copy();for(int i=0;i<18&&!rest.isEmpty();i++){var existing=storage.getItem(i);if(existing.isEmpty()){storage.setItem(i,rest.copy());rest=ItemStack.EMPTY;}else if(ItemStack.isSameItemSameComponents(existing,rest)){int moved=Math.min(rest.getCount(),existing.getMaxStackSize()-existing.getCount());existing.grow(moved);rest.shrink(moved);storage.setChanged();}}
                if(rest.isEmpty()){entity.discard();event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);return;}// ItemEntity.playerTouch already holds this stack reference before Pre fires.
                entity.getItem().setCount(rest.getCount());
            }
        }
    }
    public static int enchant(ItemStack s,String name,LivingEntity e){var registry=e.registryAccess().registryOrThrow(Registries.ENCHANTMENT);var key=ResourceKey.create(Registries.ENCHANTMENT,WitchingGadgets.id(name));return registry.getHolder(key).map(v->s.getEnchantmentLevel(v)).orElse(0);}
    @SubscribeEvent public static void target(LivingChangeTargetEvent e){
        if(e.getNewAboutToBeSetTarget() instanceof Player p){if(Gear.spectral(p)&&!Gear.seesSpectral(e.getEntity())){e.setNewAboutToBeSetTarget(null);return;}
            if(p.isShiftKeyDown()&&e.getEntity() instanceof net.minecraft.world.entity.PathfinderMob){int strength=enchant(p.getItemBySlot(EquipmentSlot.FEET),"stealth",p)+enchant(p.getItemBySlot(EquipmentSlot.LEGS),"stealth",p);var look=e.getEntity().getLookAngle();var heading=p.getLookAngle();double chance=strength*.1-(Math.signum(look.x)!=Math.signum(heading.x)||Math.signum(look.z)!=Math.signum(heading.z)?.1:0);if(strength>0&&(p.getRandom().nextFloat()<chance||!p.level().getEntitiesOfClass(net.minecraft.world.entity.PathfinderMob.class,p.getBoundingBox().inflate(5),mob->!Gear.seesSpectral(mob)&&mob.getTarget()==p).isEmpty()))e.setNewAboutToBeSetTarget(null);}
        }
    }
    @SubscribeEvent public static void breakBlock(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e){var player=e.getPlayer();if(Gear.spectral(player)){e.setCanceled(true);return;}if(player.getMainHandItem().getItem() instanceof PrimordialItem&&!player.getPersistentData().getBoolean("WGAreaMining")){var hit=player.pick(6,1,false);if(hit instanceof net.minecraft.world.phys.BlockHitResult block&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&block.getBlockPos().equals(e.getPos())){player.getPersistentData().putLong("WGMiningPos",e.getPos().asLong());player.getPersistentData().putInt("WGMiningFace",block.getDirection().get3DDataValue());}}}
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem e){if(Gear.spectral(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void interactEntity(PlayerInteractEvent.EntityInteract e){if(Gear.spectral(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific e){if(Gear.spectral(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(Gear.spectral(e.getEntity())){e.setCanceled(true);return;}e.getEntity().getPersistentData().putInt("WGNativeAttackTick",e.getEntity().tickCount);e.getEntity().getPersistentData().putFloat("WGNativeCritical",1);}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void critical(CriticalHitEvent e){e.getEntity().getPersistentData().putFloat("WGNativeCritical",e.isCriticalHit()?e.getDamageMultiplier():1);}
    static float cleaveDamage(Player p,ItemStack weapon,LivingEntity other,float fallback){
        if(!(p.level() instanceof net.minecraft.server.level.ServerLevel level)||!p.getPersistentData().contains("WGNativeAttackTick")||p.getPersistentData().getInt("WGNativeAttackTick")!=p.tickCount)return fallback;
        float base=(float)p.getAttributeValue(Attributes.ATTACK_DAMAGE),charge=p.getAttackStrengthScale(.5F);
        float enchant=EnchantmentHelper.modifyDamage(level,weapon,other,p.damageSources().playerAttack(p),base)-base;
        return (base*(.2F+charge*charge*.8F)+weapon.getItem().getAttackDamageBonus(other,base,p.damageSources().playerAttack(p)))*p.getPersistentData().getFloat("WGNativeCritical")+enchant*charge;
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock e){if(Gear.spectral(e.getEntity())){e.setCanceled(true);return;}if(e.getItemStack().getItem() instanceof thaumcraft.wand.WandItem&&Multiblock.tryForm(e.getLevel(),e.getPos(),e.getEntity(),e.getItemStack(),e.getFace())){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}}
    @SubscribeEvent public static void terraGuard(LivingDamageEvent.Post e){
        if(e.getNewDamage()<=0||!(e.getEntity() instanceof Player p)||p.level().isClientSide())return;
        var weapon=p.getMainHandItem();
        if(!(weapon.getItem() instanceof PrimordialItem tool)||!tool.kind.equals("primordial_claymore")||Data.get(weapon).getInt("Affinity")!=1||p.getRandom().nextInt(4)!=0)return;
        var resistance=p.getEffect(MobEffects.DAMAGE_RESISTANCE);int duration=resistance==null?0:resistance.getDuration(),amplifier=resistance==null?-1:resistance.getAmplifier();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,Math.max(duration,Math.min(80,duration+30)),Math.max(amplifier,Math.min(2,amplifier+1))));
    }
    @SubscribeEvent public static void hurt(LivingIncomingDamageEvent e){
        LivingEntity victim=e.getEntity();Entity attacker=e.getSource().getDirectEntity();float originalAmount=e.getAmount();
        if(e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)&&victim.hasEffect(StatusEffects.CINDER))e.setAmount(e.getAmount()*(2+victim.getEffect(StatusEffects.CINDER).getAmplifier()));
        if(e.getSource().getDirectEntity() instanceof Player p){
            var weapon=p.getMainHandItem();int backstab=enchant(weapon,"backstabbing",p);
            if(backstab>0&&EquipmentRules.sameHeading(victim,p))e.setAmount(e.getAmount()*(1+backstab*.2F+(victim instanceof Mob m&&m.getTarget()!=p?.4F:0)));
            if(weapon.getItem() instanceof PrimordialItem tool){
                if(tool.kind.equals("primordial_warhammer"))victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION,20,0));
                if(tool.kind.equals("primordial_greataxe"))for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){var armor=victim.getItemBySlot(slot);if(armor.getItem() instanceof ArmorItem)armor.hurtAndBreak(5,victim,slot);}
                if(tool.kind.equals("primordial_warhammer")&&(victim instanceof Slime||victim.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)))e.setAmount(e.getAmount()*2);
                if(tool.kind.equals("primordial_greataxe")){int pieces=0;for(var armor:victim.getArmorSlots())if(!armor.isEmpty())pieces++;e.setAmount(e.getAmount()*(1+pieces*.5F));}
                int affinity=Data.get(weapon).getInt("Affinity");
                switch(affinity){
                    case 0->{if(!cleaving){cleaving=true;try{for(LivingEntity other:p.level().getEntitiesOfClass(LivingEntity.class,victim.getBoundingBox().inflate(2),v->v!=victim&&v!=p&&!v.isAlliedTo(p))){boolean hit=other.hurt(p.damageSources().playerAttack(p),cleaveDamage(p,weapon,other,originalAmount));if(hit&&p.level() instanceof net.minecraft.server.level.ServerLevel level){EnchantmentHelper.doPostAttackEffects(level,other,p.damageSources().playerAttack(p));}}}finally{cleaving=false;}}}
                    case 2->{victim.igniteForSeconds(4);victim.addEffect(new MobEffectInstance(StatusEffects.CINDER,80,1));}
                    case 3->victim.addEffect(new MobEffectInstance(StatusEffects.DISSOLVE,80,2));
                    case 4->{}
                    case 5->{victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,0));victim.addEffect(new MobEffectInstance(MobEffects.HUNGER,120,0));}
                }
            }
        }
        if(victim instanceof Player p){
            for(var s:Gear.equipped(p))if(s.is(Content.item("wolven_cloak"))||s.is(Content.item("wolven_kama"))){
                Data.integer(s,"WolfPotion",1+(e.getAmount()>=8?1:0)+(e.getAmount()>=12?1:0));
            }
            for(ItemStack s:p.getArmorSlots())if(s.getItem() instanceof PrimordialArmor){
                switch(Data.get(s).getInt("Affinity")){
                    case 0->{if(e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile){projectile.setOwner(p);projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1));projectile.hurtMarked=true;e.setCanceled(true);}}
                    case 1->{}
                    case 2->{}
                    case 5->{if(attacker instanceof LivingEntity a&&p.getRandom().nextInt(4)==0){a.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,10,0));a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,10,3));}}
                }
            }
        }
    }
    @SubscribeEvent public static void drops(LivingDropsEvent e){
        var mob=e.getEntity();
        int titleLoot=0;if(e.getSource().getEntity() instanceof LivingEntity killer){var looting=killer.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING);titleLoot=killer.getMainHandItem().getEnchantmentLevel(looting)+(killer instanceof Player p&&Gear.has(p,"covetous_coin_ring")?2:0);}
        String title=mob instanceof thaumcraft.entity.boss.CultistLeader?"crimsonPraetor":mob instanceof thaumcraft.entity.monster.CultistKnight?"crimsonKnight":mob instanceof thaumcraft.entity.monster.CultistCleric?"crimsonCultist":null;
        if(title!=null&&(mob instanceof thaumcraft.entity.boss.CultistLeader?mob.getRandom().nextBoolean():mob.getRandom().nextInt(10)<1+titleLoot)){var drop=Content.stack("crimson_title");var tag=Data.get(drop);tag.putString("Title","wg.title."+title);Data.put(drop,tag);e.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),drop));}
        if(mob instanceof Wolf){int looting=titleLoot;int n=0;for(int i=0;i<2+Math.min(4,looting);i++)if(mob.getRandom().nextInt(Math.max(1,3-looting))==0)n++;if(n>0)e.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(Content.item("wolf_pelt"),n)));}
        if(mob.getPersistentData().getBoolean("WGCrystallize")){var essence=new ItemStack(Content.tc("crystallized_essence"));thaumcraft.aspect.AspectKeys.ASPECTS.set(essence,new thaumcraft.aspect.AspectList().add(thaumcraft.aspect.Aspect.LIFE,1));e.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),essence));}
        if(e.getSource().getDirectEntity() instanceof ServerPlayer p&&p.getMainHandItem().is(Content.item("primordial_claymore"))){
            var holder=p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING);int looting=p.getMainHandItem().getEnchantmentLevel(holder)+(Gear.has(p,"covetous_coin_ring")?2:0);
            if(looting>0&&p.getRandom().nextInt(6)<looting){Item head=mob instanceof WitherSkeleton?Items.WITHER_SKELETON_SKULL:mob instanceof Skeleton?Items.SKELETON_SKULL:mob instanceof Zombie?Items.ZOMBIE_HEAD:mob instanceof Creeper?Items.CREEPER_HEAD:mob instanceof Player?Items.PLAYER_HEAD:null;if(head!=null&&e.getDrops().stream().noneMatch(drop->drop.getItem().is(head))){var stack=new ItemStack(head);if(mob instanceof Player player)stack.set(DataComponents.PROFILE,new net.minecraft.world.item.component.ResolvableProfile(player.getGameProfile()));e.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),stack));}}
        }
    }
    @SubscribeEvent public static void primordialKill(LivingDeathEvent e){boolean order=e.getSource().getDirectEntity() instanceof Player p&&p.getMainHandItem().getItem() instanceof PrimordialItem&&Data.get(p.getMainHandItem()).getInt("Affinity")==4;e.getEntity().getPersistentData().putBoolean("WGOrderKill",order);}
    @SubscribeEvent public static void xp(LivingExperienceDropEvent e){if(e.getEntity().getPersistentData().getBoolean("WGOrderKill")){e.setDroppedExperience(e.getDroppedExperience()*5);e.getEntity().getPersistentData().remove("WGOrderKill");}}
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e){if(Gear.has(e.getEntity(),"haste_vambraces")){var p=e.getEntity();float value=e.getNewSpeed();if(!p.onGround()&&!thaumcraft.item.armor.Hover.isHovering(thaumcraft.item.armor.Hover.getHarness(p)))value*=5;var aqua=p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.AQUA_AFFINITY);if(p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)&&p.getItemBySlot(EquipmentSlot.HEAD).getEnchantmentLevel(aqua)==0)value*=5;float hardness=e.getState().getDestroySpeed(p.level(),e.getPosition().orElse(p.blockPosition()));if(hardness>20)value=5+hardness;e.setNewSpeed(value);}}
        @SubscribeEvent public static void bowMovement(PlayerTickEvent.Pre e){var player=e.getEntity();var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);var id=WitchingGadgets.id("sniper_draw_speed");if(speed==null)return;boolean drawing=player.isUsingItem()&&player.getUseItem().getItem() instanceof BowItem&&Gear.has(player,"snipers_ring");if(drawing){if(!speed.hasModifier(id))speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,2.75,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));}else speed.removeModifier(id);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
        Player p=e.getEntity();
        if(!p.level().isClientSide()){int runes=0;for(var piece:p.getArmorSlots())if(piece.getItem() instanceof PrimordialArmor||piece.getItem() instanceof RobeItem)runes+=thaumcraft.infusion.InfusionHooks.getFinalCharge(piece);if(runes!=p.getPersistentData().getInt("WGArmorRunes")){p.getPersistentData().putInt("WGArmorRunes",runes);thaumcraft.item.gear.RunicShield.markDirty(p);}}
        for(var rule:List.of(new Object[]{"knockback_shoulders",Attributes.KNOCKBACK_RESISTANCE,"shoulder_knockback",.6},new Object[]{"wolven_vambraces",Attributes.ATTACK_DAMAGE,"vambrace_strength",2.0})){
            @SuppressWarnings("unchecked") var attribute=(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>)rule[1];
            if(Gear.has(p,(String)rule[0]))Gear.modifier(p,attribute,(String)rule[2],(double)rule[3]);else p.getAttribute(attribute).removeModifier(WitchingGadgets.id((String)rule[2]));
        }
        if(!p.level().isClientSide()&&enchant(p.getItemBySlot(EquipmentSlot.HEAD),"throwoff",p)>0)for(var passenger:List.copyOf(p.getPassengers()))if(passenger instanceof LivingEntity rider){rider.hurt(p.damageSources().playerAttack(p),1);rider.push(p.getRandom().nextFloat()*.4,.1,p.getRandom().nextFloat()*.4);rider.stopRiding();}
        boolean boots=p.getItemBySlot(EquipmentSlot.FEET).is(Content.item("primordial_boots"));var step=p.getAttribute(Attributes.STEP_HEIGHT);var id=WitchingGadgets.id("primordial_step");if(step!=null){if(boots&&thaumcraft.item.armor.ArmorEvents.movingForward(p)&&!p.isShiftKeyDown()&&!p.getAbilities().flying){if(!step.hasModifier(id))step.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,.4,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));}else step.removeModifier(id);}if(boots){p.fallDistance=Math.max(0,p.fallDistance-.25F);if(thaumcraft.item.armor.ArmorEvents.movingForward(p)&&!p.getAbilities().flying)p.moveRelative(p.onGround()?p.isInWater()?.055F/4:.055F:thaumcraft.item.armor.Hover.isHovering(thaumcraft.item.armor.Hover.getHarness(p))?.01F:.03F,new net.minecraft.world.phys.Vec3(0,0,1));}
        if(p.level().isClientSide())return;
        boolean reflect=false;for(var armor:p.getArmorSlots())if(armor.getItem() instanceof PrimordialArmor&&Data.get(armor).getInt("Affinity")==0)reflect=true;
        if(reflect)for(var projectile:p.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,p.getBoundingBox().inflate(4))){if(projectile.getOwner()==p)continue;var away=projectile.position().subtract(p.position());if(away.dot(projectile.getDeltaMovement())>-Math.sqrt(.5*away.lengthSqr()*projectile.getDeltaMovement().lengthSqr()))continue;var source=projectile.getOwner();var aim=source==null?away:source.getEyePosition().subtract(projectile.position());projectile.setDeltaMovement(aim.normalize().scale(projectile.getDeltaMovement().length()));projectile.setOwner(p);projectile.hurtMarked=true;}
        if(p.getPersistentData().getLong("WGKnockbackUntil")>0&&p.level().getGameTime()>=p.getPersistentData().getLong("WGKnockbackUntil")){p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(WitchingGadgets.id("gem_resistance"));p.getPersistentData().remove("WGKnockbackUntil");}
        if(p.onGround())p.getPersistentData().remove("WGDoubleJump");
        for(ItemStack s:p.getArmorSlots())if(s.getItem() instanceof PrimordialArmor&&Data.get(s).getInt("Affinity")==3)for(var effect:List.of(MobEffects.POISON,MobEffects.BLINDNESS,MobEffects.WITHER,MobEffects.CONFUSION,thaumcraft.registry.ModEffects.TAINT_POISON))p.removeEffect(effect);
    }
}
