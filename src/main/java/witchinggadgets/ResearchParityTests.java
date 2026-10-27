package witchinggadgets;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;
import thaumcraft.network.PlayerCompletePayload;
import thaumcraft.research.*;

/** Progress is fixture data; learning runs through the native player request and item-use paths. */
@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class ResearchParityTests {
    static List<ResearchItem> nodes(){return ResearchCategories.get(Research.CATEGORY).research().stream().filter(r->!r.isStub()).toList();}
    static void prerequisites(net.minecraft.server.level.ServerPlayer p,ResearchItem r){for(String key:r.parents())Knowledge.get(p).addResearch(key);for(String key:r.parentsHidden())Knowledge.get(p).addResearch(key);}
    @GameTest(template="test/platform") public static void allLegacyNodesAndIconsExist(GameTestHelper h){
        h.assertValueEqual(nodes().size(),Compat.twilight()?48:47,"missing or extra WG research");
        for(var r:ResearchCategories.get(Research.CATEGORY).research()){
            if(r.iconItem()!=null)h.assertTrue(!r.iconItem().create().isEmpty(),"unresolved item icon "+r.key());
            else h.assertTrue(r.iconTexture()!=null,"missing icon "+r.key());
            for(String parent:r.parents())h.assertTrue(ResearchCategories.getResearch(parent)!=null,"missing parent "+parent);
            for(String parent:r.parentsHidden())h.assertTrue(ResearchCategories.getResearch(parent)!=null,"missing hidden parent "+parent);
        }
        var icons=Map.of("BAGOFTRICKS","witchinggadgets:bag_of_tricks","GEMCUTTING","witchinggadgets:cutting_table","ALCHEMICALTRANSMOGRIFY","minecraft:grass_block","WGFAKEJARLABEL","thaumcraft:warded_jar","WGFAKEARCANESTONE","thaumcraft:arcane_stone","EMPOWERPEARL","witchinggadgets:powerless_pearl","MIRRORPUMP","witchinggadgets:essentia_pump","TERRAFORMER","witchinggadgets:terrestrial_reformer");
        icons.forEach((key,id)->h.assertTrue(ResearchCategories.getResearch(key).iconItem().item().equals(net.minecraft.resources.ResourceLocation.parse(id)),"wrong explicit legacy icon "+key));h.succeed();
    }
    @GameTest(template="test/platform") public static void autoUnlockDoesNotGrantAdvancedMetallurgy(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);Knowledge.get(p).clearResearch();ResearchManager.grantAutoUnlock(p);
        for(String key:List.of("WGPOTIONS","SPINNINGWHEEL"))h.assertTrue(ResearchManager.isComplete(p,key),"missing starting research "+key);
        for(String key:List.of("METALLURGICPERFECTION_CLUSTERS","METALLURGICPERFECTION_TRANSMUTATION"))h.assertTrue(!ResearchManager.hasRequisites(p,key)&&!ResearchManager.isComplete(p,key),"advanced metallurgy available on a fresh character");h.succeed();
    }
    @GameTest(template="test/platform") public static void everyMandatoryParentBlocksNativeLearning(GameTestHelper h){
        for(var r:nodes()){
            var required=new ArrayList<>(r.parents());required.addAll(r.parentsHidden());
            for(String missing:required){
                var p=OrdinaryPlayTests.player(h);prerequisites(p,r);Knowledge.get(p).removeResearch(missing);
                for(var a:r.tags().getAspects())Knowledge.get(p).setAspectPool(a,100);
                p.getInventory().add(new ItemStack(Items.PAPER,2));p.getInventory().add(new ItemStack(Content.tc("scribing_tools")));
                h.assertTrue(!ResearchManager.hasRequisites(p,r.key()),"missing parent ignored "+r.key()+" -> "+missing);
                ResearchManager.handlePlayerComplete(new PlayerCompletePayload(r.key(),ResearchManager.isBuyable(r)?0:1),p);
                h.assertTrue(!ResearchManager.isComplete(p,r.key())&&ResearchNotes.findNoteSlot(p,r.key())<0,"native UI request bypassed parent "+missing);
            }
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void allSecondaryResearchPaysExactLegacyAspectCostsOnce(GameTestHelper h){
        for(var r:nodes())if(r.isSecondary()){
            var p=OrdinaryPlayTests.player(h);prerequisites(p,r);Knowledge.get(p).addResearch("@"+r.key());
            for(var a:r.tags().getAspects())Knowledge.get(p).setAspectPool(a,100);
            ResearchManager.handlePlayerComplete(new PlayerCompletePayload(r.key(),0),p);
            h.assertTrue(ResearchManager.isComplete(p,r.key()),"native purchase failed "+r.key());
            ResearchManager.handlePlayerComplete(new PlayerCompletePayload(r.key(),0),p);
            for(var a:r.tags().getAspects())h.assertValueEqual(Knowledge.get(p).getAspectPool(a),100-r.tags().getAmount(a),"wrong/repeated research cost "+r.key()+" "+a);
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void primaryResearchCreatesUsableNotesThroughNativeRequest(GameTestHelper h){
        for(var r:nodes())if(!r.isSecondary()&&!r.isAutoUnlock()){
            var p=OrdinaryPlayTests.player(h);prerequisites(p,r);Knowledge.get(p).addResearch("@"+r.key());
            p.getInventory().add(new ItemStack(Items.PAPER,2));p.getInventory().add(new ItemStack(Content.tc("scribing_tools")));
            ResearchManager.handlePlayerComplete(new PlayerCompletePayload(r.key(),1),p);
            int slot=ResearchNotes.findNoteSlot(p,r.key());h.assertTrue(slot>=0,"native note request failed "+r.key());
            var note=p.getInventory().getItem(slot);var data=ResearchNoteKeys.NOTE.get(note);
            h.assertTrue(data!=null&&!data.complete()&&!data.hexes().isEmpty(),"note has no real puzzle "+r.key());
            h.assertValueEqual(p.getInventory().countItem(Items.PAPER),1,"wrong paper cost "+r.key());
            ResearchManager.handlePlayerComplete(new PlayerCompletePayload(r.key(),1),p);
            h.assertValueEqual(p.getInventory().countItem(Items.PAPER),1,"repeat click charged paper again "+r.key());
            // A solved puzzle is a fixture; consuming the solved note must use the normal item path.
            ResearchNoteKeys.NOTE.set(note,new ResearchNote(data.key(),data.color(),true,data.copies(),data.hexes()));
            OrdinaryPlayTests.use(p,note);h.assertTrue(ResearchManager.isComplete(p,r.key()),"normal solved-note use failed "+r.key());
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void nativeCompletionImmediatelyUnlocksAllThirteenAliases(GameTestHelper h){
        for(var element:PortData.read("proxies")){
            var o=element.getAsJsonObject();String source=PortData.str(o,"original"),alias=PortData.str(o,"key");var p=OrdinaryPlayTests.player(h);
            var r=ResearchCategories.getResearch(source);h.assertTrue(r.siblings().contains(alias),"missing native sibling "+alias);
            ResearchManager.complete(p,source,false);h.assertTrue(ResearchManager.isComplete(p,alias),"native completion did not sync "+alias);
            var proxy=ResearchCategories.getResearch(alias);h.assertTrue(proxy.isStub()&&proxy.isHidden()&&proxy.parentsHidden().isEmpty(),"proxy behavior differs from original "+alias);
            h.assertTrue(proxy.pages().equals(r.pages())&&proxy.nameKey().equals(r.nameKey()),"proxy opened different native research pages "+alias);
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void existingSaveMigrationAddsOnlyAliasesAndPreservesProgress(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);var k=Knowledge.get(p);k.addResearch("ARCANESTONE");k.addResearch("BAGOFTRICKS");k.setAspectPool(Aspect.MAGIC,17);
        var before=new HashSet<>(k.research());Events.researchAliases(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
        before.add("WGFAKEARCANESTONE");h.assertTrue(k.research().equals(before),"login migration granted unrelated research or removed old progress");h.assertValueEqual(k.getAspectPool(Aspect.MAGIC),17,"migration charged aspects");h.assertTrue(!k.isResearchComplete("AGEINGSTONE"),"migration learned WG descendants");h.succeed();
    }
    @GameTest(template="test/platform") public static void scannerCluesRevealHiddenBagsAndPearlsWithoutLearningThem(GameTestHelper h){
        for(var clue:Map.of("ENDERBAG",new ItemStack(Items.ENDER_CHEST),"EMPOWERPEARL",Content.stack("powerless_pearl")).entrySet()){
            var p=OrdinaryPlayTests.player(h);
            for(var r:ResearchCategories.allResearch())if(!r.key().equals(clue.getKey())&&(r.isHidden()||r.isLost()))Knowledge.get(p).addResearch("@"+r.key());
            h.assertTrue(ResearchManager.createClue(p,clue.getValue(),null),"native scan clue did not reveal "+clue.getKey());
            h.assertTrue(ResearchManager.isComplete(p,"@"+clue.getKey())&&!ResearchManager.isComplete(p,clue.getKey()),"scan clue auto-learned hidden research");
        }h.succeed();
    }
    @GameTest(template="test/platform") public static void legacyPageOrderAndCyclingVariantsAreRestored(GameTestHelper h){
        var bag=ResearchCategories.getResearch("BAGOFTRICKS").pages();h.assertTrue(bag.size()==3&&bag.get(0) instanceof ResearchPage.Text&&bag.get(1).equals(ResearchPage.arcane("WG:BAGOFTRICKS_CLOTH"))&&bag.get(2).equals(ResearchPage.arcane("WG:BAGOFTRICKS_BAG")),"bag recipe/text order");
        var camera=ResearchCategories.getResearch("SCANCAMERA").pages();h.assertTrue(camera.size()==5&&camera.get(1).equals(ResearchPage.arcane("WG:SCANCAMERA"))&&camera.get(2) instanceof ResearchPage.Text&&camera.get(3).equals(ResearchPage.arcane("WG:SCANCAMERA_DEVELOP"))&&camera.get(4).equals(ResearchPage.crafting("WG:SCANCAMERA_CLEARPLATE")),"camera lost developing page or original order");
        var kama=ResearchCategories.getResearch("CLOAKKAMA").pages();h.assertTrue(kama.size()==2&&kama.get(1) instanceof ResearchPage.Arcane a&&a.recipes().size()==(Compat.twilight()?5:4),"kama variants must cycle on one page");
        var cutting=ResearchCategories.getResearch("GEMCUTTING").pages();h.assertTrue(cutting.size()==9&&cutting.get(1) instanceof ResearchPage.Crafting&&cutting.get(2) instanceof ResearchPage.Compound,"cutting assembly pages lost");
        h.assertTrue(cutting.stream().noneMatch(p->p instanceof ResearchPage.Text t&&t.text().endsWith("noMirrors")),"both mirror configuration paragraphs displayed");
        h.assertTrue(ResearchCategories.getResearch("WGFAKEELDRITCHMINOR").isSpecial()&&ResearchCategories.getResearch("WGFAKEPRIMPEARL").isSpecial(),"eldritch proxy frames not special");h.succeed();
    }
    @GameTest(template="test/platform") public static void blankPhotoCannotUseBookDisplayRecipe(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);Knowledge.get(p).addResearch("SCANCAMERA");var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));grid.set(0,Content.stack("iconographic_plate"));grid.set(1,new ItemStack(Items.PAPER));grid.set(2,new ItemStack(Items.INK_SAC));
        h.assertTrue(ArcaneCrafting.find(grid,p)==null,"book preview let a blank plate produce a fake photo");h.succeed();
    }
}
