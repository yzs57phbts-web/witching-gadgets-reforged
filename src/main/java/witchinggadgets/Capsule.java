package witchinggadgets;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
public final class Capsule {
    public static void register(){net.minecraft.world.level.block.DispenserBlock.registerBehavior(Content.item("crystal_capsule"),new net.minecraft.core.dispenser.DefaultDispenseItemBehavior(){
        @Override protected ItemStack execute(net.minecraft.core.dispenser.BlockSource source,ItemStack stack){
            var facing=source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);var pos=source.pos().relative(facing);var level=source.level();ItemStack result;
            if(Data.get(stack).getInt("FluidAmount")==1000){var fluid=handler(stack.copyWithCount(1)).getFluidInTank(0);var state=fluid.isEmpty()?net.minecraft.world.level.block.Blocks.AIR.defaultBlockState():fluid.getFluid().defaultFluidState().createLegacyBlock();if(state.isAir()||!level.getBlockState(pos).canBeReplaced())return super.execute(source,stack);level.setBlockAndUpdate(pos,state);result=Content.stack("crystal_capsule");}
            else{var fluid=level.getFluidState(pos);if(fluid.isEmpty()||!fluid.isSource()||!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup))return super.execute(source,stack);if(pickup.pickupBlock(null,level,pos,level.getBlockState(pos)).isEmpty())return super.execute(source,stack);result=filled(new FluidStack(fluid.getType(),1000));}
            stack.shrink(1);if(stack.isEmpty())return result;var remainder=source.blockEntity().insertItem(result);if(!remainder.isEmpty())super.execute(source,remainder);return stack;
        }
    });}
    public static net.neoforged.neoforge.fluids.capability.IFluidHandlerItem handler(ItemStack stack){return new net.neoforged.neoforge.fluids.capability.IFluidHandlerItem(){
        public ItemStack getContainer(){return stack;}public int getTanks(){return 1;}public int getTankCapacity(int tank){return 1000;}public boolean isFluidValid(int tank,FluidStack fluid){return !fluid.isEmpty();}
        public FluidStack getFluidInTank(int tank){var tag=Data.get(stack);return tag.getInt("FluidAmount")==1000?new FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(tag.getString("Fluid"))),1000):FluidStack.EMPTY;}
        public int fill(FluidStack resource,IFluidHandler.FluidAction action){if(stack.getCount()!=1||resource.getAmount()<1000||!getFluidInTank(0).isEmpty())return 0;if(action.execute())Data.put(stack,Data.get(filled(resource.copyWithAmount(1000))));return 1000;}
        public FluidStack drain(FluidStack fluid,IFluidHandler.FluidAction action){return FluidStack.isSameFluidSameComponents(fluid,getFluidInTank(0))?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        public FluidStack drain(int maximum,IFluidHandler.FluidAction action){var fluid=getFluidInTank(0);if(stack.getCount()!=1||maximum<1000||fluid.isEmpty())return FluidStack.EMPTY;if(action.execute()){var tag=Data.get(stack);tag.remove("Fluid");tag.remove("FluidAmount");Data.put(stack,tag);}return fluid;}
    };}
    public static InteractionResult use(UseOnContext c){
        var l=c.getLevel();var s=c.getItemInHand();var p=c.getPlayer();if(p==null)return InteractionResult.PASS;var t=Data.get(s);var handler=l.getCapability(Capabilities.FluidHandler.BLOCK,c.getClickedPos(),c.getClickedFace());
        if(handler!=null){
            if(t.getInt("FluidAmount")==1000){var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(t.getString("Fluid")));if(handler.fill(new FluidStack(fluid,1000),IFluidHandler.FluidAction.SIMULATE)!=1000)return InteractionResult.PASS;if(!l.isClientSide()){handler.fill(new FluidStack(fluid,1000),IFluidHandler.FluidAction.EXECUTE);replaceOne(c,Content.stack("crystal_capsule"));}return InteractionResult.sidedSuccess(l.isClientSide());}
            var drained=handler.drain(1000,IFluidHandler.FluidAction.SIMULATE);if(drained.getAmount()!=1000)return InteractionResult.PASS;if(!l.isClientSide())replaceOne(c,filled(handler.drain(drained,IFluidHandler.FluidAction.EXECUTE)));return InteractionResult.sidedSuccess(l.isClientSide());
        }
        var pos=c.getClickedPos();var fluid=l.getFluidState(pos);
        if(t.getInt("FluidAmount")==1000){pos=pos.relative(c.getClickedFace());if(!l.mayInteract(p,pos)||!l.getBlockState(pos).canBeReplaced())return InteractionResult.FAIL;var stored=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(t.getString("Fluid")));if(stored==Fluids.EMPTY||stored.defaultFluidState().createLegacyBlock().isAir())return InteractionResult.PASS;if(!l.isClientSide()){l.setBlockAndUpdate(pos,stored.defaultFluidState().createLegacyBlock());replaceOne(c,Content.stack("crystal_capsule"));}return InteractionResult.sidedSuccess(l.isClientSide());}
        if(!fluid.isEmpty()&&fluid.isSource()&&l.mayInteract(p,pos)&&t.getInt("FluidAmount")==0&&l.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup){if(!l.isClientSide()){if(pickup.pickupBlock(p,l,pos,l.getBlockState(pos)).isEmpty())return InteractionResult.PASS;replaceOne(c,filled(new FluidStack(fluid.getType(),1000)));}return InteractionResult.sidedSuccess(l.isClientSide());}
        return InteractionResult.PASS;
    }
    static ItemStack filled(FluidStack fluid){var out=Content.stack("crystal_capsule");var t=Data.get(out);t.putString("Fluid",net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());t.putInt("FluidAmount",fluid.getAmount());Data.put(out,t);return out;}
    static void replaceOne(UseOnContext c,ItemStack replacement){var p=c.getPlayer();var held=c.getItemInHand();if(held.getCount()==1)p.setItemInHand(c.getHand(),replacement);else{held.shrink(1);if(!p.addItem(replacement))p.drop(replacement,false);}}
}
