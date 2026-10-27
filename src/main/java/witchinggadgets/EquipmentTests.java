package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;
import thaumcraft.research.*;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.common.inventory.container.CuriosContainer;

/** Prepared equipment; native inventory menus, player ticks, attacks, targeting and loot. */
@GameTestHolder("witchinggadgets_equipment") @PrefixGameTestTemplate(false)
public final class EquipmentTests {
    static final String[] CLOAKS={"", "cloak","spectral_mantle","cloak_of_voluminous_pockets","wolven_cloak","mantle_of_the_raven"};
    static final String[] KAMAS={"", "kama","spectral_kama","kama_of_voluminous_pockets","wolven_kama","kama_of_the_raven"};
    static final String[] SHOULDERS={"","double_jump_shoulders","knockback_shoulders"},HANDS={"","wolven_vambraces","haste_vambraces"};
    static final String[] ARMOR={"primordial_helm","primordial_cuirass","primordial_greaves","primordial_boots"};
    static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    static ServerPlayer player(GameTestHelper h){var p=OrdinaryPlayTests.player(h);var pos=h.absolutePos(new BlockPos(3,2,3));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);for(int i=0;i<61;i++)p.tick();return p;}
    static net.minecraft.core.Holder<Enchantment> enchant(ServerPlayer p,String id){return p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(net.minecraft.resources.ResourceLocation.parse(id)).orElseThrow();}
    static void putOn(ServerPlayer p,ItemStack stack){
        p.getInventory().setItem(9,stack);var menu=new CuriosContainer(1,p.getInventory());p.containerMenu=menu;
        for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i).container==p.getInventory()&&menu.getSlot(i).getContainerSlot()==9){menu.clicked(i,0,ClickType.QUICK_MOVE,p);break;}
        if(!p.getInventory().getItem(9).isEmpty())throw new IllegalStateException("native Curios rejected "+stack);
        p.closeContainer();
    }
    static void putOn(ServerPlayer p,String kind){if(!kind.isEmpty())putOn(p,Content.stack(kind));}
    static void clear(ServerPlayer p){CuriosApi.getCuriosInventory(p).orElseThrow().getCurios().values().forEach(v->{for(int i=0;i<v.getStacks().getSlots();i++)v.getStacks().setStackInSlot(i,ItemStack.EMPTY);});p.getInventory().clearContent();p.removeAllEffects();}
    static void frame(ServerPlayer p){p.doTick();p.serverLevel().tickNonPassenger(p);}
    static void ticks(ServerPlayer p,int n){for(int i=0;i<n;i++)frame(p);}
    static void close(GameTestHelper h,double actual,double expected,String message){h.assertTrue(Math.abs(actual-expected)<.0001,message+": "+actual+" expected "+expected);}
    @GameTest(template="test/platform",timeoutTicks=200) public static void everyTravellerLoadoutUsesNativeSlotsAndClearsBonuses(GameTestHelper h){
        var p=player(h);h.assertValueEqual(CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("ring").orElseThrow().getStacks().getSlots(),2,"baseline must provide exactly two ring slots");int count=0;
        for(String cloak:CLOAKS)for(String kama:KAMAS)for(String shoulder:SHOULDERS)for(String hand:HANDS)for(int rings=0;rings<6;rings++){
            if((cloak.contains("raven")||kama.contains("raven"))&&!Compat.twilight())continue;
            clear(p);putOn(p,cloak);putOn(p,kama);putOn(p,shoulder);putOn(p,hand);if(rings==1||rings==3||rings==4)putOn(p,"covetous_coin_ring");if(rings==2||rings==3||rings==5)putOn(p,"snipers_ring");if(rings==4)putOn(p,"covetous_coin_ring");if(rings==5)putOn(p,"snipers_ring");p.setOnGround(true);frame(p);
            close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),hand.equals("wolven_vambraces")?3:1,"strength loadout "+count);
            close(h,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),shoulder.equals("knockback_shoulders")?.6:0,"knockback loadout "+count);
            h.assertTrue(!Gear.spectral(p),"unequipped cloak activation leaked into next loadout");count++;
        }
        clear(p);frame(p);close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),1,"strength remained after removal");close(h,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,"resistance remained after removal");System.out.println("WG_PORT12_LOADOUTS_OK: "+count);ravenMovement(h);h.succeed();
    }
    static void ravenMovement(GameTestHelper h){
        if(!Compat.twilight())return;var p=player(h);putOn(p,"mantle_of_the_raven");p.serverLevel().tickNonPassenger(p);var cape=Gear.equipped(p).getFirst();p.setYRot(0);p.setYHeadRot(0);p.setXRot(90);
        for(boolean sneak:List.of(false,true)){p.setShiftKeyDown(sneak);p.setOnGround(false);p.setDeltaMovement(0,-1,0);p.fallDistance=8;p.serverLevel().tickNonPassenger(p);close(h,p.getDeltaMovement().y,sneak?-.75:-.5,"raven fall factor");close(h,p.getDeltaMovement().z,sneak?.1:.05,"raven lost horizontal boost while looking down");close(h,p.fallDistance,0,"raven fall damage");}
        Gear.activate(cape,p);p.setShiftKeyDown(false);p.setOnGround(false);p.setDeltaMovement(0,-1,0);p.serverLevel().tickNonPassenger(p);close(h,p.getDeltaMovement().y,-1,"disabled glide still slowed descent");
        var harness=new ItemStack(Content.tc("hover_harness"));var jar=new ItemStack(thaumcraft.registry.ModBlocks.FILLED_JAR.get());AspectKeys.ASPECTS.set(jar,new AspectList().add(Aspect.ENERGY,8));thaumcraft.item.armor.Hover.setJar(harness,jar);thaumcraft.item.armor.Hover.setHovering(harness,true);p.setItemSlot(EquipmentSlot.CHEST,harness);p.doTick();h.assertTrue(thaumcraft.item.armor.Hover.isHovering(harness),"ordinary charged harness did not stay enabled");thaumcraft.item.armor.ArmorEvents.setMovingForward(p,true);p.setOnGround(false);p.setDeltaMovement(0,1,0);p.serverLevel().tickNonPassenger(p);close(h,p.getDeltaMovement().y,1.125,"raven did not boost hover flight with glide disabled");h.assertTrue(p.getDeltaMovement().z>0,"raven ignored actual server forward input during flight");
        p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.doTick();Gear.activate(cape,p);putOn(p,"kama_of_the_raven");p.setOnGround(false);p.setDeltaMovement(0,-1,0);p.serverLevel().tickNonPassenger(p);close(h,p.getDeltaMovement().y,-.25,"two raven garments did not combine their original glide factors");close(h,p.getDeltaMovement().z,.1,"two raven garments horizontal boost");System.out.println("WG_PORT12_RAVEN_OK: glide, sneak, downward view, disabled glide, charged hover harness, dual garments");
    }
    @GameTest(template="test/platform") public static void knockbackGemAndShouldersStackThenExpireIndependently(GameTestHelper h){
        var p=player(h);putOn(p,"knockback_shoulders");frame(p);OrdinaryPlayTests.use(p,GemItem.create(Aspect.EARTH,0,1,0));frame(p);close(h,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),1,"shoulders + Terra cap");p.removeEffect(StatusEffects.KNOCKBACK);close(h,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),.6,"gem removal deleted shoulders");clear(p);frame(p);close(h,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,"bonus survived both removals");h.succeed();
    }
    @GameTest(template="test/platform") public static void extraJumpPreservesEveryJumpPotionAndTravellerBootBonus(GameTestHelper h){
        var p=player(h);int n=0;for(int boot=0;boot<3;boot++)for(int amp=-1;amp<=5;amp++){
            p.removeAllEffects();p.setItemSlot(EquipmentSlot.FEET,boot==0?ItemStack.EMPTY:boot==1?Content.stack("primordial_boots"):new ItemStack(Content.tc("boots_traveller")));
            if(amp>=0)p.addEffect(new MobEffectInstance(MobEffects.JUMP,100,amp));p.setDeltaMovement(Vec3.ZERO);EquipmentRules.extraJump(p);close(h,p.getDeltaMovement().y,.42+(amp<0?0:(amp+1)*.1)+(boot==2?.275F:0),"jump potion/boots combination "+n++);
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void hasteVambracesRespectAquaAffinityAndOriginalHardBlockRule(GameTestHelper h){
        var p=player(h);putOn(p,"haste_vambraces");frame(p);var pos=p.blockPosition();h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.above(),Blocks.WATER.defaultBlockState());frame(p);frame(p);
        var helmet=new ItemStack(Items.DIAMOND_HELMET);helmet.enchant(enchant(p,"minecraft:aqua_affinity"),1);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        for(boolean ground:List.of(true,false))for(boolean aqua:List.of(true,false)){p.setOnGround(ground);p.setItemSlot(EquipmentSlot.HEAD,aqua?helmet:ItemStack.EMPTY);var e=new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(p,Blocks.STONE.defaultBlockState(),1,pos);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(e);close(h,e.getNewSpeed(),(ground?1:5)*(aqua?1:5),"air/water/Aqua affinity");}
        var hard=new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(p,Blocks.OBSIDIAN.defaultBlockState(),1,pos);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(hard);close(h,hard.getNewSpeed(),55,"obsidian haste special case");h.succeed();
    }
    @GameTest(template="test/platform") public static void bowRingBonusCombinesWithSpeedAndDisappearsOnRelease(GameTestHelper h){
        var p=player(h);putOn(p,"snipers_ring");p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,100,1));frame(p);double base=p.getAttributeValue(Attributes.MOVEMENT_SPEED);OrdinaryPlayTests.use(p,new ItemStack(Items.BOW));p.getInventory().setItem(9,new ItemStack(Items.ARROW,16));p.startUsingItem(InteractionHand.MAIN_HAND);frame(p);h.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED)>base,"draw-speed bonus missing with speed potion");p.stopUsingItem();frame(p);close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),base,"ring bonus remained after bow release");clear(p);frame(p);h.succeed();
    }
    @GameTest(template="test/platform") public static void wolfCloaksUseDamageThresholdsWithoutOverwritingStrongerPotions(GameTestHelper h){
        var p=player(h);putOn(p,"wolven_cloak");putOn(p,"wolven_kama");frame(p);for(int damage:new int[]{2,8,12}){p.removeAllEffects();p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),damage);frame(p);int amp=damage>=12?3:damage>=8?2:1;for(var effect:List.of(MobEffects.DAMAGE_BOOST,MobEffects.MOVEMENT_SPEED,MobEffects.DAMAGE_RESISTANCE))h.assertValueEqual(p.getEffect(effect).getAmplifier(),amp,"wolf threshold "+damage);}
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,5));p.invulnerableTime=0;p.hurt(p.damageSources().generic(),1);frame(p);h.assertValueEqual(p.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier(),5,"wolf erased stronger potion");h.succeed();
    }
    @GameTest(template="test/platform") public static void spectralGearHonorsDocumentedSpiderExceptionAndKeepsPotionsSeparate(GameTestHelper h){
        var p=player(h);putOn(p,"spectral_mantle");p.getInventory().setItem(10,OrdinaryPlayTests.wand());var cloak=Gear.equipped(p).stream().filter(s->s.is(Content.item("spectral_mantle"))).findFirst().orElseThrow();Gear.activate(cloak,p);frame(p);h.assertTrue(Gear.spectral(p)&&!p.hasEffect(MobEffects.INVISIBILITY),"spectral state became a potion");
        for(var type:List.of(EntityType.ZOMBIE,EntityType.SPIDER)){var mob=type.create(h.getLevel());mob.setTarget(p);h.assertTrue((mob.getTarget()==p)==(type==EntityType.SPIDER),"documented spectral exception wrong "+type);}
        var observer=player(h);h.assertTrue(SpectralRendering.hidden(p,observer),"ordinary observer sees spectral body");observer.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Content.tc("goggles_of_revealing")));h.assertTrue(!SpectralRendering.hidden(p,observer),"goggles cannot detect spectral wearer");
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,300));Gear.activate(cloak,p);frame(p);h.assertTrue(!Gear.spectral(p)&&p.hasEffect(MobEffects.INVISIBILITY),"spectral toggle consumed real invisibility potion");h.succeed();
    }
    @GameTest(template="test/platform") public static void spectralBlocksRealMiningUseAndAttacksAndStopsWithoutVis(GameTestHelper h){
        var p=player(h);putOn(p,"spectral_kama");p.getInventory().setItem(10,OrdinaryPlayTests.wand());var cloak=Gear.equipped(p).getFirst();Gear.activate(cloak,p);var at=p.blockPosition().north();h.getLevel().setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));h.assertTrue(!p.gameMode.destroyBlock(at)&&h.getLevel().getBlockState(at).is(Blocks.STONE),"spectral player mined a block");
        var mob=EntityType.ZOMBIE.create(h.getLevel());mob.moveTo(p.position().add(1,0,0));float before=mob.getHealth();p.attack(mob);close(h,mob.getHealth(),before,"spectral attack hurt target");p.getInventory().setItem(10,ItemStack.EMPTY);p.tickCount=99;frame(p);h.assertTrue(!Gear.spectral(p),"spectral stayed active without maintenance vis");h.succeed();
    }
    @GameTest(template="test/platform") public static void translucentGearAndUnveilingFollowSelfVersusOtherRules(GameTestHelper h){
        var wearer=player(h);var viewer=player(h);var reveal=new ItemStack(Content.tc("goggles_of_revealing"));reveal.enchant(enchant(viewer,"witchinggadgets:unveiling"),1);int count=0;
        for(boolean invisible:List.of(false,true))for(boolean revealed:List.of(false,true))for(int level=0;level<=2;level++)for(boolean self:List.of(false,true)){
            wearer.removeAllEffects();if(invisible)wearer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,100));wearer.setItemSlot(EquipmentSlot.HEAD,revealed?reveal:ItemStack.EMPTY);viewer.setItemSlot(EquipmentSlot.HEAD,revealed?reveal:ItemStack.EMPTY);frame(wearer);frame(viewer);
            for(String kind:List.of("primordial_cuirass","bewitched_leggings","cloak","kama","knockback_shoulders","wolven_vambraces")){var stack=Content.stack(kind);if(level>0)stack.enchant(enchant(wearer,"witchinggadgets:translucency"),level);h.assertTrue(EquipmentRules.hidden(stack,wearer,self?wearer:viewer)==((level>1||level>0&&invisible)&&(!revealed||self)),"translucency matrix "+count++);}
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void enchantmentEligibilityAndExclusiveSetsMatchReleasedClasses(GameTestHelper h){
        var p=player(h);for(var r:TcRecipes.all(InfusionEnchantmentRecipe.class))if(r.key().startsWith("WG:")){Knowledge.get(p).addResearch(r.research());var inputs=r.components().stream().map(i->i.displayStacks().getFirst()).toList();var enc=enchant(p,r.enchantment().toString());
            for(var item:List.of(Items.DIAMOND_SWORD,Items.DIAMOND_AXE,Items.DIAMOND_PICKAXE,Items.DIAMOND_HELMET,Items.DIAMOND_LEGGINGS,Content.item("primordial_helm"),Content.item("primordial_greataxe"),Content.item("cloak"),Content.item("knockback_shoulders"),Content.tc("goggles_of_revealing"))){
                var stack=new ItemStack(item);boolean armor=item instanceof ArmorItem,head=armor&&((ArmorItem)item).getEquipmentSlot()==EquipmentSlot.HEAD;
                boolean expected=switch(r.enchantment().getPath()){case "backstabbing"->item instanceof PrimordialItem t?!t.kind.equals("primordial_warhammer"):item instanceof SwordItem||item instanceof AxeItem;case "stealth"->armor&&(((ArmorItem)item).getEquipmentSlot()==EquipmentSlot.FEET||((ArmorItem)item).getEquipmentSlot()==EquipmentSlot.LEGS);case "unveiling"->head&&(item instanceof thaumcraft.node.Revealer||item instanceof thaumcraft.item.gear.Goggles);case "translucency"->new ItemStack(item).isDamageableItem()||item instanceof Gear;case "throwoff"->armor||item instanceof Gear;default->false;};
                var actual=InfusionCrafting.findEnchantment(h.getLevel(),inputs,stack,p);h.assertTrue((actual!=null&&actual.key().equals(r.key()))==expected,"wrong enchantment eligibility: "+r.enchantment()+" "+item);
            }
            if(r.enchantment().getPath().equals("backstabbing")){for(String other:List.of("minecraft:sharpness","minecraft:smite","minecraft:bane_of_arthropods")){var stack=new ItemStack(Items.DIAMOND_AXE);stack.enchant(enchant(p,other),1);h.assertTrue((InfusionCrafting.findEnchantment(h.getLevel(),inputs,stack,p)!=null)==other.endsWith("sharpness"),"backstab conflict "+other);}}
        }allEnchantmentStates(h,p);h.succeed();
    }
    static void allEnchantmentStates(GameTestHelper h,ServerPlayer p){
        var recipes=TcRecipes.all(InfusionEnchantmentRecipe.class).stream().filter(r->r.key().startsWith("WG:")).toList();int count=0;
        for(var entry:Content.ITEM_MAP.entrySet()){
            var item=entry.getValue().get();if(!Compat.visible(entry.getKey())||!(item instanceof Gear||item instanceof ArmorItem||item instanceof PrimordialItem||entry.getKey().equals("primordial_gauntlet")))continue;
            var applicable=new ArrayList<InfusionEnchantmentRecipe>();for(var recipe:recipes){var stack=new ItemStack(item);var holder=enchant(p,recipe.enchantment().toString());boolean allowed=switch(recipe.enchantment().getPath()){
                case "translucency"->stack.isDamageableItem()||item instanceof Gear;
                case "throwoff"->item instanceof ArmorItem||item instanceof Gear;
                case "stealth"->item instanceof ArmorItem armor&&(armor.getEquipmentSlot()==EquipmentSlot.FEET||armor.getEquipmentSlot()==EquipmentSlot.LEGS);
                case "unveiling"->item instanceof ArmorItem armor&&armor.getEquipmentSlot()==EquipmentSlot.HEAD&&(item instanceof thaumcraft.node.Revealer||item instanceof thaumcraft.item.gear.Goggles);
                case "backstabbing"->item instanceof PrimordialItem tool&&!tool.kind.equals("primordial_warhammer");default->false;};
                h.assertTrue(holder.value().isSupportedItem(stack)==allowed,"released equipment enchantment predicate mismatch "+entry.getKey()+" "+recipe.enchantment());if(allowed)applicable.add(recipe);
            }
            int states=1;for(var recipe:applicable)states*=1+enchant(p,recipe.enchantment().toString()).value().getMaxLevel();
            for(int code=0;code<states;code++){int remaining=code;var stack=new ItemStack(item);for(var recipe:applicable){var holder=enchant(p,recipe.enchantment().toString());int radix=holder.value().getMaxLevel()+1;int level=remaining%radix;remaining/=radix;if(level>0)stack.enchant(holder,level);}
                for(var recipe:applicable){var holder=enchant(p,recipe.enchantment().toString());var inputs=recipe.components().stream().map(i->i.displayStacks().getFirst()).toList();var found=InfusionCrafting.findEnchantment(h.getLevel(),inputs,stack,p);boolean expected=stack.getEnchantmentLevel(holder)<holder.value().getMaxLevel();h.assertTrue((found!=null&&found.key().equals(recipe.key()))==expected,"native combined enchantment plan mismatch "+entry.getKey()+" state="+code+" "+recipe.enchantment());}count++;
            }
        }System.out.println("WG_PORT12_ENCHANT_COMBINATIONS_OK: "+count);
    }
    @GameTest(template="test/platform") public static void backstabbingUsesHeadingsAndDoesNotEnchantArrowsFromHeldSword(GameTestHelper h){
        var p=player(h);var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(enchant(p,"witchinggadgets:backstabbing"),3);p.setItemInHand(InteractionHand.MAIN_HAND,sword);
        for(int yaw:new int[]{0,180}){var mob=EntityType.COW.create(h.getLevel());mob.moveTo(p.getX(),p.getY(),p.getZ()+1,yaw,0);mob.setYHeadRot(yaw);p.setYRot(0);p.setYHeadRot(0);mob.hurt(p.damageSources().playerAttack(p),2);close(h,mob.getMaxHealth()-mob.getHealth(),yaw==0?4:2,"backstab heading "+yaw);}
        var mob=EntityType.COW.create(h.getLevel());var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),p,new ItemStack(Items.ARROW),null);mob.hurt(p.damageSources().arrow(arrow,p),2);close(h,mob.getMaxHealth()-mob.getHealth(),2,"held sword enchanted arrow damage");h.succeed();
    }
    @GameTest(template="test/platform") public static void airCleaveDoesNotMultiplyBackstabOrArmoredAxeBonusTwice(GameTestHelper h){
        var p=player(h);var sword=Content.stack("primordial_claymore");sword.enchant(enchant(p,"witchinggadgets:backstabbing"),3);p.setItemInHand(InteractionHand.MAIN_HAND,sword);var primary=EntityType.COW.create(h.getLevel());var secondary=EntityType.COW.create(h.getLevel());primary.moveTo(p.position().add(0,0,1));secondary.moveTo(p.position().add(1,0,1));primary.setYRot(0);secondary.setYRot(0);h.getLevel().addFreshEntity(primary);h.getLevel().addFreshEntity(secondary);primary.hurt(p.damageSources().playerAttack(p),2);close(h,primary.getHealth(),secondary.getHealth(),"secondary air hit applied Backstab twice");h.succeed();
    }
    @GameTest(template="test/platform") public static void everyArmorAffinityCombinationHandlesDamageAndRemoval(GameTestHelper h){
        var p=player(h);int count=0;for(int code=0;code<2401;code++){int v=code,n=0;for(int slot=0;slot<4;slot++){int mode=v%7;v/=7;var stack=mode==0?ItemStack.EMPTY:Content.stack(ARMOR[slot]);if(mode>0){Data.integer(stack,"Affinity",mode-1);n++;}p.setItemSlot(SLOTS[slot],stack);}
            for(int i=0;i<4;i++){var stack=p.getItemBySlot(SLOTS[i]);if(stack.isEmpty())continue;var armor=(PrimordialArmor)stack.getItem();double set=.875+.125*n;int defense=new int[]{3,7,6,3}[i];close(h,armor.protection(stack,p,p.damageSources().mobAttack(EntityType.COW.create(h.getLevel())),SLOTS[i]).ratio(),defense/25.0*set,"mixed armor physical ratio "+code);close(h,armor.protection(stack,p,p.damageSources().magic(),SLOTS[i]).ratio(),defense/35.0*set,"mixed armor magic ratio "+code);}
            p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),2);h.assertTrue(p.getHealth()>0&&p.getHealth()<=20,"mixed armor damage became invalid "+code);count++;
        }for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);frame(p);h.assertTrue(!p.getAttribute(Attributes.STEP_HEIGHT).hasModifier(WitchingGadgets.id("primordial_step")),"boots step survived removal");System.out.println("WG_PORT12_ARMOR_MATRIX_OK: "+count);h.succeed();
    }
    @GameTest(template="test/platform") public static void waterArmorCuresAllFiveEffectsAndPreservesHelpfulPotions(GameTestHelper h){
        var p=player(h);
        var chest=Content.stack("primordial_cuirass");Data.integer(chest,"Affinity",3);p.setItemSlot(EquipmentSlot.CHEST,chest);for(var effect:List.of(MobEffects.POISON,MobEffects.BLINDNESS,MobEffects.WITHER,MobEffects.CONFUSION,thaumcraft.registry.ModEffects.TAINT_POISON))p.addEffect(new MobEffectInstance(effect,200));p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200));frame(p);for(var effect:List.of(MobEffects.POISON,MobEffects.BLINDNESS,MobEffects.WITHER,MobEffects.CONFUSION,thaumcraft.registry.ModEffects.TAINT_POISON))h.assertTrue(!p.hasEffect(effect),"water armor did not cure "+effect);h.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED),"water armor erased helpful effect");h.succeed();
    }
    @GameTest(template="test/platform") public static void helmetThrowoffEjectsRiderAlreadyPresentWhenEquipped(GameTestHelper h){
        var p=player(h);var rider=EntityType.COW.create(h.getLevel());h.assertTrue(rider.startRiding(p,true),"fixture rider cannot mount player");var helmet=Content.stack("primordial_helm");helmet.enchant(enchant(p,"witchinggadgets:throwoff"),1);p.setItemSlot(EquipmentSlot.HEAD,helmet);frame(p);h.assertTrue(!rider.isPassenger(),"already attached rider ignored Throwoff");close(h,rider.getHealth(),9,"throwoff damage changed");h.succeed();
    }
    @GameTest(template="test/platform") public static void primordialWeaponsNeverBlockOrReduceDamageOnRightClick(GameTestHelper h){
        var p=player(h);var attacker=EntityType.COW.create(h.getLevel());attacker.moveTo(p.position().add(0,0,1));
        for(String name:List.of("primordial_claymore","primordial_warhammer","primordial_greataxe"))for(int mode=0;mode<6;mode++){
            p.removeAllEffects();p.setHealth(20);p.invulnerableTime=0;var weapon=Content.stack(name);Data.integer(weapon,"Affinity",mode);OrdinaryPlayTests.use(p,weapon);ticks(p,6);p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(attacker),6);
            close(h,p.getHealth(),14,"right click reduced triggering damage "+name+" mode "+mode);h.assertTrue(!p.isUsingItem()&&!p.isBlocking()&&weapon.getUseAnimation()==UseAnim.NONE,"weapon still blocks: "+name);
            var shield=new ItemStack(Items.SHIELD);p.setItemInHand(InteractionHand.OFF_HAND,shield);var result=p.gameMode.useItem(p,p.level(),weapon,InteractionHand.MAIN_HAND);h.assertTrue(result==InteractionResult.PASS,"weapon swallowed offhand interaction");p.gameMode.useItem(p,p.level(),shield,InteractionHand.OFF_HAND);ticks(p,6);h.assertTrue(p.isBlocking()&&p.getUseItem().is(Items.SHIELD),"ordinary offhand shield stopped working");p.stopUsingItem();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void terraClaymoreProcsOnlyAfterRealDamageAndKeepsStrongerPotions(GameTestHelper h){
        var p=player(h);int procs=0;var weapon=Content.stack("primordial_claymore");Data.integer(weapon,"Affinity",1);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);frame(p);p.getRandom().setSeed(130025L);
        for(int hit=0;hit<256;hit++){p.removeAllEffects();p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),6);close(h,p.getHealth(),14,"Terra reduced the triggering hit");if(p.hasEffect(MobEffects.DAMAGE_RESISTANCE)){procs++;h.assertValueEqual(p.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier(),0,"first proc is not Resistance I");h.assertValueEqual(p.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration(),30,"first proc duration changed");}}
        h.assertTrue(procs>=40&&procs<=90,"Terra probability is not approximately 25%: "+procs+"/256");
        for(String name:List.of("primordial_claymore","primordial_warhammer","primordial_greataxe","primordial_gauntlet"))for(int mode=0;mode<6;mode++){if(name.equals("primordial_claymore")&&mode==1)continue;var other=Content.stack(name);Data.integer(other,"Affinity",mode);p.setItemInHand(InteractionHand.MAIN_HAND,other);p.removeAllEffects();for(int hit=0;hit<12;hit++){p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),1);}h.assertTrue(!p.hasEffect(MobEffects.DAMAGE_RESISTANCE),"Terra proc from wrong weapon/aspect: "+name+"/"+mode);}
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,weapon);for(int hit=0;hit<32;hit++){p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),1);}h.assertTrue(!p.hasEffect(MobEffects.DAMAGE_RESISTANCE),"offhand Terra claymore triggered guard");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),0);h.assertTrue(!p.hasEffect(MobEffects.DAMAGE_RESISTANCE),"zero damage triggered guard");
        for(int hit=0;hit<96;hit++){p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),1);}var res=p.getEffect(MobEffects.DAMAGE_RESISTANCE);h.assertTrue(res!=null&&res.getAmplifier()==2&&res.getDuration()==80,"passive Terra did not stack to Resistance III / 80 ticks");
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,3));for(int hit=0;hit<32;hit++){p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),1);}res=p.getEffect(MobEffects.DAMAGE_RESISTANCE);h.assertTrue(res.getAmplifier()==3&&res.getDuration()>=200,"Terra weakened an existing stronger/longer effect");System.out.println("WG_PORT13_TERRA_OK: "+procs+"/256; only mainhand Terra claymore; after actual damage; stack cap III/80; stronger potions preserved");h.succeed();
    }
    @GameTest(template="test/platform") public static void primordialEquipmentUsesModernAttackDamageSpeedAndCooldown(GameTestHelper h){
        var p=player(h);String[] names={"primordial_claymore","primordial_greataxe","primordial_warhammer","primordial_gauntlet"};double[] damage={11,13,13,9},speed={1.6,1,1,1.6};
        for(int i=0;i<names.length;i++){var weapon=Content.stack(names[i]);Data.integer(weapon,"Affinity",1);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);ticks(p,25);close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),damage[i],"attack damage "+names[i]);close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),speed[i],"attack speed "+names[i]);var mob=EntityType.COW.create(h.getLevel());mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(100);mob.moveTo(p.position().add(0,0,1));p.setOnGround(true);p.fallDistance=0;p.attack(mob);close(h,100-mob.getHealth(),damage[i],"native full-charge hit "+names[i]);ticks(p,10);close(h,p.getAttackStrengthScale(0),speed[i]*.5,"native ten-tick recharge "+names[i]);ticks(p,10);close(h,p.getAttackStrengthScale(0),1,"native full recharge "+names[i]);}
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);frame(p);close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),4,"weapon speed modifier leaked after removal");System.out.println("WG_PORT13_WEAPON_STATS_OK: claymore 11/1.6; axe+hammer 13/1.0; gauntlet 9/1.6; native hits and cooldowns");h.succeed();
    }
    @GameTest(template="test/platform") public static void eachWeaponAffinityCombinesWithEveryBackstabLevelAndNativeAttack(GameTestHelper h){
        var p=player(h);int count=0;for(String name:List.of("primordial_claymore","primordial_warhammer","primordial_greataxe"))for(int mode=0;mode<6;mode++)for(int back=0;back<=3;back++){
            if(name.equals("primordial_warhammer")&&back>0)continue;var weapon=Content.stack(name);Data.integer(weapon,"Affinity",mode);if(back>0)weapon.enchant(enchant(p,"witchinggadgets:backstabbing"),back);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);ticks(p,25);
            var mob=EntityType.COW.create(h.getLevel());mob.moveTo(p.position().add(0,0,1));mob.setYRot(0);p.setYRot(0);p.setYHeadRot(0);p.attack(mob);
            h.assertTrue(mob.getHealth()<mob.getMaxHealth(),"native attack failed "+name+" mode "+mode);
            if(mode==2)h.assertTrue(mob.hasEffect(StatusEffects.CINDER)&&mob.isOnFire(),"fire affinity missing");
            if(mode==3)h.assertTrue(mob.hasEffect(StatusEffects.DISSOLVE),"water affinity missing");
            if(mode==5)h.assertTrue(mob.hasEffect(MobEffects.WEAKNESS)&&mob.hasEffect(MobEffects.HUNGER),"entropy affinity missing");
            h.assertTrue(weapon.getDamageValue()>0,"native hit had no durability cost");count++;
        }System.out.println("WG_PORT12_WEAPONS_OK: "+count);h.succeed();
    }
    @GameTest(template="test/platform") public static void allFiveSocketsStabilizeEveryPointPotencyAndBrittleness(GameTestHelper h){
        var p=player(h);int count=0;for(int socket=0;socket<5;socket++)for(int potency=0;potency<=3;potency++)for(int brittle=0;brittle<=3;brittle++){
            p.getInventory().clearContent();var glove=Content.stack("primordial_gauntlet");Data.integer(glove,"Affinity",socket);p.setItemInHand(InteractionHand.MAIN_HAND,glove);BagItem.open(p,glove,9);var gem=GemItem.create(Aspect.AIR,0,potency,brittle);OrdinaryPlayTests.insert(p,socket,gem);p.closeContainer();OrdinaryPlayTests.use(p,glove);
            var stored=new BagItem.Storage(glove,9).getItem(socket);h.assertTrue(!stored.isEmpty(),"gauntlet shattered point gem "+count);h.assertValueEqual(Data.get(stored).getInt("Spent"),32,"point charge wrong");h.assertValueEqual(p.getInventory().countItem(Content.item("gemstone_dust")),0,"stable socket produced dust");OrdinaryPlayTests.use(p,glove);h.assertValueEqual(Data.get(new BagItem.Storage(glove,9).getItem(socket)).getInt("Spent"),32,"empty point charge used twice");count++;
        }System.out.println("WG_PORT12_SOCKET_STATES_OK: "+count);h.succeed();
    }
    @GameTest(template="test/platform") public static void spectralVisUsesWholeUnitsRobeDiscountAndAmuletPriority(GameTestHelper h){
        var p=player(h);putOn(p,"spectral_mantle");frame(p);var cloak=Gear.equipped(p).getFirst();var wand=OrdinaryPlayTests.wand();p.getInventory().setItem(10,wand);var item=(thaumcraft.wand.WandItem)wand.getItem();
        int plain=(int)(100*item.getConsumptionModifier(wand,p,Aspect.AIR,true));int before=thaumcraft.wand.Wands.getVis(wand,Aspect.AIR);Gear.activate(cloak,p);h.assertValueEqual(before-thaumcraft.wand.Wands.getVis(wand,Aspect.AIR),plain,"spectral cost used centivis as whole vis");Gear.activate(cloak,p);
        p.setItemSlot(EquipmentSlot.CHEST,Content.stack("bewitched_robe"));p.setItemSlot(EquipmentSlot.LEGS,Content.stack("bewitched_leggings"));int discounted=(int)(100*item.getConsumptionModifier(wand,p,Aspect.AIR,true));h.assertTrue(discounted<plain&&plain-discounted>=8,"robe discount missing from spectral cost");before=thaumcraft.wand.Wands.getVis(wand,Aspect.AIR);Gear.activate(cloak,p);h.assertValueEqual(before-thaumcraft.wand.Wands.getVis(wand,Aspect.AIR),discounted,"spectral ignored native robe discount");Gear.activate(cloak,p);
        var stone=new ItemStack(Content.tc("vis_stone"));thaumcraft.wand.VisStorage.storeVis(stone,Aspect.AIR,200);putOn(p,stone);stone=Gear.equipped(p).stream().filter(v->v.is(Content.tc("vis_stone"))).findFirst().orElseThrow();before=thaumcraft.wand.Wands.getVis(wand,Aspect.AIR);Gear.activate(cloak,p);h.assertValueEqual(thaumcraft.wand.VisStorage.getVis(stone,Aspect.AIR),100,"amulet did not pay first");h.assertValueEqual(thaumcraft.wand.Wands.getVis(wand,Aspect.AIR),before,"wand paid while amulet had vis");Gear.activate(cloak,p);
        clear(p);p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);putOn(p,"spectral_kama");frame(p);cloak=Gear.equipped(p).getFirst();thaumcraft.wand.Wands.storeVis(wand,Aspect.AIR,1);p.getInventory().setItem(10,wand);Gear.activate(cloak,p);h.assertTrue(!Gear.spectral(p),"one hundredth of vis enabled the ability");thaumcraft.wand.Wands.fill(wand);p.getInventory().setItem(10,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,wand);Gear.activate(cloak,p);h.assertTrue(Gear.spectral(p),"normal offhand vis source ignored");h.succeed();
    }
    @GameTest(template="test/platform") public static void primordialMiningUsesOriginalMaterialsHardnessAndSneakingRules(GameTestHelper h){
        var p=player(h);var at=h.absolutePos(new BlockPos(3,2,3));int count=0;
        for(String kind:List.of("primordial_warhammer","primordial_greataxe"))for(boolean sneak:List.of(false,true)){
            for(var pos:BlockPos.betweenClosed(at.offset(-1,0,-1),at.offset(1,0,1)))h.getLevel().setBlockAndUpdate(pos,kind.equals("primordial_warhammer")?Blocks.DIRT.defaultBlockState():Blocks.OAK_LEAVES.defaultBlockState());h.getLevel().setBlockAndUpdate(at,kind.equals("primordial_warhammer")?Blocks.STONE.defaultBlockState():Blocks.OAK_LOG.defaultBlockState());
            p.moveTo(at.getX()+.5,at.getY()+1,at.getZ()+.5,0,90);p.setShiftKeyDown(sneak);var tool=Content.stack(kind);p.setItemInHand(InteractionHand.MAIN_HAND,tool);h.assertTrue(p.gameMode.destroyBlock(at),"ordinary mining rejected center");for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)h.assertTrue(h.getLevel().isEmptyBlock(at.offset(x,0,z))==(!sneak||kind.equals("primordial_greataxe")),"area mining material/sneak mismatch "+kind);count++;
        }
        p.setShiftKeyDown(false);h.getLevel().setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(at.east(),Blocks.OBSIDIAN.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,Content.stack("primordial_warhammer"));p.gameMode.destroyBlock(at);h.assertTrue(h.getLevel().isEmptyBlock(at.east()),"invented hardness ratio blocked adjacent obsidian");System.out.println("WG_PORT12_MINING_OK: "+count+" material/sneak cases plus mixed hardness");h.succeed();
    }
    @GameTest(template="test/platform") public static void coinRingsAddTwoOnceWithEveryNativeFortuneLevel(GameTestHelper h){
        var p=player(h);var at=h.absolutePos(new BlockPos(3,2,3));int count=0;
        for(int rings=0;rings<=2;rings++)for(int level=0;level<=3;level++){
            clear(p);for(int i=0;i<rings;i++)putOn(p,"covetous_coin_ring");frame(p);var pick=new ItemStack(Items.DIAMOND_PICKAXE);if(level>0)pick.enchant(enchant(p,"minecraft:fortune"),level);p.setItemInHand(InteractionHand.MAIN_HAND,pick);h.getLevel().setBlockAndUpdate(at,Blocks.DIAMOND_ORE.defaultBlockState());int[] seen={-1};
            java.util.function.Consumer<net.neoforged.neoforge.event.enchanting.EnchantedBlockLootEvent> listener=e->{if(e.getPos().equals(at)&&e.getEnchantment().is(Enchantments.FORTUNE))seen[0]=e.getEnchantmentLevel();};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,listener);try{p.gameMode.destroyBlock(at);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}h.assertValueEqual(seen[0],level+(rings==0?0:2),"native Fortune incorrectly stacked "+rings+" rings with "+level);count++;
        }System.out.println("WG_PORT12_FORTUNE_OK: "+count);h.succeed();
    }
    @GameTest(template="test/platform") public static void everyStealthLevelPairUsesNativeTargetingAndSneakRequirement(GameTestHelper h){
        var p=player(h);int count=0;for(int feet=0;feet<=5;feet++)for(int legs=0;legs<=5;legs++)for(boolean sneak:List.of(false,true))for(int yaw:new int[]{0,180}){
            var boots=new ItemStack(Items.DIAMOND_BOOTS);var pants=new ItemStack(Items.DIAMOND_LEGGINGS);var enchant=enchant(p,"witchinggadgets:stealth");if(feet>0)boots.enchant(enchant,feet);if(legs>0)pants.enchant(enchant,legs);p.setItemSlot(EquipmentSlot.FEET,boots);p.setItemSlot(EquipmentSlot.LEGS,pants);p.setShiftKeyDown(sneak);p.setYRot(0);p.setYHeadRot(0);
            var mob=EntityType.ZOMBIE.create(h.getLevel());mob.setYRot(yaw);mob.setYHeadRot(yaw);float chance=(feet+legs)*.1F-(yaw==0?0:.1F);long seed=314159L+count;p.getRandom().setSeed(seed);boolean hidden=sneak&&feet+legs>0&&net.minecraft.util.RandomSource.create(seed).nextFloat()<chance;mob.setTarget(p);h.assertTrue((mob.getTarget()==null)==hidden,"native stealth targeting pair "+feet+"/"+legs+" sneak="+sneak+" yaw="+yaw);count++;
        }System.out.println("WG_PORT12_STEALTH_OK: "+count);h.succeed();
    }
    @GameTest(template="test/platform") public static void crouchedGauntletUsesChargedGemAndOpensSocketsOnlyOnFailure(GameTestHelper h){
        var p=player(h);var glove=Content.stack("primordial_gauntlet");p.setItemInHand(InteractionHand.MAIN_HAND,glove);new BagItem.Storage(glove,9).setItem(0,GemItem.create(Aspect.AIR,0,0,0));p.setShiftKeyDown(true);OrdinaryPlayTests.use(p,glove);h.assertTrue(p.hasEffect(MobEffects.JUMP)&&p.containerMenu==p.inventoryMenu,"crouching replaced a valid gem ability with a GUI");OrdinaryPlayTests.use(p,glove);h.assertTrue(p.containerMenu instanceof StorageMenu,"depleted crouched gauntlet did not open sockets");h.succeed();
    }
    @GameTest(template="test/platform") public static void airArmorReflectsApproachingProjectilesButNotTangentialOrOwnShots(GameTestHelper h){
        var p=player(h);var chest=Content.stack("primordial_cuirass");p.setItemSlot(EquipmentSlot.CHEST,chest);var source=EntityType.SKELETON.create(h.getLevel());source.moveTo(p.position().add(0,0,6));int count=0;
        for(boolean own:List.of(false,true))for(boolean incoming:List.of(false,true)){
            var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),own?p:source,new ItemStack(Items.ARROW),null);arrow.setPos(p.getX(),p.getY()+1,p.getZ()+2);var velocity=incoming?new Vec3(0,0,-1):new Vec3(1,0,0);arrow.setDeltaMovement(velocity);h.getLevel().addFreshEntity(arrow);frame(p);
            h.assertTrue((arrow.getOwner()==p)==(own||incoming),"air armor owner reflection "+count);if(!own&&incoming)h.assertTrue(arrow.getDeltaMovement().z>0,"incoming projectile was not sent back");if(!incoming)close(h,arrow.getDeltaMovement().x,1,"tangential projectile was redirected");arrow.discard();count++;
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void savedPlayerKeepsCuriosEnchantmentsRunesAndAffinityWithoutDuplicateBonuses(GameTestHelper h){
        var p=player(h);putOn(p,"knockback_shoulders");putOn(p,"wolven_vambraces");putOn(p,"covetous_coin_ring");putOn(p,"covetous_coin_ring");var cloak=Content.stack("cloak_of_voluminous_pockets");cloak.enchant(enchant(p,"witchinggadgets:translucency"),2);new BagItem.Storage(cloak,27).setItem(4,new ItemStack(Items.EMERALD,11));putOn(p,cloak);var helm=Content.stack("primordial_helm");Data.integer(helm,"Affinity",5);helm.enchant(enchant(p,"witchinggadgets:unveiling"),1);p.setItemSlot(EquipmentSlot.HEAD,helm);frame(p);
        var tag=new net.minecraft.nbt.CompoundTag();p.saveWithoutId(tag);var restored=player(h);restored.load(tag);frame(restored);h.assertValueEqual(Gear.equipped(restored).size(),5,"saved Curios slots lost");close(h,restored.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),.6,"restored duplicate knockback");close(h,restored.getAttributeValue(Attributes.ATTACK_DAMAGE),3,"restored duplicate strength");h.assertValueEqual(Data.get(restored.getItemBySlot(EquipmentSlot.HEAD)).getInt("Affinity"),5,"saved affinity lost");var saved=Gear.equipped(restored).stream().filter(s->s.is(Content.item("cloak_of_voluminous_pockets"))).findFirst().orElseThrow();h.assertValueEqual(new BagItem.Storage(saved,27).getItem(4).getCount(),11,"saved worn storage lost");h.assertValueEqual(Events.enchant(saved,"translucency",restored),2,"saved curio enchantment lost");clear(restored);frame(restored);close(h,restored.getAttributeValue(Attributes.ATTACK_DAMAGE),1,"restored modifier survived removal");h.succeed();
    }
    @GameTest(template="test/platform") public static void unveilingAcceptsEveryRegisteredRevealingHelmetInTheActualPack(GameTestHelper h)throws Exception{
        var p=player(h);var enchant=enchant(p,"witchinggadgets:unveiling");var accepted=new ArrayList<String>();var missing=new ArrayList<String>();
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)if(item instanceof ArmorItem armor&&armor.getEquipmentSlot()==EquipmentSlot.HEAD&&(item instanceof thaumcraft.node.Revealer||item instanceof thaumcraft.item.gear.Goggles)){
            String name=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();accepted.add(name);if(!enchant.value().isSupportedItem(new ItemStack(item)))missing.add(name);
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("wg-equipment-helmets.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(accepted));h.assertTrue(missing.isEmpty(),"registered revealing helmets cannot receive Unveiling: "+missing);System.out.println("WG_PORT12_REVEALING_HELMETS_OK: "+accepted);h.succeed();
    }
    @GameTest(template="test/platform") public static void nativeAirCleaveRecalculatesSmiteAndAppliesFireAspectToEachTarget(GameTestHelper h){
        var p=player(h);var weapon=Content.stack("primordial_claymore");weapon.enchant(enchant(p,"minecraft:smite"),1);weapon.enchant(enchant(p,"minecraft:fire_aspect"),1);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);ticks(p,25);p.setOnGround(true);p.setSprinting(true);p.fallDistance=0;
        var primary=EntityType.ZOMBIE.create(h.getLevel());var other=EntityType.COW.create(h.getLevel());primary.moveTo(p.position().add(0,0,1));other.moveTo(p.position().add(1,0,1));for(var entity:List.of(primary,other)){entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);entity.setHealth(100);h.getLevel().addFreshEntity(entity);}float[] seen={0,0};
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> listener=e->{if(e.getEntity()==primary&&seen[0]==0)seen[0]=e.getAmount();if(e.getEntity()==other&&seen[1]==0)seen[1]=e.getAmount();};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,listener);try{p.attack(primary);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        close(h,seen[0]-seen[1],2.5,"Smite bonus from primary undead leaked to secondary animal");h.assertTrue(primary.isOnFire()&&other.isOnFire(),"native Fire Aspect did not reach both air targets");primary.discard();other.discard();h.succeed();
    }
    @GameTest(template="test/platform") public static void coinRingsCombineWithEveryLootingLevelOnActualMobDeaths(GameTestHelper h){
        var p=player(h);int count=0;for(int rings=0;rings<=2;rings++)for(int looting=0;looting<=3;looting++){
            clear(p);for(int i=0;i<rings;i++)putOn(p,"covetous_coin_ring");frame(p);var weapon=Content.stack("primordial_claymore");Data.integer(weapon,"Affinity",4);if(looting>0)weapon.enchant(enchant(p,"minecraft:looting"),looting);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);var mob=EntityType.ZOMBIE.create(h.getLevel());mob.moveTo(p.position().add(0,0,1));h.getLevel().addFreshEntity(mob);int[] seen={-1};
            java.util.function.Consumer<net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent> listener=e->{if(e.getEnchantment().is(Enchantments.LOOTING)&&e.getDamageSource().getEntity()==p)seen[0]=e.getEnchantmentLevel();};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,listener);try{mob.hurt(p.damageSources().playerAttack(p),200);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}h.assertValueEqual(seen[0],looting+(rings==0?0:2),"native looting stack mismatch");h.assertTrue(mob.isDeadOrDying(),"native looting fixture did not die");mob.discard();count++;
        }
        var mob=EntityType.COW.create(h.getLevel());var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),p,new ItemStack(Items.ARROW),null);mob.hurt(p.damageSources().arrow(arrow,p),200);h.assertTrue(!mob.getPersistentData().getBoolean("WGOrderKill"),"held order sword multiplied bow-kill XP");System.out.println("WG_PORT12_LOOTING_OK: "+count);h.succeed();
    }
    @GameTest(template="test/platform") public static void allThreeCrimsonTitlesRefreshNativeNameAndClearWhenRemoved(GameTestHelper h){
        var p=player(h);for(String title:List.of("crimsonCultist","crimsonKnight","crimsonPraetor")){clear(p);var item=Content.stack("crimson_title");Data.string(item,"Title","wg.title."+title);putOn(p,item);frame(p);String name=net.minecraft.network.chat.Component.Serializer.toJson(p.getDisplayName(),p.registryAccess());h.assertTrue(name.contains("wg.title."+title),"native display name did not refresh for "+title);clear(p);frame(p);p.refreshDisplayName();h.assertTrue(!net.minecraft.network.chat.Component.Serializer.toJson(p.getDisplayName(),p.registryAccess()).contains("wg.title."),"removed title remained in display name");}h.succeed();
    }
    @GameTest(template="test/platform") public static void nativeRunicShieldCountsCloakKamaArmorAndDuplicateRingsExactlyOnce(GameTestHelper h){
        int[] max={-1};var p=OrdinaryPlayTests.player(h,packet->{if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof thaumcraft.network.RunicChargePayload charge)max[0]=charge.max();});for(int i=0;i<61;i++)p.tick();
        for(String name:List.of("cloak","kama","knockback_shoulders","wolven_vambraces","covetous_coin_ring","covetous_coin_ring")){var stack=Content.stack(name);thaumcraft.infusion.InfusionKeys.HARDEN.set(stack,1);putOn(p,stack);}
        for(int i=0;i<4;i++){var stack=Content.stack(ARMOR[i]);thaumcraft.infusion.InfusionKeys.HARDEN.set(stack,2);p.setItemSlot(SLOTS[i],stack);}p.getInventory().setItem(10,OrdinaryPlayTests.wand());ticks(p,2);h.assertValueEqual(max[0],14,"native shield omitted or double-counted traveller gear");h.assertTrue(thaumcraft.item.gear.RunicShield.getCharge(p)>0,"native combined shield did not recharge from wand");
        CuriosApi.getCuriosInventory(p).orElseThrow().getCurios().values().forEach(v->{for(int i=0;i<v.getStacks().getSlots();i++)v.getStacks().setStackInSlot(i,ItemStack.EMPTY);});ticks(p,2);h.assertValueEqual(max[0],8,"unequipped runic gear remained counted");for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);ticks(p,2);h.assertValueEqual(thaumcraft.item.gear.RunicShield.getCharge(p),0,"shield survived full equipment removal");h.succeed();
    }
    @GameTest(template="test/platform") public static void orderAbilityMultipliesNativeExperienceOnlyForDirectWeaponKills(GameTestHelper h){
        var p=player(h);var sword=Content.stack("primordial_claymore");Data.integer(sword,"Affinity",4);p.setItemInHand(InteractionHand.MAIN_HAND,sword);
        for(boolean arrowKill:List.of(false,true)){var mob=EntityType.COW.create(h.getLevel());mob.moveTo(p.position().add(0,0,1));h.getLevel().addFreshEntity(mob);int[] xp={-1,-1};
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent> first=e->{if(e.getEntity()==mob)xp[0]=e.getDroppedExperience();};java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent> last=e->{if(e.getEntity()==mob)xp[1]=e.getDroppedExperience();};var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;bus.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,first);bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,last);
            try{var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),p,new ItemStack(Items.ARROW),null);mob.hurt(arrowKill?p.damageSources().arrow(arrow,p):p.damageSources().playerAttack(p),200);}finally{bus.unregister(first);bus.unregister(last);}h.assertTrue(xp[0]>0,"native experience event missing");h.assertValueEqual(xp[1],xp[0]*(arrowKill?1:5),"order XP leaked into ranged attack or lost direct bonus");mob.discard();
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void ordinaryAbilitySelectionChangesOnlyChosenWornOrHeldItem(GameTestHelper h){
        var p=player(h);putOn(p,"spectral_mantle");putOn(p,"kama_of_voluminous_pockets");for(int i=0;i<4;i++)p.setItemSlot(SLOTS[i],Content.stack(ARMOR[i]));var sword=Content.stack("primordial_claymore");p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.getInventory().setItem(10,OrdinaryPlayTests.wand());frame(p);
        h.assertValueEqual(GearAbilities.entries(p).size(),7,"ability menu omitted equipped choices");h.assertTrue(GearAbilities.quick(p),"held weapon quick switch failed");h.assertValueEqual(Data.get(sword).getInt("Affinity"),1,"quick switch did not change held item");for(var slot:SLOTS)h.assertValueEqual(Data.get(p.getItemBySlot(slot)).getInt("Affinity"),0,"quick weapon switch also changed armor");
        h.assertTrue(GearAbilities.activate(p,100+EquipmentSlot.HEAD.ordinal()),"helmet ability selection failed");h.assertValueEqual(Data.get(p.getItemBySlot(EquipmentSlot.HEAD)).getInt("Affinity"),1,"selected helmet did not cycle");for(var slot:List.of(EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))h.assertValueEqual(Data.get(p.getItemBySlot(slot)).getInt("Affinity"),0,"helmet selection changed another piece");h.assertTrue(!Gear.spectral(p),"armor choice activated spectral gear");
        int storage=1000+net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(Content.item("kama_of_voluminous_pockets"));h.assertTrue(GearAbilities.activate(p,storage)&&p.containerMenu instanceof StorageMenu,"selected worn storage did not open");p.closeContainer();h.assertTrue(!Gear.spectral(p),"storage choice also activated spectral mantle");
        var heldHelmet=Content.stack("primordial_helm");p.setItemInHand(InteractionHand.MAIN_HAND,heldHelmet);h.assertTrue(GearAbilities.activate(p,100+EquipmentSlot.MAINHAND.ordinal()),"held armor cannot be configured before equipping");h.assertValueEqual(Data.get(heldHelmet).getInt("Affinity"),1,"held helmet did not change");h.assertValueEqual(Data.get(p.getItemBySlot(EquipmentSlot.HEAD)).getInt("Affinity"),1,"held helmet choice changed the worn helmet instead");h.assertTrue(!GearAbilities.activate(p,200),"invalid ability selection accepted");
        CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("belt").orElseThrow().getStacks().setStackInSlot(0,ItemStack.EMPTY);h.assertTrue(!GearAbilities.activate(p,storage),"removed curio can still activate from a stale menu");h.succeed();
    }
}
