package witchinggadgets;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;

@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class InteractionRegressionTests {
    static Player near(GameTestHelper h,MachineEntity machine){var p=h.makeMockPlayer(GameType.CREATIVE);p.setPos(machine.getBlockPos().getX()+2,machine.getBlockPos().getY(),machine.getBlockPos().getZ());return p;}
    static ItemStack crystal(Aspect aspect,int count){var s=new ItemStack(Content.tc("crystallized_essence"),count);AspectKeys.ASPECTS.set(s,new AspectList().add(aspect,1));return s;}
    @GameTest(template="test/platform") public static void tablePreviewOnlyConsumesWhenExtracted(GameTestHelper h){
        var m=PortGameTests.place(h,"cutting_table");var cap=m.automation();cap.insertItem(0,new ItemStack(Items.DIAMOND,2),false);cap.insertItem(1,crystal(Aspect.ORDER,2),false);
        h.assertTrue(cap.getStackInSlot(4).getItem() instanceof GemItem,"ordinary insertion does not expose recipe result");
        cap.extractItem(4,1,true);h.assertValueEqual(m.items.getStackInSlot(0).getCount(),2,"simulation consumes gem");
        h.assertTrue(cap.extractItem(4,1,false).getItem() instanceof GemItem,"automation cannot take result");
        h.assertValueEqual(m.items.getStackInSlot(0).getCount(),1,"wrong gem consumption");h.assertValueEqual(m.items.getStackInSlot(1).getCount(),1,"wrong essence consumption");
        h.getLevel().destroyBlock(m.getBlockPos(),true);h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(m.getBlockPos()).inflate(2),e->e.getItem().getItem() instanceof GemItem).isEmpty(),"breaking table drops unpaid preview");h.succeed();
    }
    @GameTest(template="test/platform") public static void tableShiftClickPaysForEveryGem(GameTestHelper h){
        var m=PortGameTests.place(h,"cutting_table");m.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,3));m.items.setStackInSlot(1,crystal(Aspect.ORDER,3));var p=near(h,m);var menu=new MachineMenu(1,p.getInventory(),m);
        menu.clicked(4,0,ClickType.QUICK_MOVE,p);h.assertValueEqual(p.getInventory().countItem(Content.item("infused_gem")),3,"shift click must craft the three available gems");h.assertTrue(m.items.getStackInSlot(0).isEmpty()&&m.items.getStackInSlot(1).isEmpty(),"bulk output did not pay for every result");h.succeed();
    }
    @GameTest(template="test/platform") public static void tableCursorClickPaysForVisibleResult(GameTestHelper h){
        var m=PortGameTests.place(h,"cutting_table");m.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,2));m.items.setStackInSlot(1,crystal(Aspect.FIRE,2));var p=near(h,m);var menu=new MachineMenu(1,p.getInventory(),m);
        menu.clicked(4,0,ClickType.PICKUP,p);h.assertTrue(menu.getCarried().getItem() instanceof GemItem,"cursor did not receive gem");h.assertValueEqual(m.items.getStackInSlot(0).getCount(),1,"cursor did not consume exactly one gem");h.succeed();
    }
    @GameTest(template="test/platform") public static void tableRejectsMixedAspectsAndUnrelatedAutomationInputs(GameTestHelper h){
        var m=PortGameTests.place(h,"cutting_table");var cap=m.automation();h.assertTrue(!cap.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"table accepts dirt as gem");h.assertTrue(!cap.insertItem(1,new ItemStack(Items.PAPER),false).isEmpty(),"table accepts paper as essence");
        cap.insertItem(0,new ItemStack(Items.DIAMOND),false);cap.insertItem(1,crystal(Aspect.ORDER,1),false);cap.insertItem(2,crystal(Aspect.FIRE,1),false);h.assertTrue(cap.getStackInSlot(4).isEmpty(),"mixed primal aspects produce arbitrary gem");h.succeed();
    }
    @GameTest(template="test/platform") public static void labelSelectionShowsAndPaysForResult(GameTestHelper h){
        var m=PortGameTests.place(h,"label_library");m.automation().insertItem(0,new ItemStack(Content.tc("jar_label"),2),false);var p=near(h,m);var menu=new MachineMenu(1,p.getInventory(),m);menu.clickMenuButton(p,100+Aspect.TREE.ordinal());
        h.assertTrue(!menu.getSlot(1).getItem().isEmpty(),"label output remains invisible until click");menu.clicked(1,0,ClickType.PICKUP,p);
        h.assertValueEqual(m.items.getStackInSlot(0).getCount(),1,"label extraction fails to consume one blank");h.assertTrue(AspectKeys.ASPECTS.get(menu.getCarried()).getAspects().getFirst()==Aspect.TREE,"selected aspect lost");h.succeed();
    }
    @GameTest(template="test/platform") public static void voidFilterCopiesSamplesWithoutTakingOrReturningItems(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var bag=Content.stack("voidlinked_bag");p.getInventory().setItem(0,bag);Data.bool(bag,"GhostFilter",true);var store=new BagItem.Storage(bag,18);var menu=new StorageMenu(1,p.getInventory(),bag,store,18);menu.setCarried(new ItemStack(Items.DIAMOND,12));
        menu.clicked(0,0,ClickType.PICKUP,p);h.assertValueEqual(menu.getCarried().getCount(),12,"filter takes physical items from cursor");h.assertValueEqual(store.getItem(0).getCount(),1,"filter stores whole stack");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(menu.getCarried().isEmpty()&&store.getItem(0).isEmpty(),"filter can create physical copy");h.succeed();
    }
    @GameTest(template="test/platform") public static void oldVoidFilterRefundIsOneTime(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var bag=Content.stack("voidlinked_bag");p.getInventory().setItem(0,bag);new BagItem.Storage(bag,18).setItem(0,new ItemStack(Items.DIAMOND,7));VoidFilter.migrate(p,bag);VoidFilter.migrate(p,bag);
        h.assertValueEqual(p.getInventory().countItem(Items.DIAMOND),7,"migration lost or duplicated stored real items");h.assertValueEqual(new BagItem.Storage(bag,18).getItem(0).getCount(),1,"migration did not normalize sample");h.succeed();
    }
    @GameTest(template="test/platform") public static void spectralActivationRequiresVis(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);var cloak=Content.stack("spectral_mantle");Gear.activate(cloak,p);h.assertTrue(!Data.get(cloak).getBoolean("Active"),"free spectral activation without wand");h.succeed();}
    @GameTest(template="test/platform") public static void primordialArmorUsesOriginalFireAndEarthProtection(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var chest=Content.stack("primordial_cuirass");p.setItemSlot(EquipmentSlot.CHEST,chest);var armor=(PrimordialArmor)chest.getItem();Data.integer(chest,"Affinity",2);var fire=armor.protection(chest,p,p.damageSources().onFire(),EquipmentSlot.CHEST);h.assertValueEqual(fire.ratio(),.75D,"fire mode is only generic fortress resistance");Data.integer(chest,"Affinity",1);h.assertTrue(armor.protection(chest,p,p.damageSources().drown(),EquipmentSlot.CHEST).ratio()>0,"earth mode cannot absorb armor-bypassing damage");h.succeed();
    }
    @GameTest(template="test/platform",timeoutTicks=220) public static void fullExtruderChestStopsWorkAndResumesWhenSpaceOpens(GameTestHelper h){
        var p=GameplayTests.player(h);var pos=h.absolutePos(new BlockPos(3,2,3));GameplayTests.place(h,p,Content.stack("stone_extruder"),pos);var m=(MachineEntity)h.getLevel().getBlockEntity(pos);var target=pos.relative(m.facing());h.getLevel().setBlockAndUpdate(target,Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(target);for(int i=0;i<27;i++)chest.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        h.runAfterDelay(80,()->{h.assertValueEqual(m.progress,0,"extruder continues cycle against full inventory");h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(2)).isEmpty(),"blocked extruder spills items");chest.setItem(0,new ItemStack(Items.COBBLESTONE,63));h.runAfterDelay(50,()->{h.assertValueEqual(chest.getItem(0).getCount(),64,"extruder fails to resume automatically");h.succeed();});});
    }
}
