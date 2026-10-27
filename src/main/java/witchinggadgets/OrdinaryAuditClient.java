package witchinggadgets;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.aspect.*;
import thaumcraft.research.*;
import top.theillusivec4.curios.common.network.client.*;
import static witchinggadgets.GameplayAuditClient.*;

/** Extends the existing real-network audit; a second process checks a real save/reopen. */
final class OrdinaryAuditClient {
    static final BlockPos WHEEL=new BlockPos(14,64,0),LIB=new BlockPos(17,64,0),CAMERA_TARGET=new BlockPos(14,65,6),BENCH=new BlockPos(18,64,6),SAVE_WHEEL=new BlockPos(14,64,-6),SAVE_SAUNA=new BlockPos(18,64,-6);
    static final BlockPos FURNACE=new BlockPos(24,64,4);
    static PumpMatrixScene analysisScene;
    static int screenshotStart;
    static void step(String name,Consumer<ServerPlayer> prepare,Runnable action,Consumer<ServerPlayer> verify){add(name,prepare,action,verify);}
    static int inventorySlot(int index){var m=mc().player.containerMenu;for(int i=0;i<m.slots.size();i++)if(m.getSlot(i).container==mc().player.getInventory()&&m.getSlot(i).getContainerSlot()==index)return i;throw new IllegalStateException("missing client inventory slot "+index);}
    static void mouse(int slot,ClickType type){var m=mc().player.containerMenu;mc().gameMode.handleInventoryMouseClick(m.containerId,slot,0,type,mc().player);}
    static void insert(int inv,int slot){mouse(inventorySlot(inv),ClickType.PICKUP);mouse(slot,ClickType.PICKUP);}
    static void clear(ServerPlayer p){p.closeContainer();p.getInventory().clearContent();give(p,ItemStack.EMPTY);}
    static void sync(ServerPlayer p){p.inventoryMenu.broadcastChanges();}
    static void waitSteps(String name,int count){for(int i=0;i<count;i++)step(name+" "+i,p->{},()->{},p->{});}
    static void openCurios(){net.neoforged.neoforge.network.PacketDistributor.sendToServer(new CPacketOpenCurios(ItemStack.EMPTY));}
    static void frame(ServerPlayer p){
        var l=p.serverLevel();
        for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
            if(y==2&&x==0&&z==0)continue;var pos=FURNACE.offset(x,y,z);var block=y<2?Multiblock.expectedBrick(y*9+(z+1)*3+x+1):Blocks.NETHER_BRICK_STAIRS;
            var direction=x<0?Direction.EAST:x>0?Direction.WEST:z<0?Direction.SOUTH:Direction.NORTH;
            if(l.isEmptyBlock(pos.below()))l.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            p.moveTo(pos.getX()+2.5,pos.getY(),pos.getZ()+2.5,direction.toYRot(),85);p.setShiftKeyDown(true);GameplayTests.click(p,new ItemStack(block),pos.below(),Direction.UP);p.setShiftKeyDown(false);check(l.getBlockState(pos).is(block),"ordinary furnace brick placement failed");
        }
        p.moveTo(FURNACE.getX()+.5,FURNACE.getY()+3,FURNACE.getZ()+.5,0,90);OrdinaryPlayTests.use(p,new ItemStack(Items.LAVA_BUCKET));check(l.getBlockState(FURNACE.above(2)).is(Blocks.LAVA),"ordinary lava bucket did not fill furnace well");
        check(Multiblock.valid(l,FURNACE),"furnace built through ordinary block items is invalid");
    }
    static void checkFortune(ServerPlayer p){
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),1);p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var pos=new BlockPos(14,64,8);p.serverLevel().setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());int[] observed={-1};
        Consumer<net.neoforged.neoforge.event.enchanting.EnchantedBlockLootEvent> listener=e->{if(e.getPos().equals(pos)&&e.getEnchantment().is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE))observed[0]=e.getEnchantmentLevel();};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,listener);try{p.gameMode.destroyBlock(pos);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}check(observed[0]==3,"native coin ring did not give Fortune III from Fortune I");System.out.println("WG_PORT8_FORTUNE_OK: actual copied loot tool, actual miner, Fortune 1 + 2 = 3");
    }
    static void equip(String kind){
        step("open native Curios for "+kind,p->{clear(p);p.getInventory().setItem(9,Content.stack(kind));sync(p);},OrdinaryAuditClient::openCurios,p->check(p.containerMenu instanceof top.theillusivec4.curios.common.inventory.container.CuriosContainer,"native Curios menu did not open"));
        step("shift-equip "+kind,p->{},()->mouse(inventorySlot(9),ClickType.QUICK_MOVE),p->check(Gear.has(p,kind),"native slot rejected "+kind));
    }
    static void unequip(String kind){
        step("remove "+kind+" through native Curios",p->{},()->{var m=mc().player.containerMenu;for(int i=0;i<m.slots.size();i++)if(m.getSlot(i).container!=mc().player.getInventory()&&m.getSlot(i).getItem().is(Content.item(kind))){mouse(i,ClickType.QUICK_MOVE);return;}throw new IllegalStateException("equipped Curios slot is not visible: "+kind);},p->check(!Gear.has(p,kind),"native unequip failed: "+kind));
        step("close Curios after "+kind,p->{},()->mc().player.closeContainer(),p->{});
    }
    static void plan(){
        if(Boolean.getBoolean("wg.combatAudit")){CombatReviewClient.plan();return;}
        if(Boolean.getBoolean("wg.equipmentAudit")){EquipmentAuditClient.plan();return;}
        if(Boolean.getBoolean("wg.recipeAudit")){RecipeReviewClient.plan();return;}
        if(Boolean.getBoolean("wg.researchAudit")){ResearchAuditClient.plan();return;}
        if(Boolean.getBoolean("wg.detailAudit")){DetailReviewClient.plan();return;}
        if(Boolean.getBoolean("wg.ordinaryResume")){resume();return;}
        GameplayAuditClient.plan();steps.removeLast();
        step("extend disposable platform",p->{for(var at:BlockPos.betweenClosed(12,63,-9,24,71,10))p.serverLevel().setBlockAndUpdate(at,at.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());for(var drop:p.serverLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(12,60,-10,30,75,12)))drop.discard();clear(p);p.getPersistentData().putBoolean("WGPort8Saved",false);},()->{},p->{});
        step("inspect pump opaque rim from side",p->{give(p,ItemStack.EMPTY);view(p,1.8,64.7,1.5,PUMP);},()->shot("pump-continuous-opaque-side-rim"),p->{});
        place("spinning_wheel",WHEEL);
        step("open ordinary spinning window",p->{clear(p);view(p,16.5,64,2.5,WHEEL);for(int i=0;i<4;i++)p.getInventory().setItem(9+i,new ItemStack(Items.STRING));sync(p);},()->click(WHEEL,Direction.UP,false),p->check(p.containerMenu instanceof MachineMenu,"spinning window did not open"));
        step("insert four strings by mouse",p->{},()->{for(int i=0;i<4;i++)insert(9+i,i);},p->{});
        waitSteps("wait for ordinary wheel ticks",5);
        step("collect spun yarn",p->{},()->{check(mc().player.containerMenu.getSlot(5).getItem().is(Content.item("yarn")),"real client spinning output missing");shot("spinning-real-yarn-output");mouse(5,ClickType.QUICK_MOVE);},p->check(p.getInventory().countItem(Content.item("yarn"))==2,"wheel did not pay/return correct yarn"));
        step("close spinning window",p->{},()->mc().player.closeContainer(),p->{});
        place("label_library",LIB);
        step("open unconfigured library",p->{clear(p);p.getInventory().setItem(9,new ItemStack(Content.tc("jar_label"),2));view(p,19.5,64,2.5,LIB);sync(p);},()->click(LIB,Direction.UP,false),p->{});
        step("insert blanks without choosing an aspect",p->{},()->insert(9,0),p->check(((MachineMenu)p.containerMenu).machine.essentia==null,"unconfigured server library invented an aspect"));
        step("verify blank client preview",p->{},()->{check(((MachineMenu)mc().player.containerMenu).machine.essentia==null&&!mc().player.containerMenu.getSlot(1).hasItem(),"unconfigured client library fabricated an Aer label");shot("library-no-false-air-preview");},p->Knowledge.get(p).discoverAspect(Aspect.FIRE));
        step("choose Ignis and take labels",p->{},()->mc().gameMode.handleInventoryButtonClick(mc().player.containerMenu.containerId,100+Aspect.FIRE.ordinal()),p->{});
        step("collect selected labels",p->{},()->mouse(1,ClickType.QUICK_MOVE),p->check(p.getInventory().countItem(Content.tc("jar_label"))==2,"native library extraction lost blanks"));
        step("close library",p->{},()->mc().player.closeContainer(),p->{});
        step("take actual camera photograph",p->{clear(p);for(var a:Aspect.values())Knowledge.get(p).discoverAspect(a);for(var block:List.of(Blocks.STONE,Blocks.COPPER_BLOCK,Blocks.DIAMOND_BLOCK,Blocks.IRON_BLOCK,Blocks.GOLD_BLOCK,Blocks.EMERALD_BLOCK,Blocks.LAPIS_BLOCK,Blocks.REDSTONE_BLOCK,Blocks.COAL_BLOCK,Blocks.AMETHYST_BLOCK,Blocks.QUARTZ_BLOCK)){p.serverLevel().setBlockAndUpdate(CAMERA_TARGET,block.defaultBlockState());var target=ScanManager.resolveBlock(p.serverLevel(),CAMERA_TARGET);if(target!=null&&ScanManager.validScan(target.aspects(),p)&&!ScanManager.hasBeenScanned(p,target))break;}p.getInventory().setItem(9,new ItemStack(Content.tc("mirrored_glass"),2));give(p,Content.stack("scan_camera"));view(p,14.5,64,9.5,CAMERA_TARGET);sync(p);},()->mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND),p->{check(p.getInventory().countItem(Content.item("iconographic_plate"))==1,"network camera failed");check(p.getInventory().countItem(Content.tc("mirrored_glass"))==1,"camera spent incorrect glass");});
        step("show photograph and camera runes",p->{},()->{check(ClientEffects.received.getOrDefault(Effects.Kind.CAMERA_RUNES,0)>0,"camera runes did not reach real client");shot("camera-photograph-runes");},p->{});
        place(new ItemStack(thaumcraft.registry.ModBlocks.ARCANE_WORKTABLE.get()),BENCH,Direction.UP);
        step("open real workbench for developing",p->{var plate=p.getInventory().items.stream().filter(s->s.is(Content.item("iconographic_plate"))).findFirst().orElseThrow();var keep=plate.copy();clear(p);p.getInventory().setItem(9,keep);p.getInventory().setItem(10,OrdinaryPlayTests.wand());p.getInventory().setItem(11,new ItemStack(Items.PAPER,2));p.getInventory().setItem(12,new ItemStack(Items.INK_SAC,2));ResearchManager.complete(p,"SCANCAMERA",false);view(p,20.5,64,8.5,BENCH);sync(p);},()->click(BENCH,Direction.UP,false),p->check(p.containerMenu instanceof thaumcraft.menu.ArcaneWorkbenchMenu,"native arcane workbench did not open"));
        step("insert developing materials by mouse",p->{},()->{insert(10,1);insert(9,2);insert(11,3);insert(12,4);},p->{});
        step("take first photograph",p->{},()->{shot("photo-native-workbench-preview");mouse(0,ClickType.PICKUP);mouse(inventorySlot(13),ClickType.PICKUP);},p->{check(p.containerMenu.getSlot(2).getItem().getCount()==1,"developing consumed original plate");check(p.getInventory().countItem(Content.item("developed_iconograph"))==1,"first photo missing");});
        step("take second photograph from same plate",p->{},()->mouse(0,ClickType.QUICK_MOVE),p->{check(p.containerMenu.getSlot(2).getItem().getCount()==1,"repeat developing consumed original plate");check(p.getInventory().countItem(Content.item("developed_iconograph"))==2,"repeat developing duplicated/lost copies");});
        step("close photo workbench",p->{},()->mc().player.closeContainer(),p->{});
        step("use developed scan normally",p->{var photo=p.getInventory().items.stream().filter(s->s.is(Content.item("developed_iconograph"))).findFirst().orElseThrow().copyWithCount(1);check(!Knowledge.get(p).isScanned(ScanKind.valueOf(Data.get(photo).getString("PhotoKind")),Data.get(photo).getString("PhotoKey")),"photographic target was already known");p.getInventory().clearContent();give(p,photo);},()->mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND),p->check(p.getMainHandItem().isEmpty(),"developed scan was not consumed after learning"));
        step("open hungry bag for actual pickup",p->{clear(p);give(p,Content.stack("hungry_bag"));for(int i=0;i<18;i++)p.getInventory().setItem(9+i,new ItemStack(Items.DIAMOND,i==0?62:64));sync(p);view(p,14.5,64,9.5,CAMERA_TARGET);},()->mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND),p->check(p.containerMenu instanceof StorageMenu,"hungry bag did not open"));
        step("fill hungry bag through window",p->{},()->{for(int i=0;i<18;i++)insert(9+i,i);},p->{});
        step("walk pickup into nearly full open bag",p->{var drop=new ItemEntity(p.serverLevel(),p.getX(),p.getY(),p.getZ(),new ItemStack(Items.DIAMOND,5));drop.setNoPickUpDelay();p.serverLevel().addFreshEntity(drop);},()->{},p->{check(((StorageMenu)p.containerMenu).storage.getItem(0).getCount()==64,"actual pickup did not update open bag");check(p.getInventory().countItem(Items.DIAMOND)==3,"actual partial pickup duplicated two diamonds");});
        step("show synchronized hungry storage",p->{},()->{check(mc().player.containerMenu.getSlot(0).getItem().getCount()==64,"pickup did not synchronize to client slot");shot("hungry-partial-pickup-conservation");},p->{});
        step("close hungry bag",p->{},()->mc().player.closeContainer(),p->{});
        step("open enderlinked bag",p->{clear(p);p.getEnderChestInventory().setItem(5,new ItemStack(Items.EMERALD,7));give(p,Content.stack("enderlinked_bag"));},()->mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND),p->check(p.containerMenu.getSlot(5).getItem().getCount()==7,"ender storage was not shared"));
        step("take ender chest contents",p->{},()->mouse(5,ClickType.QUICK_MOVE),p->check(p.getInventory().countItem(Items.EMERALD)==7&&p.getEnderChestInventory().getItem(5).isEmpty(),"ender bag duplicated/lost emeralds"));
        step("close ender bag",p->{},()->mc().player.closeContainer(),p->{});
        step("use point gem from selected hand",p->{clear(p);p.setHealth(2);give(p,GemItem.create(Aspect.ORDER,0,0,0));},()->mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND),p->{check(p.getHealth()>=8,"point gem did not heal through network use");check(p.getInventory().countItem(Content.item("gemstone_dust"))==1,"ordinary point use lost shatter dust");});
        step("show returned gem dust",p->{},()->shot("point-gem-dust-survives-normal-use"),p->{});
        equip("knockback_shoulders");
        step("verify shoulder attribute",p->{},()->shot("curios-native-shoulder-slot"),p->check(Math.abs(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)-.6)<.001,"worn shoulder attribute missing"));
        unequip("knockback_shoulders");
        step("verify attribute removal",p->{},()->{},p->check(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==0,"unequip left knockback modifier"));
        equip("wolven_vambraces");
        step("verify vambrace attack attribute",p->{},()->{},p->check(p.getAttributeValue(Attributes.ATTACK_DAMAGE)==3,"worn vambrace strength missing"));
        unequip("wolven_vambraces");
        equip("spectral_mantle");
        step("close spectral equipment menu",p->{p.getInventory().setItem(10,OrdinaryPlayTests.wand());sync(p);},()->mc().player.closeContainer(),p->{});
        step("activate equipped spectral mantle with ability key",p->{},()->{Client.ABILITY.setDown(true);net.minecraft.client.KeyMapping.click(Client.ABILITY.getKey());},p->check(Gear.spectral(p),"native ability key did not activate worn mantle"));
        step("check real spectral invisibility",p->{},()->{Client.ABILITY.setDown(false);check(Gear.spectral(mc().player),"spectral state did not reach client");shot("spectral-native-ability");net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(0));},p->check(!Gear.spectral(p),"spectral ability did not turn off"));
        step("reopen Curios for spectral removal",p->{},OrdinaryAuditClient::openCurios,p->{});unequip("spectral_mantle");
        step("build furnace using ordinary block items and lava bucket",p->{clear(p);frame(p);ResearchManager.complete(p,"INFERNALBLASTFURNACE",false);give(p,OrdinaryPlayTests.wand());view(p,21.5,64,4.5,FURNACE.west());},()->click(FURNACE.west(),Direction.WEST,false),p->{check(p.serverLevel().getBlockEntity(FURNACE) instanceof MachineEntity m&&m.master!=null&&m.facing()==Direction.WEST,"ordinary frame and network wand did not form furnace");check(p.containerMenu==p.inventoryMenu,"furnace incorrectly opened a menu");});
        step("throw cluster into actual furnace",p->{var drop=new ItemEntity(p.serverLevel(),24.5,67.1,4.5,new ItemStack(Content.tc("native_iron_cluster")));drop.setNoPickUpDelay();drop.setDeltaMovement(0,0,0);p.serverLevel().addFreshEntity(drop);give(p,ItemStack.EMPTY);view(p,20.5,64,7.5,FURNACE);},()->shot("furnace-ordinary-bricks-bucket-wand"),p->{});
        equip("double_jump_shoulders");
        step("close Curios for double jump",p->{},()->mc().player.closeContainer(),p->{});
        step("press jump in midair",p->p.connection.teleport(14.5,79,9.5,0,0),()->mc().options.keyJump.setDown(true),p->check(p.getPersistentData().getBoolean("WGDoubleJump"),"real jump-key edge did not execute double jump"));
        step("release jump and land",p->{p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.connection.teleport(14.5,64,9.5,0,0);},()->mc().options.keyJump.setDown(false),p->{});
        step("reopen Curios for shoulders",p->{},OrdinaryAuditClient::openCurios,p->{});unequip("double_jump_shoulders");
        equip("wolven_cloak");
        step("close Curios for wolf response",p->{},()->mc().player.closeContainer(),p->{});
        step("stand on natural campfire damage",p->{p.setHealth(p.getMaxHealth());p.serverLevel().setBlockAndUpdate(new BlockPos(14,64,9),Blocks.CAMPFIRE.defaultBlockState());p.connection.teleport(14.5,64.5,9.5,0,0);},()->{},p->{});
        step("verify actual wolf retaliation buffs",p->{check(p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)&&p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)&&p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),"campfire damage did not trigger worn wolf buffs");p.serverLevel().setBlockAndUpdate(new BlockPos(14,64,9),Blocks.AIR.defaultBlockState());p.clearFire();},()->shot("wolf-cloak-natural-damage-buffs"),p->{});
        step("reopen Curios for wolf removal",p->{},OrdinaryAuditClient::openCurios,p->{});unequip("wolven_cloak");
        equip("covetous_coin_ring");
        step("verify coin ring with actual mining",OrdinaryAuditClient::checkFortune,()->shot("coin-ring-real-mining"),p->{});unequip("covetous_coin_ring");
        waitSteps("finish real furnace processing",6);
        step("verify furnace output and dismantle",p->{var l=p.serverLevel();int count=l.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(FURNACE).inflate(5),e->e.getItem().is(Items.IRON_INGOT)).stream().mapToInt(e->e.getItem().getCount()).sum();check(count==3,"ordinary furnace did not eject three iron ingots");check(p.gameMode.destroyBlock(FURNACE.west()),"ordinary furnace dismantling failed");check(l.getBlockState(FURNACE.above()).is(Blocks.SOUL_SAND)&&l.getBlockState(FURNACE.above(2)).is(Blocks.LAVA),"dismantling did not restore original center materials");view(p,20.5,64,7.5,FURNACE);System.out.println("WG_PORT8_FURNACE_OK: hand-placed frame, lava bucket, network charged wand, actual dropped cluster, 3 ingots, ordinary dismantling");},()->shot("furnace-native-result-and-dismantling"),p->{});
        step("build normal altar for abacus diagnosis",p->{clear(p);for(var pos:BlockPos.betweenClosed(-43,65,37,15,65,72))p.serverLevel().setBlockAndUpdate(pos,Blocks.STONE_BRICKS.defaultBlockState());analysisScene=new PumpMatrixScene(p.serverLevel(),new BlockPos(-40,62,40));analysisScene.build(p,true);give(p,Content.stack("arcane_abacus"));view(p,analysisScene.matrix.getX()+.5,analysisScene.center.getY(),analysisScene.matrix.getZ()+3.5,analysisScene.matrix);},()->click(analysisScene.matrix,Direction.SOUTH,false),p->check(InfusionAnalysis.inspect(p,analysisScene.matrix).recipe()!=null,"abacus did not diagnose idle recipe"));
        step("show abacus recipe and stability",p->{},()->{mc().setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));shot("abacus-idle-recipe-and-stability");mc().setScreen(null);},p->p.gameMode.destroyBlock(analysisScene.center.west(2)));
        step("diagnose actual missing pedestal",p->{give(p,Content.stack("arcane_abacus"));},()->click(analysisScene.matrix,Direction.SOUTH,false),p->check(InfusionAnalysis.inspect(p,analysisScene.matrix).warnings().stream().anyMatch(w->w.key().equals("noPartnerPedestal")),"abacus missed broken pedestal partner"));
        step("show original abacus warning",p->{},()->{mc().setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));shot("abacus-missing-partner-warning");mc().setScreen(null);},p->{p.removeAllEffects();p.connection.teleport(16.5,64,-3.5,0,30);});
        place("spinning_wheel",SAVE_WHEEL);place("sauna_stove",SAVE_SAUNA);
        step("fill sauna normally before save",p->{clear(p);give(p,new ItemStack(Items.WATER_BUCKET));view(p,20.5,64,-3.5,SAVE_SAUNA);},()->click(SAVE_SAUNA,Direction.UP,false),p->check(((MachineEntity)p.serverLevel().getBlockEntity(SAVE_SAUNA)).steamTicks>0,"sauna did not start with normal bucket"));
        step("load ordinary wheel before save",p->{give(p,ItemStack.EMPTY);for(int i=0;i<4;i++)p.getInventory().setItem(9+i,new ItemStack(Items.STRING));view(p,16.5,64,-3.5,SAVE_WHEEL);sync(p);},()->click(SAVE_WHEEL,Direction.UP,false),p->{});
        step("insert wheel materials for restart",p->{},()->{for(int i=0;i<4;i++)insert(9+i,i);},p->{});
        step("close persistent wheel",p->{},()->mc().player.closeContainer(),p->{});
        step("prepare bag through ordinary use before save",p->{clear(p);give(p,Content.stack("hungry_bag"));p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,7));p.inventoryMenu.sendAllDataToRemote();},()->mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND),p->check(p.containerMenu instanceof StorageMenu,"persistent bag did not open: menu="+p.containerMenu.getClass()+", shift="+p.isShiftKeyDown()+", hand="+Data.get(p.getMainHandItem())));
        step("store diamonds by mouse before save",p->{check(p.getInventory().getItem(9).getCount()==7,"persistent diamond supply was not seven");},()->{check(mc().player.containerMenu instanceof StorageMenu,"persistent bag is not open on client");check(mc().player.getInventory().getItem(9).getCount()==7,"persistent diamond supply was not synchronized");insert(9,0);},p->check(((StorageMenu)p.containerMenu).storage.getItem(0).getCount()==7,"persistent mouse insertion failed: contents="+((StorageMenu)p.containerMenu).storage.getItem(0)+", carried="+p.containerMenu.getCarried()));
        step("close persistent bag",p->{},()->mc().player.closeContainer(),p->p.getPersistentData().putBoolean("WGPort8Saved",true));
        step("finish first process and save world",p->{check(new BagItem.Storage(p.getMainHandItem(),18).getItem(0).getCount()==7,"persistent bag not ready: hand="+p.getMainHandItem()+", data="+p.getMainHandItem().getComponents());System.out.println("WG_PORT8_NETWORK_OK: "+steps.size()+" stages; original core scenarios plus spinning, unconfigured library, camera/develop/copy/scan, live hungry pickup, ender storage, point dust, native Curios equip/unequip and ability key");},()->{System.out.println("WG_PORT8_CLIENT_OK: "+shots+" screenshots; saving for restart verification");mc().stop();},p->{});
        int checkpoint=Integer.getInteger("wg.auditCheckpoint",0);
        if(checkpoint>0){var tail=new ArrayList<>(steps.subList(checkpoint,steps.size()));steps.clear();step("reset isolated persistence scene",p->{clear(p);for(var pos:List.of(SAVE_WHEEL,SAVE_SAUNA))p.serverLevel().removeBlock(pos,false);for(var drop:p.serverLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(12,60,-10,24,75,0)))drop.discard();p.connection.teleport(16.5,64,-3.5,0,30);},()->mc().setScreen(null),p->{});steps.addAll(tail);}
    }
    static void resume(){
        step("verify actual saved world on second process",p->{check(p.getPersistentData().getBoolean("WGPort8Saved"),"first-process save marker missing");check(p.getMainHandItem().is(Content.item("hungry_bag")),"saved bag missing from player's hand");check(new BagItem.Storage(p.getMainHandItem(),18).getItem(0).getCount()==7,"saved bag lost seven diamonds");check(((MachineEntity)p.serverLevel().getBlockEntity(SAVE_SAUNA)).steamTicks>0,"saved sauna lost active steam");view(p,19.5,64,-4.5,SAVE_SAUNA);},()->{mc().options.pauseOnLostFocus=false;mc().setScreen(null);mc().options.hideGui=false;},p->{});
        waitSteps("resume actual machine world ticks",6);
        step("verify recipe and sauna after reload",p->{var wheel=(MachineEntity)p.serverLevel().getBlockEntity(SAVE_WHEEL);check(wheel.items.getStackInSlot(5).is(Content.item("yarn"))&&wheel.items.getStackInSlot(5).getCount()==2,"saved spinning recipe did not finish exactly once");check(p.hasEffect(thaumcraft.registry.ModEffects.WARP_WARD),"reloaded sauna did not restore its steam area");System.out.println("WG_PORT8_RELOAD_OK: real process restart retained bag diamonds, completed spinning recipe exactly once and resumed sauna protection");},()->shot("real-save-reopen-machines-and-bag"),p->{});
        step("finish reopened world",p->{},()->{System.out.println("WG_PORT8_RESUME_CLIENT_OK: "+shots+" screenshots; clean second save");mc().stop();},p->{});
    }
}
