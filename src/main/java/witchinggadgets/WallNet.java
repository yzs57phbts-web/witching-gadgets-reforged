package witchinggadgets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
public final class WallNet {
    private static final WeakHashMap<Level,Cache> CACHE=new WeakHashMap<>();
    private record Cache(long tick,Map<BlockPos,Boolean> powered){}
    public static boolean powered(Level l,BlockPos start){long tick=l.getGameTime()/5;var cached=CACHE.computeIfAbsent(l,k->new Cache(tick,new HashMap<>()));if(cached.tick!=tick){cached=new Cache(tick,new HashMap<>());CACHE.put(l,cached);}Boolean value=cached.powered.get(start);if(value!=null)return value;
        Set<BlockPos> visited=new HashSet<>();ArrayDeque<BlockPos> open=new ArrayDeque<>();open.add(start);boolean powered=false;
        while(!open.isEmpty()&&visited.size()<4096){var pos=open.removeFirst();if(!visited.add(pos)||!l.hasChunkAt(pos)||!l.getBlockState(pos).is(Content.block("ethereal_wall")))continue;powered|=l.hasNeighborSignal(pos);for(Direction d:Direction.values()){var n=pos.relative(d);if(l.hasChunkAt(n)&&l.getBlockState(n).is(Content.block("ethereal_wall"))&&!visited.contains(n))open.add(n);}}
        for(BlockPos pos:visited)cached.powered.put(pos,powered);return powered;
    }
}
