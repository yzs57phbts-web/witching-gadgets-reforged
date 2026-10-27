package witchinggadgets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
/** Stores only the visual state; the original blocks remain in the block entity for dismantling. */
public final class FurnaceBlock extends MachineBlock {
    static final IntegerProperty PART=IntegerProperty.create("part",0,26);
    static final BooleanProperty LIT=BlockStateProperties.LIT;
    static final BooleanProperty SPECIAL=BooleanProperty.create("special");
    FurnaceBlock(Properties p){super("infernal_blast_furnace",p);registerDefaultState(defaultBlockState().setValue(PART,0).setValue(LIT,false).setValue(SPECIAL,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(PART,LIT,SPECIAL);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return s.getValue(PART)==22?net.minecraft.world.phys.shapes.Shapes.empty():super.getCollisionShape(s,l,p,c);}
    @Override protected void entityInside(BlockState s,net.minecraft.world.level.Level l,net.minecraft.core.BlockPos p,net.minecraft.world.entity.Entity e){if(s.getValue(PART)==22&&!l.isClientSide()){
        if(e instanceof net.minecraft.world.entity.item.ItemEntity item){if(BlastRecipes.resolve(item.getItem())==null){Effects.furnaceReject(l,p);item.discard();}else if(l.getBlockEntity(p) instanceof MachineEntity m&&m.acceptBlastInput(item.getItem()))item.discard();}
        else if(e instanceof net.minecraft.world.entity.LivingEntity living&&!living.fireImmune()){living.hurt(l.damageSources().lava(),3);living.igniteForSeconds(10);}
    }super.entityInside(s,l,p,e);}
}
