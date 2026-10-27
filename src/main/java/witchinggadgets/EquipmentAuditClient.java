package witchinggadgets;

import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import static witchinggadgets.GameplayAuditClient.*;

/** Real local client/server, Curios menu clicks, keys, attacks, saved equipment and rendering. */
final class EquipmentAuditClient {
    static int shots;
    static UUID target;
    static ItemStack marked(String kind,String enchant,int level){var stack=Content.stack(kind);stack.enchant(mc().player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(net.minecraft.resources.ResourceLocation.parse(enchant)).orElseThrow(),level);return stack;}
    static void shot(String name){Screenshot.grab(mc().gameDirectory,"wg-port12-"+String.format("%02d",++shots)+"-"+name+".png",mc().getMainRenderTarget(),c->System.out.println("WG_PORT12_SCREENSHOT: "+name));}
    static void key(KeyMapping mapping){mapping.setDown(false);KeyMapping.click(mapping.getKey());}
    static void chooseAbility(int action){check(mc().screen instanceof GearAbilityScreen,"native equipment ability menu absent");var screen=(GearAbilityScreen)mc().screen;int index=-1;for(int i=0;i<screen.entries.size();i++)if(screen.entries.get(i).action()==action)index=i;check(index>=0,"ability entry missing");while(index<screen.offset)screen.mouseScrolled(0,0,0,1);while(index>=screen.offset+screen.rows)screen.mouseScrolled(0,0,0,-1);check(screen.mouseClicked(screen.width/2.0,screen.top+(index-screen.offset)*22+10,0),"native ability button did not click");}
    static void release(){mc().options.keyUp.setDown(false);mc().options.keyUse.setDown(false);mc().options.keyJump.setDown(false);mc().gameMode.releaseUsingItem(mc().player);}
    static void equip(String name){
        add("open native Curios for "+name,p->{p.closeContainer();for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);give(p,ItemStack.EMPTY);p.getInventory().setItem(9,Content.stack(name));p.inventoryMenu.sendAllDataToRemote();},OrdinaryAuditClient::openCurios,p->check(p.containerMenu instanceof top.theillusivec4.curios.common.inventory.container.CuriosContainer,"native Curios menu did not open"));
        add("shift-equip "+name,p->{},()->OrdinaryAuditClient.mouse(OrdinaryAuditClient.inventorySlot(9),net.minecraft.world.inventory.ClickType.QUICK_MOVE),p->check(Gear.has(p,name),"native slot rejected "+name));
    }
    static void remove(String name){add("open Curios to remove "+name,p->{},OrdinaryAuditClient::openCurios,p->{});OrdinaryAuditClient.unequip(name);}
    static void plan(){
        if(Boolean.getBoolean("wg.equipmentResume")){resume();return;}
        add("prepare disposable survival equipment review",p->{p.closeContainer();p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.removeAllEffects();EquipmentTests.clear(p);for(var s:EquipmentSlot.values())if(s.isArmor())p.setItemSlot(s,ItemStack.EMPTY);for(var entity:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(-8,63,-8,15,95,15),entity->entity!=p))entity.discard();p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,p.server);for(var pos:BlockPos.betweenClosed(-5,63,-5,10,72,10))p.serverLevel().setBlockAndUpdate(pos,pos.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());p.connection.teleport(.5,64,.5,0,0);give(p,ItemStack.EMPTY);},()->{mc().options.pauseOnLostFocus=false;mc().setScreen(null);mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT);release();RecipeReviewClient.atlas();},p->{});
        add("walk without boots through native movement",p->{p.connection.teleport(.5,64,.5,0,0);},()->mc().options.keyUp.setDown(true),p->p.getPersistentData().putDouble("WGWalkBase",p.getZ()-.5));
        add("stop and equip primordial boots fixture",p->{},()->mc().options.keyUp.setDown(false),p->{p.connection.teleport(.5,64,.5,0,0);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.setItemSlot(EquipmentSlot.FEET,Content.stack("primordial_boots"));});
        add("walk with boots through native movement",p->{p.connection.teleport(.5,64,.5,0,0);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);},()->mc().options.keyUp.setDown(true),p->{double base=p.getPersistentData().getDouble("WGWalkBase");check(p.getZ()-.5>base*1.2,"primordial speed exists only on server: base="+base+" equipped="+(p.getZ()-.5));check(p.getAttributeValue(Attributes.STEP_HEIGHT)>.99,"native step bonus missing");System.out.println("WG_PORT12_WALK_OK: base="+base+" equipped="+(p.getZ()-.5));});
        add("stop and remove boots",p->{p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);p.connection.teleport(.5,64,.5,0,0);},()->mc().options.keyUp.setDown(false),p->{});
        add("prepare ordinary ladder",p->{for(int y=64;y<=80;y++){p.serverLevel().setBlockAndUpdate(new BlockPos(6,y,-1),Blocks.STONE.defaultBlockState());p.serverLevel().setBlockAndUpdate(new BlockPos(6,y,0),Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING,net.minecraft.core.Direction.SOUTH));}p.connection.teleport(6.5,64,.7,180,0);},()->{},p->{});
        add("climb without vambraces",p->{},()->mc().options.keyUp.setDown(true),p->p.getPersistentData().putDouble("WGClimbBase",p.getY()-64));
        add("stop climbing",p->{p.connection.teleport(.5,64,.5,0,0);},()->mc().options.keyUp.setDown(false),p->{});
        equip("haste_vambraces");
        add("close Curios and approach ladder",p->p.connection.teleport(6.5,64,.7,180,0),()->mc().player.closeContainer(),p->{});
        add("climb with vambraces over real movement packets",p->{},()->mc().options.keyUp.setDown(true),p->{double base=p.getPersistentData().getDouble("WGClimbBase");check(p.getY()-64>base+1,"vambrace climbing exists only on server: base="+base+" equipped="+(p.getY()-64));System.out.println("WG_PORT12_CLIMB_OK: base="+base+" equipped="+(p.getY()-64));});
        add("release ladder input",p->{},()->{mc().options.keyUp.setDown(false);shot("native-vambrace-climbing");},p->p.connection.teleport(.5,64,.5,0,0));
        remove("haste_vambraces");
        for(String name:List.of("wolven_cloak","spectral_kama","knockback_shoulders","wolven_vambraces","covetous_coin_ring","snipers_ring","crimson_title"))equip(name);
        add("close complete equipped set",p->{give(p,ItemStack.EMPTY);p.getInventory().setItem(10,OrdinaryPlayTests.wand());},()->mc().player.closeContainer(),p->{check(Gear.equipped(p).size()==7,"native Curios lost combined gear");check(p.getAttributeValue(Attributes.ATTACK_DAMAGE)==3&&p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==.6,"combined native attributes wrong");});
        add("show combined cloak kama and shoulders",p->{},()->shot("combined-traveller-set"),p->{});
        add("use Terra gem with shoulders",p->give(p,GemItem.create(thaumcraft.aspect.Aspect.EARTH,0,1,0)),()->mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND),p->check(p.hasEffect(StatusEffects.KNOCKBACK)&&p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==1,"native gem/shoulder combination failed"));
        add("draw bow while sniper and coin rings are worn",p->{give(p,new ItemStack(Items.BOW));p.getInventory().setItem(9,new ItemStack(Items.ARROW,16));p.inventoryMenu.sendAllDataToRemote();},()->{mc().options.keyUse.setDown(true);mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);},p->check(p.isUsingItem()&&p.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(WitchingGadgets.id("sniper_draw_speed")),"native draw bonus missing"));
        add("release bow",p->{},()->{release();shot("sniper-ring-bow-set");},p->check(!p.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(WitchingGadgets.id("sniper_draw_speed")),"draw modifier survived release"));
        add("activate spectral kama with native ability key",p->{give(p,ItemStack.EMPTY);p.removeEffect(StatusEffects.KNOCKBACK);},()->key(Client.ABILITY),p->check(Gear.spectral(p)&&!p.hasEffect(MobEffects.INVISIBILITY),"spectral key did not set independent gear state"));
        add("render spectral wearer in third person",p->{},()->{check(Gear.spectral(mc().player),"spectral data did not sync");check(SpectralRendering.tint(mc().player,mc().player)==0x807F7F7F,"self spectral tint");shot("spectral-self-body");},p->{});
        add("spawn survival target",p->{var cow=net.minecraft.world.entity.EntityType.COW.create(p.serverLevel());cow.moveTo(p.getX(),p.getY(),p.getZ()+2);cow.setNoAi(true);p.serverLevel().addFreshEntity(cow);target=cow.getUUID();},()->{},p->{});
        add("try attacking while spectral over real connection",p->{},()->{var entity=mc().level.entitiesForRendering();for(var e:entity)if(e.getUUID().equals(target)){mc().gameMode.attack(mc().player,e);return;}throw new IllegalStateException("target missing on client");},p->{var victim=(LivingEntity)p.serverLevel().getEntity(target);check(victim.getHealth()==10,"spectral network attack hurt target");victim.discard();});
        add("deactivate spectral with native key",p->{},()->key(Client.ABILITY),p->check(!Gear.spectral(p),"spectral native toggle stuck"));
        for(String name:List.of("wolven_cloak","spectral_kama","knockback_shoulders","wolven_vambraces","covetous_coin_ring","snipers_ring","crimson_title"))remove(name);
        add("verify all native unequip cleanup",p->{},()->{},p->check(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==0&&p.getAttributeValue(Attributes.ATTACK_DAMAGE)==1,"native unequip left bonuses"));
        equip("double_jump_shoulders");
        add("close shoulders and prepare jump potion",p->{p.setItemSlot(EquipmentSlot.FEET,Content.stack("primordial_boots"));give(p,GemItem.create(thaumcraft.aspect.Aspect.AIR,0,3,0));p.connection.teleport(.5,64,.5,0,0);},()->{mc().player.closeContainer();mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);},p->check(p.hasEffect(MobEffects.JUMP)&&p.getEffect(MobEffects.JUMP).getAmplifier()==5,"real Aer gem potion missing"));
        add("perform potion boosted double jump with native key",p->{p.connection.teleport(.5,82,.5,0,0);p.fallDistance=0;},()->mc().options.keyJump.setDown(true),p->{check(p.getPersistentData().getBoolean("WGDoubleJump"),"native extra jump missing");check(p.getY()>81,"potion boost did not affect network jump: "+p.getY());});
        add("land and release",p->{p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.connection.teleport(.5,64,.5,0,0);},()->{release();shot("jump-potion-primordial-boots");},p->{});
        add("cycle held weapon without changing armor with native key",p->{for(int i=0;i<4;i++){var armor=Content.stack(EquipmentTests.ARMOR[i]);Data.integer(armor,"Affinity",i==0?0:i+1);p.setItemSlot(EquipmentTests.SLOTS[i],armor);}give(p,Content.stack("primordial_warhammer"));},()->key(Client.SELECT),p->{check(Data.get(p.getMainHandItem()).getInt("Affinity")==1,"native weapon cycle failed");for(int i=0;i<4;i++)check(Data.get(p.getItemBySlot(EquipmentTests.SLOTS[i])).getInt("Affinity")== (i==0?0:i+1),"weapon switching unexpectedly changed armor");});
        add("open per-item ability menu with actual R key",p->{},()->key(Client.ABILITY),p->check(Data.get(p.getItemBySlot(EquipmentSlot.HEAD)).getInt("Affinity")==0,"opening ability menu changed armor"));
        add("select only helmet through native ability button",p->{},()->{shot("per-item-ability-selection");chooseAbility(100+EquipmentSlot.HEAD.ordinal());},p->{for(int i=0;i<4;i++)check(Data.get(p.getItemBySlot(EquipmentTests.SLOTS[i])).getInt("Affinity")==i+1,"individual helmet selection changed another armor piece");check(Data.get(p.getMainHandItem()).getInt("Affinity")==1,"helmet selection changed held weapon");});
        add("show mixed affinity armor",p->{},()->shot("mixed-affinity-armor"),p->{});
        add("hide cuirass with actual Translucency II",p->{p.getItemBySlot(EquipmentSlot.CHEST).enchant(p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(WitchingGadgets.id("translucency")).orElseThrow(),2);},()->{check(EquipmentRules.hidden(mc().player.getItemBySlot(EquipmentSlot.CHEST),mc().player,mc().player),"Translucency did not sync to client armor");shot("translucency-hidden-cuirass");},p->{});
        add("own Unveiling keeps own translucent cuirass hidden",p->{p.getItemBySlot(EquipmentSlot.HEAD).enchant(p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(WitchingGadgets.id("unveiling")).orElseThrow(),1);},()->{check(EquipmentRules.hidden(mc().player.getItemBySlot(EquipmentSlot.CHEST),mc().player,mc().player),"own Unveiling cancelled own Translucency");shot("own-unveiling-translucency");},p->{});
        add("render explicitly translucent WG weapon in first person",p->{var sword=Content.stack("primordial_claymore");sword.enchant(p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(WitchingGadgets.id("translucency")).orElseThrow(),2);give(p,sword);},()->{mc().options.setCameraType(CameraType.FIRST_PERSON);check(EquipmentRules.hidden(mc().player.getMainHandItem(),mc().player,mc().player),"held Translucency missing");},p->{});
        add("observe hidden weapon after a rendered frame",p->{},()->shot("explicitly-enchanted-hidden-weapon"),p->{});
        add("restore third person",p->{},()->mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT),p->{});
        remove("double_jump_shoulders");
        equip("cloak_of_voluminous_pockets");
        add("activate worn storage cloak",p->{give(p,ItemStack.EMPTY);p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,7));p.inventoryMenu.sendAllDataToRemote();},()->{mc().player.closeContainer();key(Client.ABILITY);},p->check(p.containerMenu==p.inventoryMenu,"opening ability picker should not activate storage yet"));
        add("select worn storage without changing armor",p->{},()->chooseAbility(1000+net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(Content.item("cloak_of_voluminous_pockets"))),p->check(p.containerMenu instanceof StorageMenu,"selected worn storage key failed"));
        add("store diamonds in worn cloak through native window",p->{},()->{OrdinaryAuditClient.insert(9,0);shot("worn-cloak-storage");},p->check(((StorageMenu)p.containerMenu).storage.getItem(0).getCount()==7,"worn storage lost inserted stack"));
        add("close worn storage",p->{},()->mc().player.closeContainer(),p->{});
        add("prepare save with real stored diamonds and enchanted equipment",p->{var helm=p.getItemBySlot(EquipmentSlot.HEAD);check(Events.enchant(helm,"unveiling",p)==1,"pre-save helmet missing after Curios inventory operations");for(int i=0;i<4;i++)check(Data.get(p.getItemBySlot(EquipmentTests.SLOTS[i])).getInt("Affinity")==i+1,"pre-save armor missing");p.getPersistentData().putBoolean("WGEquipmentPort12",true);p.server.saveEverything(true,false,true);},()->shot("save-before-reopen"),p->{});
        add("finish first equipment client process",p->System.out.println("WG_PORT12_NETWORK_OK: native Curios menus, mixed set, gems, bow, spectral attacks, keys, potion double jump, worn storage"),()->{release();System.out.println("WG_PORT12_CLIENT_OK: "+shots+" screenshots");mc().stop();},p->{});
    }
    static void resume(){
        add("verify independently reopened equipment save",p->{check(p.getPersistentData().getBoolean("WGEquipmentPort12"),"wrong saved world");var cloak=Gear.equipped(p).stream().filter(s->s.is(Content.item("cloak_of_voluminous_pockets"))).findFirst().orElseThrow();check(new BagItem.Storage(cloak,27).getItem(0).getCount()==7,"saved cloak lost real diamonds");check(Events.enchant(p.getItemBySlot(EquipmentSlot.HEAD),"unveiling",p)==1,"saved helmet lost enchantment");for(int i=0;i<4;i++)check(Data.get(p.getItemBySlot(EquipmentTests.SLOTS[i])).getInt("Affinity")==i+1,"saved mixed affinities changed");},()->{mc().options.pauseOnLostFocus=false;mc().setScreen(null);mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT);shot("real-process-reopen");},p->{});
        add("finish save reload review",p->System.out.println("WG_PORT12_REOPEN_OK: independent process, same equipped cloak, actual inserted diamonds, helmet enchantment, four saved affinities"),()->mc().stop(),p->{});
    }
}
