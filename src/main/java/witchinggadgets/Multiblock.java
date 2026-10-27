package witchinggadgets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.aspect.*;
import thaumcraft.research.ResearchManager;
public final class Multiblock {
    private static boolean dismantling;
    public static Block expectedBrick(int position){return position<9&&position!=4?Blocks.NETHER_BRICKS:position==10||position==12||position==13||position==14||position==16?Blocks.SOUL_SAND:Blocks.OBSIDIAN;}
    public static boolean valid(Level l,BlockPos root){
        for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
            BlockPos pos=root.offset(x,y,z);if(!l.hasChunkAt(pos))return false;BlockState s=l.getBlockState(pos);int i=y*9+(z+1)*3+x+1;
            if(y<2){if(!s.is(expectedBrick(i)))return false;}
            else if(x==0&&z==0){if(!s.is(Blocks.LAVA)||!s.getFluidState().isSource())return false;}
            else{if(!s.is(Blocks.NETHER_BRICK_STAIRS)||s.getValue(StairBlock.HALF)!=net.minecraft.world.level.block.state.properties.Half.BOTTOM)return false;Direction d=s.getValue(StairBlock.FACING);if(!((x<0&&d==Direction.EAST)||(x>0&&d==Direction.WEST)||(z<0&&d==Direction.SOUTH)||(z>0&&d==Direction.NORTH)))return false;}
        }return true;
    }
    public static boolean tryForm(Level l,BlockPos clicked,Player p,ItemStack wand){
        return tryForm(l,clicked,p,wand,p.getDirection().getOpposite());
    }
    public static boolean tryForm(Level l,BlockPos clicked,Player p,ItemStack wand,Direction clickedFace){
        if(l.isClientSide()||!p.isCreative()&&!ResearchManager.isComplete(p,"INFERNALBLASTFURNACE"))return false;
        for(BlockPos pos:BlockPos.betweenClosed(clicked.offset(-1,0,-1),clicked.offset(1,2,1))){if(!l.getBlockState(pos).is(Blocks.LAVA))continue;BlockPos root=pos.below(2).immutable();if(!valid(l,root))continue;
            if(!p.isCreative()&&(!(wand.getItem() instanceof thaumcraft.wand.WandItem casting)||!casting.consumeAllVisCrafting(wand,p,new AspectList().add(Aspect.FIRE,50).add(Aspect.EARTH,50).add(Aspect.ENTROPY,50),true)))return false;
            Direction exit=clickedFace!=null&&clickedFace.getAxis().isHorizontal()?clickedFace:Direction.getNearest(p.getX()-root.getX()-.5,0,p.getZ()-root.getZ()-.5);
            for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
                BlockPos at=root.offset(x,y,z);BlockState original=l.getBlockState(at);l.setBlock(at,Content.block("infernal_blast_furnace").defaultBlockState().setValue(MachineBlock.FACING,exit).setValue(FurnaceBlock.PART,y*9+(z+1)*3+x+1),3);
                if(l.getBlockEntity(at) instanceof MachineEntity m){m.master=root;m.original=original;m.position=y*9+(z+1)*3+x+1;m.active=true;m.sync();}
            }return true;
        }return false;
    }
    public static void breakFurnace(Level l,BlockPos broken,MachineEntity part){
        if(dismantling||part.master==null)return;dismantling=true;
        try{for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
            BlockPos at=part.master.offset(x,y,z);if(at.equals(broken)||!l.hasChunkAt(at))continue;if(l.getBlockEntity(at) instanceof MachineEntity m&&m.kind.equals("infernal_blast_furnace")&&m.original!=null)l.setBlock(at,m.original,3);
        }}finally{dismantling=false;}
    }
    public static int bellows(MachineEntity m){int n=0;for(Direction d:Direction.Plane.HORIZONTAL){BlockPos pos=m.getBlockPos().relative(d,2);if(m.getLevel().hasChunkAt(pos)&&m.getLevel().getBlockState(pos).is(thaumcraft.registry.ModBlocks.ARCANE_BELLOWS.get())&&m.getLevel().getBlockState(pos).getValue(thaumcraft.alchemy.BellowsBlock.FACING)==d.getOpposite()&&!m.getLevel().hasNeighborSignal(pos))n++;}return n;}
}
