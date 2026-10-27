package witchinggadgets;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(WitchingGadgets.ID)
@PrefixGameTestTemplate(false)
public final class VisualRegressionTests {
    static JsonObject asset(String path){try(var in=VisualRegressionTests.class.getResourceAsStream("/assets/witchinggadgets/"+path)){return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new UncheckedIOException(e);}}
    @GameTest(template="test/platform") public static void cuttingTableSeparatesConditionalGeometry(GameTestHelper h){var o=asset("port/legacy_models.json").getAsJsonObject("cutting_table");h.assertValueEqual(o.getAsJsonArray("cubes").size(),13,"flasks/bowls leaked into base mesh");h.assertValueEqual(o.getAsJsonArray("flasks").size(),3,"flask positions missing");h.assertValueEqual(o.getAsJsonArray("bowls").size(),3,"essence positions missing");h.succeed();}
    @GameTest(template="test/platform") public static void noDungeonPrototypesInCreative(GameTestHelper h){for(String name:List.of("obsidian_plate","obsidian_decoration","obsidian_decoration_1","obsidian_decoration_2","magic_tile_lock","sarcophagus","snow_generator","wall_mirror","void_walkway","glimmer_of_light"))h.assertTrue(!ReleaseContent.creative(name),"prototype exposed: "+name);h.assertTrue(ReleaseContent.creative("stone_extruder"),"real release device hidden");h.succeed();}
    @GameTest(template="test/platform") public static void localeHasNoEnglishFallbackKeys(GameTestHelper h){var en=asset("lang/en_us.json");var ru=asset("lang/ru_ru.json");List<String> missing=new ArrayList<>();for(var e:en.entrySet())if(!ru.has(e.getKey())||ru.get(e.getKey()).getAsString().isBlank())missing.add(e.getKey());h.assertTrue(missing.isEmpty(),"Russian keys missing: "+missing);h.succeed();}
    @GameTest(template="test/platform") public static void robeHasDyedBaseAndUncoloredOverlay(GameTestHelper h){var armor=(ArmorItem)Content.item("bewitched_robe");var layers=armor.getMaterial().value().layers();h.assertValueEqual(layers.size(),2,"robe overlay omitted");h.assertTrue(layers.get(0).dyeable()&&!layers.get(1).dyeable(),"robe dye affects wrong texture pass");h.succeed();}
    @GameTest(template="test/platform") public static void clearingRobeDyeRestoresOriginalPurple(GameTestHelper h){var robe=Content.stack("bewitched_robe");robe.set(DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xFF0000,false));var recipe=new DyeRecipe(CraftingBookCategory.MISC);var result=recipe.assemble(CraftingInput.of(1,1,List.of(robe)),h.getLevel().registryAccess());h.assertValueEqual(result.get(DataComponents.DYED_COLOR).rgb(),6961280,"robe resets to vanilla leather brown");h.succeed();}
    @GameTest(template="test/platform") public static void allBlockItemsInheritHandTransforms(GameTestHelper h){for(var n:Content.BLOCK_MAP.keySet()){if(n.equals("rose_vines"))continue;var o=asset("models/block/"+n+".json");h.assertTrue(o.has("parent"),"block lacks standard held transforms: "+n);}h.succeed();}
}
