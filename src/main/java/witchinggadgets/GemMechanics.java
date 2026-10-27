package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.aspect.Aspect;

final class GemMechanics {
    static boolean ore(Level level,BlockPos pos){var state=level.getBlockState(pos);return state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("_ore");}
    static List<BlockPos> ores(Level level,BlockPos origin){
        var search=level.getBlockState(origin).getBlock();var found=new ArrayList<BlockPos>();var visited=new HashSet<BlockPos>();var open=new ArrayDeque<BlockPos>();open.add(origin);visited.add(origin);
        int processed=0;while(!open.isEmpty()&&processed++<400){var pos=open.removeFirst();if(ore(level,pos))found.add(pos);for(var face:Direction.values()){var next=pos.relative(face);if(next.distSqr(origin)<32&&level.hasChunkAt(next)&&!visited.contains(next)&&(level.getBlockState(next).is(search)||ore(level,next))){visited.add(next);open.add(next);}}}
        return found;
    }
    static void recharge(ItemStack gem,Player player,Aspect aspect){
        var ref=thaumcraft.vis.VisRelayBlockEntity.NEARBY_PLAYERS.get(player.getId());var relay=ref==null?null:ref.get();
        if(relay==null||relay.isRemoved()||relay.getLevel()!=player.level()||!player.level().hasChunkAt(relay.getBlockPos())||relay.getBlockPos().distToCenterSqr(player.position())>=26)return;
        if(relay.consumeVis(aspect,1)>0){Data.integer(gem,"Spent",Math.max(0,Data.get(gem).getInt("Spent")-1));relay.triggerConsumeEffect(aspect);Effects.near(player.level(),Effects.Kind.GEM_RECHARGE,Vec3.atCenterOf(relay.getBlockPos()),Vec3.ZERO,aspect.getColor(),player.getId());}
    }
}
