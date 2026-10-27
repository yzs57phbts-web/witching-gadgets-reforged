package witchinggadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class WallCamouflage {
    static ItemInteractionResult use(ItemStack held,BlockState wall,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        // Sneaking with a building block must allow extending the wall normally.
        if(player.isShiftKeyDown()||!(held.getItem() instanceof BlockItem item)||item.getBlock()==wall.getBlock())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var context=new BlockPlaceContext(new UseOnContext(player,hand,hit));
        BlockState sample=item.getBlock().getStateForPlacement(context);
        if(sample==null||sample.getRenderShape()!=RenderShape.MODEL)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var component=held.get(DataComponents.BLOCK_STATE);
        if(component!=null)sample=component.apply(sample);
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof MachineEntity machine){machine.original=sample;machine.sync();}
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
