package witchinggadgets;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.crafting.*;
@GameTestHolder("witchinggadgets") @PrefixGameTestTemplate(false)
public final class Refinement14Tests {
 @GameTest(template="test/platform")
 public static void cinnabarClusterBelongsToWGAndAcceptsRawOrBlock(GameTestHelper h){
  h.assertTrue(TcRecipes.get("PureCinnabar")==null,"duplicate TC research recipe remains");var r=(CrucibleRecipe)TcRecipes.get("WG:PURECINNABAR");
  h.assertTrue(r!=null&&r.research().equals("PURECINNABAR"),"WG cinnabar research recipe missing");
  h.assertTrue(r.catalyst().test(new ItemStack(Content.tc("raw_cinnabar"))),"raw cinnabar rejected");
  h.assertTrue(r.catalyst().test(new ItemStack(Content.tc("cinnabar_ore"))),"ore block rejected");
  h.assertTrue(r.catalyst().test(new ItemStack(Content.tc("deepslate_cinnabar_ore"))),"deepslate ore block rejected");
  h.assertFalse(r.catalyst().test(new ItemStack(Items.REDSTONE)),"unrelated material accepted");
  h.assertTrue(r.catalyst().displayStacks().stream().anyMatch(s->s.is(Content.tc("raw_cinnabar"))),"raw cinnabar icon missing");
  h.assertTrue(r.catalyst().displayStacks().stream().anyMatch(s->s.is(Content.tc("deepslate_cinnabar_ore"))),"deepslate cinnabar icon missing");
  h.assertTrue(r.result().create().is(Content.tc("native_cinnabar_cluster")),"output changed");h.succeed();
 }
}
