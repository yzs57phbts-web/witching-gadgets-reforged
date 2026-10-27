package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.aspect.*;
import thaumcraft.device.EssentiaMirrorBlockEntity;
import thaumcraft.network.EssentiaSourceFxPayload;
import thaumcraft.platform.ModNetwork;

/** Mirror requests are isolated by aspect, so an exhausted channel cannot pause unrelated channels. */
final class PumpMirrorAccess {
    record Key(BlockPos mirror,Aspect aspect){}
    static final class Sources {final List<BlockPos> positions=new ArrayList<>();long retryAt;boolean scanned;}
    private static final WeakHashMap<ServerLevel,Map<Key,Sources>> CACHE=new WeakHashMap<>();
    static boolean take(EssentiaMirrorBlockEntity mirror,Aspect aspect){
        if(!mirror.isLinkValid(true)||!(mirror.target(true) instanceof EssentiaMirrorBlockEntity partner)||!(partner.getLevel() instanceof ServerLevel level))return false;
        var key=new Key(partner.getBlockPos(),aspect);var sources=CACHE.computeIfAbsent(level,l->new HashMap<>()).computeIfAbsent(key,k->new Sources());
        if(level.getGameTime()<sources.retryAt)return false;
        if(!sources.scanned){
            sources.positions.clear();var origin=partner.getBlockPos();var direction=partner.getFacing();
            // The native mirror's 8-block directional source volume, excluding mirror recursion.
            for(int first=-8;first<=8;first++)for(int second=-8;second<=8;second++)for(int depth=0;depth<8;depth++){
                BlockPos pos=direction.getStepY()!=0?origin.offset(first,depth*direction.getStepY(),second):direction.getStepX()==0?origin.offset(first,second,depth*direction.getStepZ()):origin.offset(depth*direction.getStepX(),first,second);
                if(pos.equals(origin)||!level.hasChunkAt(pos))continue;
                var tile=level.getBlockEntity(pos);if(tile instanceof AspectSource&&!(tile instanceof EssentiaMirrorBlockEntity))sources.positions.add(pos);
            }
            sources.scanned=true;
        }
        for(var pos:sources.positions){
            if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof AspectSource source)||source instanceof EssentiaMirrorBlockEntity)continue;
            if(source.takeFromContainer(aspect,1)){
                var origin=partner.getBlockPos();ModNetwork.sendToNear(level,null,origin.getX(),origin.getY(),origin.getZ(),32,new EssentiaSourceFxPayload(origin,(byte)(origin.getX()-pos.getX()),(byte)(origin.getY()-pos.getY()),(byte)(origin.getZ()-pos.getZ()),aspect.getColor()));
                return true;
            }
        }
        sources.scanned=false;sources.retryAt=level.getGameTime()+100;return false;
    }
}
