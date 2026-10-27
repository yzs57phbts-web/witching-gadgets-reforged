package witchinggadgets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
public final class StorageMenu extends AbstractContainerMenu {
    final ItemStack holder;
    final Container storage;
    final int size,count;
    final String kind;
    StorageMenu(int id,Inventory inv,RegistryFriendlyByteBuf data){this(id,inv,ItemStack.STREAM_CODEC.decode(data),data.readVarInt());}
    private StorageMenu(int id,Inventory inv,ItemStack holder,int size){this(id,inv,holder,new SimpleContainer(size),size);}
    StorageMenu(int id,Inventory inv,ItemStack holder,Container storage,int size){
        super(Content.STORAGE_MENU.get(),id);this.holder=holder;this.storage=storage;this.size=size;kind=Compat.name(holder);count=kind.equals("primordial_gauntlet")?5:size;
        int[][] gems={{60,6},{100,6},{57,42},{80,53},{103,42}};
        for(int i=0;i<count;i++){int x,y;if(count==5){x=gems[i][0];y=gems[i][1];}else if(kind.equals("voidlinked_bag")){x=29+i%6*20;y=7+i/6*20;}else if(size==27){x=8+i%9*18;y=9+i/9*18;}else{x=35+i%6*18;y=9+i/6*18;}addSlot(new Slot(storage,i,x,y){@Override public boolean mayPlace(ItemStack s){return kind.equals("primordial_gauntlet")?s.getItem() instanceof GemItem:BagItem.canStore(s);}});}
        int py=count==5?84:82;for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,py+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,py+58));
    }
    @Override public boolean stillValid(Player who){return who.level().isClientSide()||!holder.isEmpty()&&(who.getInventory().items.stream().anyMatch(s->s==holder)||who.getOffhandItem()==holder||Gear.equipped(who).stream().anyMatch(s->s==holder));}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return !(kind.equals("voidlinked_bag")&&slot.index<count)&&super.canTakeItemForPickAll(stack,slot);}
    @Override public void clicked(int slot,int button,ClickType type,Player who){
        if(kind.equals("voidlinked_bag")){
            if(slot>=0&&slot<count){
                if(type==ClickType.PICKUP){var cursor=getCarried();storage.setItem(slot,cursor.isEmpty()?ItemStack.EMPTY:cursor.copyWithCount(1));}
                else if(type==ClickType.QUICK_MOVE||type==ClickType.THROW)storage.setItem(slot,ItemStack.EMPTY);
                else if(type==ClickType.SWAP&&button>=0&&button<9){var sample=who.getInventory().getItem(button);storage.setItem(slot,sample.isEmpty()?ItemStack.EMPTY:sample.copyWithCount(1));}
                broadcastChanges();return;
            }
            if(type==ClickType.QUICK_CRAFT)return;
        }
if(slot>=0&&slot<slots.size()&&slots.get(slot).getItem()==holder)return;if(type==ClickType.SWAP&&button>=0&&button<9&&who.getInventory().getItem(button)==holder)return;super.clicked(slot,button,type,who);}
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||slot.getItem()==holder)return ItemStack.EMPTY;
        if(kind.equals("voidlinked_bag")){if(index<count){storage.setItem(index,ItemStack.EMPTY);return ItemStack.EMPTY;}for(int i=0;i<count;i++){if(ItemStack.isSameItemSameComponents(storage.getItem(i),slot.getItem()))return ItemStack.EMPTY;if(storage.getItem(i).isEmpty()){storage.setItem(i,slot.getItem().copyWithCount(1));broadcastChanges();return ItemStack.EMPTY;}}return ItemStack.EMPTY;}var s=slot.getItem();var copy=s.copy();if(index<count){if(!moveItemStackTo(s,count,slots.size(),true))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,0,count,false))return ItemStack.EMPTY;if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return copy;}
}
