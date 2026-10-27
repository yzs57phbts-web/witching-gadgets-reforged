package witchinggadgets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
public final class MachineMenu extends AbstractContainerMenu {
    public final MachineEntity machine;
    final int count;
    public MachineMenu(int id,Inventory inv,RegistryFriendlyByteBuf data){this(id,inv,clientMachine(inv,data));}
    static MachineEntity clientMachine(Inventory inv,RegistryFriendlyByteBuf data){var pos=data.readBlockPos();String kind=data.readUtf();if(!java.util.Set.of("spinning_wheel","cutting_table","label_library").contains(kind))throw new IllegalArgumentException("Unsupported machine menu: "+kind);var model=new MachineEntity(pos,Content.block(kind).defaultBlockState());model.setLevel(inv.player.level());return model;}
    public MachineMenu(int id,Inventory inv,MachineEntity machine){
        super(Content.MACHINE_MENU.get(),id);this.machine=machine;if(!java.util.Set.of("spinning_wheel","cutting_table","label_library").contains(machine.kind))throw new IllegalArgumentException("This machine has no menu: "+machine.kind);
        count=switch(machine.kind){case "spinning_wheel"->6;case "cutting_table"->5;case "label_library"->2;default->3;};
        var handler=new MachineCrafting(machine);
        for(int i=0;i<count;i++){final int slot=i;int x=20+i*23,y=40;if(machine.kind.equals("spinning_wheel")){x=i==5?142:16;y=i==5?64:16+24*i;}if(machine.kind.equals("cutting_table")){x=i==0?42:i==4?118:60+(i-1)*20;y=i==0||i==4?24:56;}
            if(machine.kind.equals("label_library")){x=8;y=i==0?8:51;}
            addSlot(new SlotItemHandler(handler,i,x,y){@Override public boolean mayPlace(ItemStack stack){return handler.isItemValid(slot,stack);}});
        }
        int px=machine.kind.equals("spinning_wheel")?16:8,py=machine.kind.equals("spinning_wheel")?151:84;for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,px+col*18,py+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,px+col*18,py+58));
        addDataSlots(new ContainerData(){public int get(int i){return switch(i){case 0->machine.progress;case 1->machine.gemCut;default->machine.essentia==null?-1:machine.essentia.ordinal();};}public void set(int i,int v){switch(i){case 0->machine.progress=v;case 1->machine.gemCut=v;default->machine.essentia=v<0?null:thaumcraft.aspect.Aspect.values()[Math.floorMod(v,thaumcraft.aspect.Aspect.values().length)];}}public int getCount(){return 3;}});
    }
    @Override public boolean clickMenuButton(Player p,int id){if(!stillValid(p))return false;if(id==0||id==1)machine.gemCut=id;else if(id==2)machine.activate(p);else if(id>=100&&id<100+thaumcraft.aspect.Aspect.values().length){var a=thaumcraft.aspect.Aspect.values()[id-100];if(!p.isCreative()&&!thaumcraft.research.Knowledge.get(p).hasDiscoveredAspect(a))return false;machine.essentia=a;}else return false;machine.sync();return true;}
    @Override public boolean stillValid(Player p){return !machine.isRemoved()&&p.distanceToSqr(machine.getBlockPos().getX()+.5,machine.getBlockPos().getY()+.5,machine.getBlockPos().getZ()+.5)<64;}
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index==count-1&&new MachineCrafting(machine).derived()){if(!moveItemStackTo(stack,count,slots.size(),true))return ItemStack.EMPTY;new MachineCrafting(machine).extractItem(index,1,false);return copy;}
        if(index<count){if(!moveItemStackTo(stack,count,slots.size(),true))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,0,count-1,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;}
}

