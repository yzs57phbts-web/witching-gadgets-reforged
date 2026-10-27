package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.*;

/** Supplies are fixtures; machines run only by ordinary placement, menu interactions and world ticks. */
@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class OrdinaryPlayTests {
    static ServerPlayer player(GameTestHelper h){return player(h,packet->{});}
    static ServerPlayer player(GameTestHelper h,java.util.function.Consumer<net.minecraft.network.protocol.Packet<?>> observer){
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"wg_ordinary");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
            final io.netty.channel.Channel testChannel=new io.netty.channel.embedded.EmbeddedChannel();
            @Override public io.netty.channel.Channel channel(){return testChannel;}
        };
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){observer.accept(packet);}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){observer.accept(packet);}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();return p;
    }
    static MachineEntity machine(GameTestHelper h,ServerPlayer p,String kind){var pos=h.absolutePos(new BlockPos(3,2,3));GameplayTests.place(h,p,Content.stack(kind),pos);return (MachineEntity)h.getLevel().getBlockEntity(pos);}
    static void use(ServerPlayer p,ItemStack item){p.setItemInHand(InteractionHand.MAIN_HAND,item);p.gameMode.useItem(p,p.serverLevel(),item,InteractionHand.MAIN_HAND);}
    static void open(ServerPlayer p,BlockPos pos){p.moveTo(pos.getX()+1.5,pos.getY(),pos.getZ()+2.5);GameplayTests.click(p,ItemStack.EMPTY,pos,Direction.UP);}
    static void insert(ServerPlayer p,int slot,ItemStack supply){
        var menu=p.containerMenu;p.getInventory().setItem(9,supply);
        int inventory=-1;for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i).container==p.getInventory()&&menu.getSlot(i).getContainerSlot()==9){inventory=i;break;}
        if(inventory<0)throw new IllegalStateException("ordinary inventory slot missing");
        menu.clicked(inventory,0,ClickType.PICKUP,p);menu.clicked(slot,0,ClickType.PICKUP,p);
        if(!menu.getCarried().isEmpty())throw new IllegalStateException("ordinary menu insertion rejected slot "+slot);
    }
    static ItemEntity pickup(GameTestHelper h,ServerPlayer p,ItemStack item){var e=new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),item);e.setNoPickUpDelay();h.getLevel().addFreshEntity(e);e.playerTouch(p);return e;}
    static ItemStack wand(){var s=thaumcraft.wand.Wands.create(thaumcraft.wand.WandRegistry.cap("gold"),thaumcraft.wand.WandRegistry.rod("greatwood"),false);thaumcraft.wand.Wands.fill(s);return s;}
    @GameTest(template="test/platform") public static void hungryPickupUpdatesOpenMenuAndPersists(GameTestHelper h){
        var p=player(h);var bag=Content.stack("hungry_bag");use(p,bag);h.assertTrue(p.containerMenu instanceof StorageMenu,"normal bag use did not open menu");
        var menu=(StorageMenu)p.containerMenu;var drop=pickup(h,p,new ItemStack(Items.DIAMOND,4));
        h.assertTrue(drop.isRemoved(),"hungry bag did not pick up drop");
        h.assertValueEqual(menu.storage.getItem(0).getCount(),4,"pickup bypassed the live bag window");
        insert(p,1,new ItemStack(Items.EMERALD,3));p.closeContainer();
        var restored=new BagItem.Storage(bag.copy(),18);h.assertValueEqual(restored.getItem(0).getCount(),4,"editing open bag erased picked-up diamonds");h.assertValueEqual(restored.getItem(1).getCount(),3,"bag edit lost emeralds");h.succeed();
    }
    @GameTest(template="test/platform") public static void hungryPickupMergesAndSpillsOnlyRemainder(GameTestHelper h){
        var p=player(h);var bag=Content.stack("hungry_bag");use(p,bag);var menu=(StorageMenu)p.containerMenu;
        for(int i=0;i<18;i++)insert(p,i,new ItemStack(Items.DIAMOND,i==0?62:64));
        pickup(h,p,new ItemStack(Items.DIAMOND,5));
        h.assertValueEqual(menu.storage.getItem(0).getCount(),64,"hungry bag did not merge the two free spaces");h.assertValueEqual(p.getInventory().countItem(Items.DIAMOND),3,"partial pickup duplicated or lost overflow");h.succeed();
    }
    @GameTest(template="test/platform") public static void hungryOverflowStaysOnGroundWhenPlayerInventoryIsFull(GameTestHelper h){
        var p=player(h);var bag=Content.stack("hungry_bag");use(p,bag);var menu=(StorageMenu)p.containerMenu;
        for(int i=0;i<18;i++)insert(p,i,new ItemStack(Items.DIAMOND,i==0?62:64));
        for(int i=1;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        var drop=pickup(h,p,new ItemStack(Items.DIAMOND,5));h.assertValueEqual(drop.getItem().getCount(),3,"unpicked overflow retained the original five items");h.assertTrue(!drop.isRemoved(),"overflow vanished with a full inventory");h.assertValueEqual(menu.storage.getItem(0).getCount(),64,"bag did not retain exactly two new diamonds");h.succeed();
    }
    @GameTest(template="test/platform") public static void automaticBagsRespectTargetedDropOwnership(GameTestHelper h){
        var p=player(h);use(p,Content.stack("hungry_bag"));var e=new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),new ItemStack(Items.DIAMOND,5));e.setNoPickUpDelay();e.setTarget(UUID.randomUUID());h.getLevel().addFreshEntity(e);e.playerTouch(p);
        h.assertTrue(!e.isRemoved()&&e.getItem().getCount()==5,"hungry bag stole a drop reserved for another player");h.assertTrue(((StorageMenu)p.containerMenu).storage.isEmpty(),"reserved drop entered the bag");h.succeed();
    }
    @GameTest(template="test/platform") public static void brittleGemShatterReturnsDustThroughNormalUse(GameTestHelper h){var p=player(h);var gem=GemItem.create(Aspect.ORDER,1,0,3);p.setHealth(2);use(p,gem);h.assertTrue(gem.isEmpty(),"guaranteed brittle gem did not shatter");h.assertValueEqual(p.getInventory().countItem(Content.item("gemstone_dust")),1,"normal item-use result erased shatter dust");h.assertValueEqual(p.getHealth(),2F,"shattered gem still performed its effect");h.succeed();}
    @GameTest(template="test/platform",timeoutTicks=180) public static void spinningWheelRunsFromNormalMenuInputs(GameTestHelper h){
        var p=player(h);var m=machine(h,p,"spinning_wheel");open(p,m.getBlockPos());
        for(int i=0;i<4;i++)insert(p,i,new ItemStack(Items.STRING));
        h.runAfterDelay(130,()->{h.assertValueEqual(m.items.getStackInSlot(5).getCount(),2,"world ticks did not produce two yarn");p.containerMenu.clicked(5,0,ClickType.QUICK_MOVE,p);h.assertValueEqual(p.getInventory().countItem(Content.item("yarn")),2,"normal output click lost yarn");for(int i=0;i<4;i++)h.assertTrue(m.items.getStackInSlot(i).isEmpty(),"string not paid");h.succeed();});
    }
    static void thread(GameTestHelper h,String kind,Item metal){var p=player(h);var m=machine(h,p,"spinning_wheel");open(p,m.getBlockPos());for(int i=0;i<4;i++)insert(p,i,new ItemStack(i<2?Items.STRING:metal));h.runAfterDelay(130,()->{h.assertTrue(m.items.getStackInSlot(5).is(Content.item(kind)),"ordinary spinning failed for "+kind);h.assertValueEqual(m.items.getStackInSlot(5).getCount(),2,"wrong thread yield");h.succeed();});}
    @GameTest(template="test/platform",timeoutTicks=180) public static void goldenThreadFromRealWheel(GameTestHelper h){thread(h,"golden_thread",Items.GOLD_NUGGET);}
    @GameTest(template="test/platform",timeoutTicks=180) public static void thaumiumThreadFromRealWheel(GameTestHelper h){thread(h,"thaumium_thread",Content.tc("thaumium_nugget"));}
    @GameTest(template="test/platform") public static void cuttingResultUsedAsPointGem(GameTestHelper h){
        var p=player(h);var m=machine(h,p,"cutting_table");open(p,m.getBlockPos());insert(p,0,new ItemStack(Items.DIAMOND));
        var phial=new ItemStack(Content.tc("essentia_phial"));AspectKeys.ASPECTS.set(phial,new AspectList().add(Aspect.ORDER,8));insert(p,1,phial);
        p.containerMenu.clicked(4,0,ClickType.PICKUP,p);var gem=p.containerMenu.getCarried();p.containerMenu.setCarried(ItemStack.EMPTY);p.closeContainer();
        h.assertTrue(gem.getItem() instanceof GemItem,"normal table output missing");p.setHealth(2);use(p,gem);
        h.assertTrue(p.getHealth()>2&&gem.isEmpty(),"crafted point gem did not heal and shatter");h.assertValueEqual(p.getInventory().countItem(Content.item("gemstone_dust")),1,"point gem dust missing");
        h.assertTrue(m.items.getStackInSlot(0).isEmpty()&&m.items.getStackInSlot(1).isEmpty(),"gem inputs unpaid");h.succeed();
    }
    @GameTest(template="test/platform") public static void libraryLabelsRealJarThenPhialFillsIt(GameTestHelper h){
        var p=player(h);var m=machine(h,p,"label_library");open(p,m.getBlockPos());insert(p,0,new ItemStack(Content.tc("jar_label"),2));
        Knowledge.get(p).discoverAspect(Aspect.FIRE);p.containerMenu.clickMenuButton(p,100+Aspect.FIRE.ordinal());p.containerMenu.clicked(1,0,ClickType.PICKUP,p);
        var label=p.containerMenu.getCarried();p.containerMenu.setCarried(ItemStack.EMPTY);p.closeContainer();
        var pos=m.getBlockPos().east(2);GameplayTests.place(h,p,new ItemStack(ModBlocks.WARDED_JAR.get()),pos);GameplayTests.click(p,label,pos,Direction.UP);
        GameplayTests.fillJar(h,p,pos,Aspect.FIRE);var jar=(thaumcraft.essentia.JarBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(jar.getAspectFilter()==Aspect.FIRE,"library label did not filter real jar");h.assertValueEqual(jar.getAmount(),8,"labeled jar lost essentia");h.assertValueEqual(m.items.getStackInSlot(0).getCount(),1,"library consumed wrong number of blanks");h.succeed();
    }
    @GameTest(template="test/platform") public static void cameraPlateCanBeDevelopedRepeatedlyWithoutBeingConsumed(GameTestHelper h){
        var p=player(h);for(var aspect:Aspect.values())Knowledge.get(p).discoverAspect(aspect);var target=h.absolutePos(new BlockPos(3,2,1));h.getLevel().setBlockAndUpdate(target,Blocks.STONE.defaultBlockState());
        p.moveTo(target.getX()+.5,target.getY()-1,target.getZ()+3.5,180,0);p.setYHeadRot(180);p.getInventory().setItem(10,new ItemStack(Content.tc("mirrored_glass"),2));use(p,Content.stack("scan_camera"));
        var plate=p.getInventory().items.stream().filter(s->s.is(Content.item("iconographic_plate"))).findFirst().orElseThrow(()->new IllegalStateException("camera produced no plate; ray="+p.pick(10,1,true)+"; target="+target+"; tags="+ScanManager.resolveBlock(h.getLevel(),target)));
        h.assertValueEqual(p.getInventory().countItem(Content.tc("mirrored_glass")),1,"camera did not spend mirrored glass");
        ResearchManager.complete(p,"SCANCAMERA",false);var at=h.absolutePos(new BlockPos(3,2,5));GameplayTests.place(h,p,new ItemStack(ModBlocks.ARCANE_WORKTABLE.get()),at);open(p,at);
        h.assertTrue(p.containerMenu instanceof thaumcraft.menu.ArcaneWorkbenchMenu,"normal arcane workbench menu missing");
        insert(p,1,wand());insert(p,2,plate.copyWithCount(1));insert(p,3,new ItemStack(Items.PAPER,2));insert(p,4,new ItemStack(Items.INK_SAC,2));
        var menu=p.containerMenu;h.assertTrue(menu.getSlot(0).hasItem(),"developing recipe not visible");
        menu.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(menu.getCarried().is(Content.item("developed_iconograph")),"real workbench did not develop photo");
        h.assertValueEqual(menu.getSlot(2).getItem().getCount(),1,"photographic master plate was consumed");h.assertValueEqual(menu.getSlot(3).getItem().getCount(),1,"paper did not decrement once");
        h.assertTrue(Data.get(menu.getCarried()).getString("PhotoKey").equals(Data.get(plate).getString("PhotoKey")),"photo scan data lost");
        int photoSlot=-1;for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i).container==p.getInventory()&&menu.getSlot(i).getContainerSlot()==13){photoSlot=i;break;}menu.clicked(photoSlot,0,ClickType.PICKUP,p);menu.clicked(0,0,ClickType.PICKUP,p);h.assertValueEqual(menu.getCarried().getCount(),1,"second unstackable photo not reproducible");h.assertValueEqual(p.getInventory().countItem(Content.item("developed_iconograph")),1,"first unstackable photo was lost");h.assertValueEqual(menu.getSlot(2).getItem().getCount(),1,"repeated developing consumed master");h.succeed();
    }
    @GameTest(template="test/platform") public static void enderlinkedBagUsesPlayersRealEnderInventory(GameTestHelper h){var p=player(h);p.getEnderChestInventory().setItem(5,new ItemStack(Items.EMERALD,7));use(p,Content.stack("enderlinked_bag"));h.assertValueEqual(p.containerMenu.getSlot(5).getItem().getCount(),7,"bag did not open actual ender chest");p.containerMenu.clicked(5,0,ClickType.QUICK_MOVE,p);h.assertValueEqual(p.getInventory().countItem(Items.EMERALD),7,"ender extraction lost items");h.assertTrue(p.getEnderChestInventory().getItem(5).isEmpty(),"ender bag duplicated items");h.succeed();}
    @GameTest(template="test/platform") public static void voidFilterDeletesActualDropWithoutConsumingSample(GameTestHelper h){
        var p=player(h);var bag=Content.stack("voidlinked_bag");use(p,bag);var menu=(StorageMenu)p.containerMenu;
        p.getInventory().setItem(9,new ItemStack(Items.STONE,4));menu.clicked(menu.count,0,ClickType.PICKUP,p);menu.clicked(0,0,ClickType.PICKUP,p);menu.clicked(menu.count,0,ClickType.PICKUP,p);
        var drop=pickup(h,p,new ItemStack(Items.STONE,3));h.assertTrue(drop.isRemoved(),"void bag did not intercept actual pickup");h.assertValueEqual(p.getInventory().countItem(Items.STONE),4,"ghost sample consumed or fabricated real stones");h.assertValueEqual(new BagItem.Storage(bag,18).getItem(0).getCount(),1,"ghost filter sample changed during pickup");h.succeed();
    }
    @GameTest(template="test/platform",timeoutTicks=180) public static void ageingStoneAcceleratesBothChildGrowthAndBreedingCooldown(GameTestHelper h){var p=player(h);var m=machine(h,p,"ageing_stone");var child=h.spawn(EntityType.COW,new BlockPos(4,2,3));child.setAge(-200);var adult=h.spawn(EntityType.COW,new BlockPos(2,2,3));adult.setAge(200);h.runAfterDelay(40,()->{h.assertTrue(child.getAge()>-140,"child growth not accelerated by world ticks");h.assertTrue(adult.getAge()<140,"breeding cooldown not accelerated");h.succeed();});}
    @GameTest(template="test/platform",timeoutTicks=120) public static void extruderFeedsRealChestAndStopsUnderRedstone(GameTestHelper h){
        var p=player(h);var m=machine(h,p,"stone_extruder");var out=m.getBlockPos().relative(m.facing());GameplayTests.place(h,p,new ItemStack(Items.CHEST),out);
        h.runAfterDelay(45,()->{var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(out);int before=0;for(int i=0;i<chest.getContainerSize();i++)before+=chest.getItem(i).getCount();h.assertValueEqual(before,1,"extruder did not feed real chest");GameplayTests.place(h,p,new ItemStack(Items.REDSTONE_BLOCK),m.getBlockPos().above());h.runAfterDelay(45,()->{int after=0;for(int i=0;i<chest.getContainerSize();i++)after+=chest.getItem(i).getCount();h.assertValueEqual(after,1,"powered extruder kept producing");h.succeed();});});
    }
    @GameTest(template="test/platform",timeoutTicks=120) public static void saunaConsumesOrdinaryBucketAndKeepsSteamAfterReload(GameTestHelper h){
        var p=player(h);var m=machine(h,p,"sauna_stove");GameplayTests.click(p,new ItemStack(Items.WATER_BUCKET),m.getBlockPos(),Direction.UP);
        h.assertTrue(p.getMainHandItem().is(Items.BUCKET),"ordinary bucket was not emptied");h.runAfterDelay(8,()->{h.assertTrue(m.steamTicks>11900,"sauna did not start from normal bucket");var saved=m.saveWithFullMetadata(h.getLevel().registryAccess());h.getLevel().removeBlockEntity(m.getBlockPos());var restored=new MachineEntity(m.getBlockPos(),m.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);h.runAfterDelay(8,()->{h.assertTrue(restored.steamTicks>11800&&restored.steamTicks<m.steamTicks,"reloaded sauna did not resume natural ticks");h.succeed();});});
    }
    @GameTest(template="test/platform") public static void saunaAutomationUsesOriginalSidePorts(GameTestHelper h){var p=player(h);var m=machine(h,p,"sauna_stove");var level=h.getLevel();var cap=net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK;h.assertTrue(level.getCapability(cap,m.getBlockPos(),Direction.UP)==null,"sauna exposed its closed top as a fluid port");h.assertTrue(level.getCapability(cap,m.getBlockPos(),Direction.DOWN)!=null&&level.getCapability(cap,m.getBlockPos(),Direction.NORTH)!=null,"sauna lost original side/bottom fluid ports");h.succeed();}
    @GameTest(template="test/pump_matrix",batch="abacus_layout",timeoutTicks=100) public static void abacusFindsIdleRecipeAndMissingPartners(GameTestHelper h){
        var p=player(h);var scene=new PumpMatrixScene(h.getLevel(),h.absolutePos(BlockPos.ZERO));scene.build(p,true);
        var before=InfusionAnalysis.inspect(p,scene.matrix);h.assertTrue(before.recipe()!=null&&before.recipe().key().equals(scene.recipe.key()),"abacus did not resolve the idle native recipe");h.assertTrue(before.warnings().isEmpty(),"balanced altar produced spurious warnings: "+before.warnings());
        p.gameMode.destroyBlock(scene.center.west(2));var after=InfusionAnalysis.inspect(p,scene.matrix);h.assertTrue(after.warnings().stream().anyMatch(w->w.key().equals("noPartnerPedestal")),"abacus omitted missing pedestal partner");h.assertTrue(after.recipe()==null,"abacus retained a recipe after a required component was removed");h.succeed();
    }
    @GameTest(template="test/platform") public static void abacusWarnsAboutHighAndDistantPedestals(GameTestHelper h){var p=player(h);var matrix=h.absolutePos(new BlockPos(1,2,1));GameplayTests.place(h,p,new ItemStack(ModBlocks.ARCANE_PEDESTAL.get()),matrix.east(2));GameplayTests.place(h,p,new ItemStack(ModBlocks.ARCANE_PEDESTAL.get()),matrix.east(9).below());var report=InfusionAnalysis.inspect(p,matrix);h.assertTrue(report.warnings().stream().anyMatch(w->w.key().equals("pedestalHeight")),"abacus omitted pedestal-height warning");h.assertTrue(report.warnings().stream().anyMatch(w->w.key().equals("pedestalPos")),"abacus omitted pedestal-distance warning");h.succeed();}
    static void fortune(GameTestHelper h,boolean worn){
        var p=player(h);var other=player(h);var wearer=worn?p:other;
        var ring=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(wearer).orElseThrow().getStacksHandler("ring").orElseThrow().getStacks();
        h.assertTrue(ring.insertItem(0,Content.stack("covetous_coin_ring"),false).isEmpty(),"native ring inventory rejected coin ring");
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),1);p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());int[] observed={-1,-1};
        java.util.function.Consumer<net.neoforged.neoforge.event.enchanting.EnchantedBlockLootEvent> listener=e->{if(e.getPos().equals(pos)&&e.getEnchantment().is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)){observed[0]=e.getEnchantmentLevel();observed[1]=e.getTool()==tool?1:0;}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,listener);
        try{h.assertTrue(p.gameMode.destroyBlock(pos),"ordinary fortune mining failed");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        h.assertValueEqual(observed[0],worn?3:1,"coin ring bonus attributed to wrong miner or lost on copied loot tool");h.assertValueEqual(observed[1],0,"regression needs the real copied loot tool");h.assertTrue(HarvestContext.player()==null,"mining context leaked after native loot calculation");h.succeed();
    }
    @GameTest(template="test/platform") public static void coinRingAddsFortuneDuringActualMining(GameTestHelper h){fortune(h,true);}
    @GameTest(template="test/platform") public static void coinRingDoesNotGrantOtherPlayersFortune(GameTestHelper h){fortune(h,false);}
}
