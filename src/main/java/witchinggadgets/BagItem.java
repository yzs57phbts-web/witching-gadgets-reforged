package witchinggadgets;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public final class BagItem extends Item {
    public final String kind;
    public BagItem(String kind,Properties p){super(p);this.kind=kind;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
        var s=p.getItemInHand(hand);if(!level.isClientSide()){
            if(kind.equals("enderlinked_bag")){p.openMenu(new SimpleMenuProvider((id,inv,player)->ChestMenu.threeRows(id,inv,player.getEnderChestInventory()),s.getHoverName()));return InteractionResultHolder.success(s);}
            if(kind.equals("voidlinked_bag")){VoidFilter.migrate(p,s);p.displayClientMessage(Component.translatable("wg.tooltip.void_filter"),true);open(p,s,18);return InteractionResultHolder.success(s);}
            var t=Data.get(s);if(!t.hasUUID("Owner")){t.putUUID("Owner",p.getUUID());t.putString("OwnerName",p.getName().getString());Data.put(s,t);}
            if(!p.isCreative()&&!t.getBoolean("Unlocked")&&!t.getUUID("Owner").equals(p.getUUID())){p.hurt(p.damageSources().magic(),4);return InteractionResultHolder.fail(s);}
            if(p.isShiftKeyDown()&&(p.isCreative()||t.getUUID("Owner").equals(p.getUUID()))){t.putBoolean("Unlocked",!t.getBoolean("Unlocked"));Data.put(s,t);p.displayClientMessage(Component.translatable(t.getBoolean("Unlocked")?"wg.bag.unlocked":"wg.bag.locked"),true);return InteractionResultHolder.success(s);}
            open(p,s,18);
        }return InteractionResultHolder.sidedSuccess(s,level.isClientSide());
    }
    public static boolean canStore(ItemStack s){return !(s.getItem() instanceof BagItem)&&!(s.getItem() instanceof Gear g&&g.kind.contains("voluminous"))&&!s.has(DataComponents.CONTAINER)&&s.getItem().canFitInsideContainerItems();}
    static Container liveStorage(Player player,ItemStack bag,int size){
        if(player.containerMenu instanceof StorageMenu menu&&menu.holder==bag)return menu.storage;
        return new Storage(bag,size);
    }
    public static class Storage extends SimpleContainer {
        final ItemStack holder;
        boolean loading=true;
        Storage(ItemStack s,int size){super(size);holder=s;s.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(getItems());loading=false;}
        @Override public boolean canPlaceItem(int i,ItemStack s){return holder.is(Content.item("primordial_gauntlet"))?i<5&&s.getItem() instanceof GemItem:canStore(s);}
        @Override public void setChanged(){super.setChanged();if(!loading)holder.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(getItems()));}
    }
    public static void open(Player player,ItemStack stack,int size){
        if(player.level().isClientSide())return;var storage=new Storage(stack,size);int rows=size/9;
        player.openMenu(new SimpleMenuProvider((id,inventory,p)->new StorageMenu(id,inventory,stack,storage,size),stack.getHoverName()),buffer->{var preview=Content.stack(Compat.name(stack));Data.put(preview,Data.get(stack));net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(buffer,preview);buffer.writeVarInt(size);});
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){long total=s.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().mapToInt(ItemStack::getCount).sum();text.add(Component.translatable("wg.tooltip.storage",total));}
}
