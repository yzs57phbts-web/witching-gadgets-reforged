package witchinggadgets;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class RecipeChainTests {
    @GameTest(template="test/platform") public static void luckyCoinRestoresRealEnchantmentsWithoutDuplicatingLegacyCoins(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);var coin=new net.minecraft.world.item.ItemStack(Content.tc("gold_coin"),3);Data.integer(coin,"lucky",1);p.getInventory().setItem(0,coin);LuckyCoin.restoreInventory(p);h.assertValueEqual(coin.getCount(),3,"coin migration changed item count");h.assertTrue(LuckyCoin.enchanted(coin)&&coin.isEnchanted(),"coin still lacks real Fortune and Looting");LuckyCoin.restoreInventory(p);h.assertValueEqual(coin.getCount(),3,"repeated migration duplicated coins");coin.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);LuckyCoin.restoreInventory(p);h.assertTrue(!LuckyCoin.enchanted(coin)&&!LuckyCoin.matches(coin),"intentional disenchanting was undone or left an unenchanted coin eligible for the ring");h.succeed();
    }
    @GameTest(template="test/platform") public static void shiftDevelopingPaysForBothCopiesAndKeepsOneMaster(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);thaumcraft.research.Knowledge.get(p).addResearch("SCANCAMERA");var pos=h.absolutePos(new net.minecraft.core.BlockPos(3,2,3));GameplayTests.place(h,p,new net.minecraft.world.item.ItemStack(thaumcraft.registry.ModBlocks.ARCANE_WORKTABLE.get()),pos);OrdinaryPlayTests.open(p,pos);
        var master=Content.stack("iconographic_plate");var data=Data.get(master);data.putString("PhotoKey","minecraft:stone");data.putString("PhotoKind","OBJECT");Data.put(master,data);
        OrdinaryPlayTests.insert(p,1,OrdinaryPlayTests.wand());OrdinaryPlayTests.insert(p,2,master);OrdinaryPlayTests.insert(p,3,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER,2));OrdinaryPlayTests.insert(p,4,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.INK_SAC,2));
        p.containerMenu.clicked(0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,p);h.assertValueEqual(p.getInventory().countItem(Content.item("developed_iconograph")),2,"shift-click lost or duplicated developed photos");h.assertValueEqual(p.containerMenu.getSlot(2).getItem().getCount(),1,"shift developing duplicated or consumed the master plate");h.assertTrue(p.containerMenu.getSlot(3).getItem().isEmpty()&&p.containerMenu.getSlot(4).getItem().isEmpty(),"shift developing did not pay paper/ink");h.succeed();
    }
    @GameTest(template="test/platform") public static void dyeRecipesKeepOriginalEligibilityAndColorMix(GameTestHelper h){
        var recipe=new DyeRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);var red=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE);
        var in=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(Content.stack("bag_of_tricks"),red));var output=recipe.assemble(in,h.getLevel().registryAccess());h.assertValueEqual(output.get(net.minecraft.core.component.DataComponents.DYED_COLOR).rgb(),0xAB618D,"default bag color was omitted or dye palette changed from 1.7.10");
        for(String name:java.util.List.of("spectral_mantle","cloak_of_voluminous_pockets","wolven_cloak","mantle_of_the_raven","spectral_kama","kama_of_voluminous_pockets","wolven_kama","kama_of_the_raven"))h.assertTrue(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(Content.stack(name),red)),h.getLevel()),"special cloak became dyeable contrary to original: "+name);
        for(String name:java.util.List.of("cloak","kama","bewitched_robe","bewitched_leggings"))h.assertTrue(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(Content.stack(name))),h.getLevel()),"unrequested color-reset recipe exists: "+name);h.succeed();
    }
    @GameTest(template="test/platform") public static void originalObjectAspectsAreAvailableToRealScanning(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);for(var a:thaumcraft.aspect.Aspect.values())thaumcraft.research.Knowledge.get(p).discoverAspect(a);
        for(var e:PortData.read("object_aspects")){var o=e.getAsJsonObject();var stack=Recipes.output(o.getAsJsonObject("item")).create();var expected=PortData.aspects(o.getAsJsonObject("aspects"));h.assertTrue(expected.equals(thaumcraft.aspect.AspectHelper.getObjectTags(stack)),"explicit original aspects lost for "+stack);}
        var pearl=Content.stack("powerless_pearl");h.assertTrue(thaumcraft.research.ScanManager.completeScan(p,thaumcraft.research.ScanManager.resolveStack(pearl)),"powerless pearl is not scannable by native TC");h.assertTrue(thaumcraft.research.ResearchManager.isComplete(p,"@EMPOWERPEARL"),"real scan did not reveal pearl restoration");h.succeed();
    }
    @GameTest(template="test/platform") public static void reusableCuttingToolsOrientEveryCreatedTable(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);var tools=Content.stack("gemcutters_tools");int index=0;
        for(var heading:net.minecraft.core.Direction.Plane.HORIZONTAL){var pos=h.absolutePos(new net.minecraft.core.BlockPos(1+index%2*3,2,1+index/2*3));index++;GameplayTests.place(h,p,new net.minecraft.world.item.ItemStack(thaumcraft.registry.ModBlocks.TABLE.get()),pos);p.setYRot(heading.toYRot());GameplayTests.click(p,tools,pos,net.minecraft.core.Direction.UP);h.assertTrue(h.getLevel().getBlockState(pos).is(Content.block("cutting_table")),"tools did not form table");h.assertValueEqual(tools.getCount(),1,"original reusable tools consumed");h.assertTrue(((MachineEntity)h.getLevel().getBlockEntity(pos)).facing()==heading.getOpposite(),"table ignored player heading");}h.succeed();
    }
    @GameTest(template="test/platform") public static void conditionalSilverIngredientsFollowLoadedTags(GameTestHelper h){
        var preferred=new com.google.gson.JsonObject();preferred.addProperty("id","minecraft:iron_ingot");preferred.addProperty("fallbackTag","c:ingots/gold");var ingredient=Recipes.ingredient(preferred);h.assertTrue(ingredient.test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT))&&!ingredient.test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT)),"available preferred metal ignored");preferred.addProperty("fallbackTag","witchinggadgets:absent_optional_metal");h.assertTrue(Recipes.ingredient(preferred).test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT)),"missing optional metal did not use iron");h.succeed();
    }
    @GameTest(template="test/pump_matrix",batch="survival_recipe_chain",timeoutTicks=240000)
    public static void everyAvailableRecipeFromLearnedResearchAndProducedWgIntermediates(GameTestHelper h){
        var chain=new SurvivalRecipeChain(h);
        h.onEachTick(()->{if(!chain.p.isRemoved()){chain.p.doTick();if(chain.p.getFoodData().getFoodLevel()<18){new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF).finishUsingItem(h.getLevel(),chain.p);chain.log("raw","minecraft:cooked_beef");}}});
        try{chain.all().whenComplete((v,error)->{chain.save(error==null?"passed":"failed",error);chain.p.discard();h.runAfterDelay(1,()->{if(error!=null)h.fail("Survival chain at "+chain.current+": "+error);else h.succeed();});});}catch(Throwable error){chain.save("failed",error);chain.p.discard();throw error;}
    }
}
