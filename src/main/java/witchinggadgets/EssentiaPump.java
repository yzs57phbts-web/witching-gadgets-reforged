package witchinggadgets;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.device.EssentiaMirrorBlockEntity;
import thaumcraft.essentia.Essentia;

final class EssentiaPump {
    final List<BlockPos> mirrors=new ArrayList<>();
    Direction scannedFacing;
    long nextScan;
    void tick(MachineEntity pump,ServerLevel level){
        if(pump.amount>0)return;
        var output=Essentia.getConnectable(level,pump.getBlockPos(),pump.facing());
        if(output==null||!output.canInputFrom(pump.facing().getOpposite()))return;
        var aspect=output.getSuctionType(pump.facing().getOpposite());
        if(aspect==null||output.getSuctionAmount(pump.facing().getOpposite())<=0)return;
        Direction back=pump.facing().getOpposite();
        if(scannedFacing!=back||level.getGameTime()>=nextScan){
            mirrors.clear();scannedFacing=back;nextScan=level.getGameTime()+20;
            for(int h=-8;h<=8;h++)for(int w=-8;w<=8;w++)for(int depth=1;depth<8;depth++){
                BlockPos target=pump.getBlockPos().offset(back.getStepX()!=0?depth*back.getStepX():w,back.getStepY()!=0?depth*back.getStepY():h,back.getStepZ()!=0?depth*back.getStepZ():back.getStepX()!=0?w:h);
                if(level.hasChunkAt(target)&&level.getBlockEntity(target) instanceof EssentiaMirrorBlockEntity)mirrors.add(target);
            }
        }
        for(var pos:mirrors){
            if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof EssentiaMirrorBlockEntity mirror))continue;
            Vec3 from=Vec3.atCenterOf(pump.getBlockPos()).add(back.getStepX(),back.getStepY(),back.getStepZ());
            // Aim at the exposed face, so a wall supporting the mirror cannot occlude it.
            Vec3 to=Vec3.atCenterOf(pos).add(mirror.getFacing().getStepX()*.45,mirror.getFacing().getStepY()*.45,mirror.getFacing().getStepZ()*.45);
            var hit=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,CollisionContext.empty()));
            if(hit.getType()!=HitResult.Type.MISS&&!hit.getBlockPos().equals(pos)&&!hit.getBlockPos().equals(pump.getBlockPos()))continue;
            if(PumpMirrorAccess.take(mirror,aspect)){
                pump.essentia=aspect;pump.amount=1;pump.sync();
                Effects.near(level,Effects.Kind.ESSENTIA_SOURCE,Vec3.atCenterOf(pump.getBlockPos()),Vec3.atCenterOf(pos),aspect.getColor(),0);
                return;
            }
        }
    }
}
