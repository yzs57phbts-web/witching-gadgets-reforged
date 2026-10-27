package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.aspect.*;

@GameTestHolder(WitchingGadgets.ID) @PrefixGameTestTemplate(false)
public final class AbacusTests {
    @GameTest(template="test/platform") public static void originalUtilityItemsRemainUnstackable(GameTestHelper h){for(String id:List.of("arcane_abacus","gemcutters_tools","developed_iconograph","guiding_thread"))h.assertValueEqual(Content.stack(id).getMaxStackSize(),1,"original unstackable item accepts a stack: "+id);h.succeed();}
    @GameTest(template="test/platform") public static void oneNormalClickSendsOneGroupedNodeReport(GameTestHelper h){
        var messages=new ArrayList<Component>();var p=OrdinaryPlayTests.player(h,packet->{if(packet instanceof ClientboundSystemChatPacket chat)messages.add(chat.content());});
        var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,thaumcraft.registry.ModBlocks.AURA_NODE.get().defaultBlockState());
        var node=(thaumcraft.node.NodeBlockEntity)h.getLevel().getBlockEntity(pos);node.setAspects(new AspectList().add(Aspect.AIR,41).add(Aspect.EARTH,2).add(Aspect.WATER,3).add(Aspect.ENTROPY,32));
        GameplayTests.click(p,Content.stack("arcane_abacus"),pos,Direction.UP);
        var reports=messages.stream().filter(c->c.getString().contains("aer: ")).toList();h.assertValueEqual(reports.size(),1,"one ordinary click did not produce exactly one grouped report");
        var report=reports.getFirst();h.assertTrue(report.getString().contains("aer: 41")&&report.getString().contains("perditio: 32")&&report.getString().contains(pos.toShortString()),"node report lost values or its header");
        for(var aspect:GemItem.PRIMALS){var line=report.getSiblings().stream().filter(c->c.getString().startsWith(aspect.getTag()+": ")).findFirst().orElseThrow();h.assertValueEqual(line.getStyle().getColor().getValue(),aspect==Aspect.ENTROPY?0xA0A0A0:aspect.getColor(),"wrong aspect color");}
        h.succeed();
    }
    @GameTest(template="test/platform") public static void separateReadingsHaveIndependentValuesAndBoundaries(GameTestHelper h){
        var p=OrdinaryPlayTests.player(h);var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,thaumcraft.registry.ModBlocks.AURA_NODE.get().defaultBlockState());var node=(thaumcraft.node.NodeBlockEntity)h.getLevel().getBlockEntity(pos);
        node.setAspects(new AspectList().add(Aspect.AIR,41));var first=AbacusReport.node(node,pos);node.setAspects(new AspectList().add(Aspect.AIR,22));var second=AbacusReport.node(node,pos);
        h.assertTrue(first.getString().contains("aer: 41")&&second.getString().contains("aer: 22"),"new reading mutated an old chat entry");h.assertTrue(first.getString().startsWith("\n[ ")&&first.getString().endsWith("------------------------")&&second.getString().startsWith("\n[ "),"readings have no visible boundary");h.succeed();
    }
}
