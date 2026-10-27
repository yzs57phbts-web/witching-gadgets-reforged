package witchinggadgets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import thaumcraft.aspect.*;
import thaumcraft.research.*;

public class UtilityItem extends Item {
    public final String kind;
    public UtilityItem(String kind,Properties p){super(p);this.kind=kind;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> text,TooltipFlag flag){if(kind.equals("crystal_capsule")){var fluid=Capsule.handler(stack.copyWithCount(1)).getFluidInTank(0);if(!fluid.isEmpty())text.add(fluid.getHoverName());}}
    @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity eater){var result=super.finishUsingItem(s,l,eater);if(kind.equals("brain_jerky")&&eater instanceof ServerPlayer p)for(int pass=l.random.nextInt(3);pass>=0;pass--)ResearchManager.addAspectPool(p,GemItem.PRIMALS[l.random.nextInt(6)],1+l.random.nextInt(2));return result;}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
        var s=p.getItemInHand(hand);if(s.getFoodProperties(p)!=null)return super.use(l,p,hand);
        if(kind.equals("crystal_capsule")){var hit=getPlayerPOVHitResult(l,p,Data.get(s).getInt("FluidAmount")==0?net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY:net.minecraft.world.level.ClipContext.Fluid.NONE);if(hit.getType()==HitResult.Type.BLOCK)return new InteractionResultHolder<>(Capsule.use(new UseOnContext(p,hand,hit)),p.getItemInHand(hand));return InteractionResultHolder.pass(s);}
        if(l.isClientSide())return InteractionResultHolder.success(s);
        switch(kind){
            case "primordial_gauntlet"->{boolean activated=false;var stored=s.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);var gems=new ArrayList<ItemStack>();stored.stream().forEach(g->gems.add(g.copy()));int index=Math.floorMod(Data.get(s).getInt("Affinity"),5);if(index<gems.size()&&gems.get(index).getItem() instanceof GemItem){activated=GemItem.activate(gems.get(index),p,true);s.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(gems));}if(p.isShiftKeyDown()&&!activated)BagItem.open(p,s,9);return InteractionResultHolder.success(s);}
            case "scan_camera"->{if(p instanceof ServerPlayer sp)Tools.photo(sp);return InteractionResultHolder.success(s);}
            case "developed_iconograph"->{if(p instanceof ServerPlayer sp)Tools.developScan(sp,s);return InteractionResultHolder.success(s);}
            case "guiding_thread"->{Tools.toggleGuidance(p,s);return InteractionResultHolder.success(s);}
            default->{return InteractionResultHolder.pass(s);}
        }
    }
    @Override public InteractionResult useOn(UseOnContext c){
        var p=c.getPlayer();if(p==null)return InteractionResult.PASS;var l=c.getLevel();var pos=c.getClickedPos();var held=c.getItemInHand();
        if(kind.equals("arcane_abacus")&&Tools.abacus(p,l,pos))return InteractionResult.sidedSuccess(l.isClientSide());
        if(kind.equals("gemcutters_tools")&&l.getBlockState(pos).is(thaumcraft.registry.ModBlocks.TABLE.get())){if(!l.isClientSide())l.setBlockAndUpdate(pos,Content.block("cutting_table").defaultBlockState().setValue(MachineBlock.FACING,p.getDirection().getOpposite()));return InteractionResult.sidedSuccess(l.isClientSide());}
        if(kind.equals("primordial_gauntlet")&&l.getBlockEntity(pos) instanceof thaumcraft.node.NodeBlockEntity node){if(!l.isClientSide()){var aspects=AspectHelper.reduceToPrimals(node.getAspects(),true);var t=Data.get(held);CompoundTag stored=new CompoundTag();for(Aspect a:aspects.getAspects()){int value=aspects.getAmount(a);if(node.getNodeModifier()!=null)value=(int)(value*switch(node.getNodeModifier()){case BRIGHT->1.2F;case PALE->.8F;case FADING->.5F;});value=(int)Math.sqrt(value);if(node.getNodeType()==thaumcraft.node.NodeType.UNSTABLE)value+=l.random.nextInt(5)-2;if(value>0)stored.putInt(a.name(),value);}t.put("Node",stored);t.putString("NodeType",node.getNodeType().name());if(node.getNodeModifier()!=null)t.putString("NodeModifier",node.getNodeModifier().name());else t.remove("NodeModifier");Data.put(held,t);l.removeBlock(pos,false);}return InteractionResult.sidedSuccess(l.isClientSide());}
        if(kind.equals("crystal_capsule")){return Capsule.use(c);}
        return super.useOn(c);
    }
    @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){
        if(!l.isClientSide()&&kind.equals("guiding_thread")&&e instanceof ServerPlayer sp)Tools.tickGuidance(sp,s,selected);
        if(l.isClientSide()||!kind.equals("primordial_gauntlet")||e.tickCount%10!=0)return;var node=Data.get(s).getCompound("Node");if(node.isEmpty())return;if(e instanceof Player player&&player.containerMenu instanceof StorageMenu menu&&menu.holder==s){for(int i=0;i<5;i++){var gem=menu.storage.getItem(i);var aspect=GemItem.aspect(gem);if(aspect!=null)Data.integer(gem,"Spent",Math.max(0,Data.get(gem).getInt("Spent")-node.getInt(aspect.name())));}menu.storage.setChanged();menu.broadcastChanges();return;}var gems=new ArrayList<ItemStack>();s.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().forEach(g->{var copy=g.copy();Aspect a=GemItem.aspect(copy);if(a!=null)Data.integer(copy,"Spent",Math.max(0,Data.get(copy).getInt("Spent")-node.getInt(a.name())));gems.add(copy);});s.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(gems));
    }
}
