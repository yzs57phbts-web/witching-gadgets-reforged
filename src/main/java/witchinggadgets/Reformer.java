package witchinggadgets;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.aspect.*;
import thaumcraft.essentia.Essentia;
import thaumcraft.platform.BiomeEdit;

final class Reformer {
    record Focus(Aspect aspect,String biome){}
    static Focus focus(MachineEntity machine){
        if(machine.getLevel()==null)return null;
        var block=machine.getLevel().getBlockState(machine.getBlockPos().above()).getBlock();
        if(!(block instanceof MachineBlock device))return null;
        return switch(device.kind){
            case "focus_plains"->new Focus(Aspect.PLANT,"minecraft:plains");
            case "focus_cold_taiga"->new Focus(Aspect.COLD,"minecraft:snowy_taiga");
            case "focus_desert"->new Focus(Aspect.FIRE,"minecraft:desert");
            case "focus_jungle"->new Focus(Aspect.TREE,"minecraft:jungle");
            case "focus_hell"->new Focus(Aspect.DARKNESS,"minecraft:nether_wastes");
            case "focus_taint"->new Focus(Aspect.TAINT,"thaumcraft:tainted_land");
            case "focus_mushroom"->new Focus(Aspect.SLIME,"minecraft:mushroom_fields");
            default->null;
        };
    }
    static void tick(MachineEntity machine,ServerLevel level){
        Focus focus=focus(machine);
        Aspect needed=focus==null?null:focus.aspect();
        if(machine.essentia!=needed){machine.essentia=needed;machine.amount=machine.reformerEssentia.getAmount(needed);machine.sync();}
        if(focus==null)return;
        if(machine.age%5==0){
            var source=Essentia.getConnectable(level,machine.getBlockPos(),Direction.DOWN);
            if(source!=null&&source.canOutputTo(Direction.UP)&&source.getSuctionAmount(Direction.UP)<128&&source.takeEssentia(needed,1,Direction.UP)==1)machine.addToContainer(needed,1);
        }
        if(machine.age%20!=0||machine.reformerEssentia.getAmount(needed)<4)return;
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME,ResourceLocation.parse(focus.biome()))).orElse(null);
        if(biome==null)return;
        // Query the actual quart column, not the jittered visual biome lookup at a block edge.
        for(int attempt=0;attempt<=40;attempt++){
            BlockPos target=machine.getBlockPos().offset(level.random.nextInt(16)-level.random.nextInt(16),0,level.random.nextInt(16)-level.random.nextInt(16));
            if(!level.hasChunkAt(target)||BiomeEdit.getColumnBiome(level,target).is(biome))continue;
            if(thaumcraft.vis.VisNet.drainVis(level,machine.getBlockPos(),Aspect.EARTH,4)!=4)return;
            if(BiomeEdit.setColumn(level,target,biome)){
                machine.takeFromContainer(needed,4);
                Effects.near(level,Effects.Kind.REFORMER_VENT,net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos()),net.minecraft.world.phys.Vec3.ZERO,needed.getColor(),0);
            }
            return;
        }
    }
}
