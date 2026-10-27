package witchinggadgets;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import thaumcraft.aspect.*;
import thaumcraft.node.NodeView;

/** One reading is one chat entry, with a visible boundary between successive readings. */
final class AbacusReport {
    static MutableComponent header(String key,BlockPos pos){return Component.literal("\n[ ").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable(key,pos.toShortString()).withStyle(ChatFormatting.GOLD)).append(" ]\n");}
    static int color(Aspect aspect){return aspect==Aspect.ENTROPY?0xA0A0A0:aspect.getColor();}
    static MutableComponent aspect(Aspect aspect,int amount){return Component.literal(aspect.getTag()+": "+amount).withStyle(style->style.withColor(color(aspect)));}
    static Component finish(MutableComponent report){return report.append(Component.literal("\n------------------------").withStyle(ChatFormatting.DARK_GRAY));}
    static Component node(NodeView node,BlockPos pos){var report=header("wg.abacus.node_header",pos);var aspects=AspectHelper.reduceToPrimals(node.getAspects(),true);for(int i=0;i<GemItem.PRIMALS.length;i++){if(i>0)report.append("\n");var aspect=GemItem.PRIMALS[i];report.append(aspect(aspect,aspects.getAmount(aspect)));}return finish(report);}
}
