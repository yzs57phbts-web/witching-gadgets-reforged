package witchinggadgets;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.wand.WandInteractable;

public class MachineBlock extends BaseEntityBlock implements WandInteractable {
    final String kind;
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public MachineBlock(String kind,Properties p){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new MachineBlock(kind,p));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,kind.equals("essentia_pump")?c.getHorizontalDirection():c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new MachineEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,Content.MACHINE.get(),MachineEntity::tick);}
    @Override protected void randomTick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){if(kind.equals("rose_vines")&&level.getBlockEntity(pos) instanceof MachineEntity machine)machine.growVines(false);}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity){if(kind.equals("rose_vines")&&!level.isClientSide()&&entity instanceof net.minecraft.world.entity.LivingEntity)entity.hurt(entity.damageSources().generic(),1);}
    @Override protected RenderShape getRenderShape(BlockState s){return java.util.Set.of("glimmer_of_light","spinning_wheel","cutting_table","terrestrial_reformer","ethereal_wall","essentia_pump","label_library","rose_vines").contains(kind)||kind.startsWith("focus_")?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return switch(kind){case "cutting_table","label_library"->Block.box(0,0,0,16,12,16);case "spinning_wheel"->Block.box(1,0,1,15,24,15);case "rose_vines"->RoseGeometry.shape(l,p);case "terrestrial_reformer"->Block.box(0,0,0,16,14.5,16);case "essentia_pump"->Block.box(1,0,1,15,16,15);case "wall_mirror"->Block.box(0,0,13,16,32,16);case "glimmer_of_light"->Shapes.empty();default->kind.startsWith("focus_")?Shapes.or(Block.box(3,0,3,13,12,13),Block.box(5,12,5,11,14,11)):Shapes.block();};
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(kind.equals("glimmer_of_light")||kind.equals("rose_vines"))return Shapes.empty();
        if(kind.equals("ethereal_wall")&&l.getBlockEntity(p) instanceof MachineEntity m&&m.active)return Shapes.empty();
        return getShape(s,l,p,c);
    }
    // Lighting runs off-thread: rose selection reads adjacent blocks and must never be used here.
    @Override protected int getLightBlock(BlockState s,BlockGetter l,BlockPos p){return kind.equals("rose_vines")?0:super.getLightBlock(s,l,p);}
    @Override protected boolean propagatesSkylightDown(BlockState s,BlockGetter l,BlockPos p){return kind.equals("rose_vines")||super.propagatesSkylightDown(s,l,p);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(stack.getItem() instanceof thaumcraft.wand.WandItem)return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if(kind.equals("ethereal_wall"))return WallCamouflage.use(stack,state,level,pos,p,hand,hit);
        if(level.getBlockEntity(pos) instanceof MachineEntity m&&m.use(p,hand))return ItemInteractionResult.sidedSuccess(level.isClientSide());
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player p,BlockHitResult hit){if(level.getBlockEntity(pos) instanceof MachineEntity m&&m.use(p,InteractionHand.MAIN_HAND))return InteractionResult.sidedSuccess(level.isClientSide());return InteractionResult.PASS;}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder loot){
        if(kind.equals("rose_vines")){var tool=loot.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);return tool!=null&&tool.getItem() instanceof ShearsItem?java.util.List.of(Content.stack("rose_vines")):java.util.List.of();}
        if(kind.equals("infernal_blast_furnace")){var entity=loot.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);if(entity instanceof MachineEntity m&&m.original!=null){var item=m.original.getBlock().asItem();return item==Items.AIR?java.util.List.of():java.util.List.of(new ItemStack(item));}return java.util.List.of();}return super.getDrops(state,loot);
    }
    @Override public InteractionResult onWandUseOn(Level l,BlockPos pos,Player p,ItemStack wand,Direction side){if(kind.equals("infernal_blast_furnace"))return InteractionResult.sidedSuccess(l.isClientSide());if(kind.equals("rose_vines")){if(!l.isClientSide()&&p.isCreative()&&l.getBlockEntity(pos) instanceof MachineEntity vine)vine.growVines(true);return InteractionResult.sidedSuccess(l.isClientSide());}if(l.getBlockEntity(pos) instanceof MachineEntity m){if(!l.isClientSide()){if(p.isShiftKeyDown()){l.setBlock(pos,l.getBlockState(pos).setValue(FACING,side),3);}else m.activate(p);if(kind.equals("spinning_wheel")||kind.equals("label_library"))Effects.sound(l,net.minecraft.world.phys.Vec3.atCenterOf(pos),thaumcraft.registry.ModSounds.TOOL.get(),.3F,1.9F+l.random.nextFloat()*.2F);}return InteractionResult.sidedSuccess(l.isClientSide());}return InteractionResult.PASS;}
    @Override protected void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){if(!old.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MachineEntity m){if(kind.equals("infernal_blast_furnace")){Multiblock.breakFurnace(level,pos,m);m.dropBlastQueue();}for(int i=0;i<m.items.getSlots();i++)Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),m.items.getStackInSlot(i));}super.onRemove(old,level,pos,next,moving);}
}
