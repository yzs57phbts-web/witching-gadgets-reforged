package witchinggadgets;

import net.minecraft.world.item.*;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import thaumcraft.aspect.*;

/** Recipe output is a preview until extracted; it is never saved or dropped as a real item. */
final class MachineCrafting implements IItemHandlerModifiable {
    final MachineEntity machine;
    final int size;
    MachineCrafting(MachineEntity machine){this.machine=machine;size=switch(machine.kind){case "spinning_wheel"->6;case "cutting_table"->5;case "label_library"->2;default->0;};}
    static boolean valid(MachineEntity machine,int slot,ItemStack stack){
        if(stack.isEmpty())return true;
        return switch(machine.kind){
            case "spinning_wheel"->slot<5&&(stack.is(Items.STRING)||stack.is(Items.GOLD_NUGGET)||stack.is(Content.tc("thaumium_nugget")));
            case "label_library"->slot==0&&stack.is(Content.tc("jar_label"));
            case "cutting_table"->{if(slot==0)yield GemMaterials.material(stack)!=null;if(slot<1||slot>3)yield false;var aspects=AspectKeys.ASPECTS.get(stack);yield (stack.is(Content.tc("essentia_phial"))||stack.is(Content.tc("crystallized_essence")))&&aspects!=null&&aspects.size()==1&&aspects.getAspects().getFirst().isPrimal();}
            default->false;
        };
    }
    static ItemStack preview(MachineEntity machine){
        if(machine.kind.equals("label_library")){
            if(machine.essentia==null||!machine.items.getStackInSlot(0).is(Content.tc("jar_label")))return ItemStack.EMPTY;
            var output=new ItemStack(Content.tc("jar_label"));AspectKeys.ASPECTS.set(output,new AspectList().add(machine.essentia,0));return output;
        }
        if(!machine.kind.equals("cutting_table"))return ItemStack.EMPTY;
        String material=GemMaterials.material(machine.items.getStackInSlot(0));if(material==null)return ItemStack.EMPTY;
        Aspect aspect=null;int count=0;
        for(int i=1;i<=3;i++){
            var input=machine.items.getStackInSlot(i);if(input.isEmpty())continue;
            if(!valid(machine,i,input))return ItemStack.EMPTY;
            var candidate=AspectKeys.ASPECTS.get(input).getAspects().getFirst();if(aspect!=null&&aspect!=candidate)return ItemStack.EMPTY;
            aspect=candidate;count++;
        }
        if(aspect==null)return ItemStack.EMPTY;
        boolean affinity=GemMaterials.affinity(material,aspect);int potency=affinity?Math.max(0,count-(machine.gemCut==1?1:0)):0;
        int brittle=affinity?0:count+(GemMaterials.aversion(material,aspect)?1:0);
        return GemItem.create(aspect,machine.gemCut,potency,brittle);
    }
    static void consume(MachineEntity machine){
        machine.items.extractItem(0,1,false);
        if(machine.kind.equals("cutting_table"))for(int i=1;i<=3;i++){
            var input=machine.items.extractItem(i,1,false);if(input.is(Content.tc("essentia_phial")))machine.emit(new ItemStack(Content.tc("glass_phial")));
        }
        machine.sync();
    }
    boolean derived(){return !machine.kind.equals("spinning_wheel")&&machine.items.getStackInSlot(size-1).isEmpty();}
    @Override public int getSlots(){return size;}
    @Override public ItemStack getStackInSlot(int slot){return slot==size-1&&derived()?preview(machine):machine.items.getStackInSlot(slot);}
    @Override public int getSlotLimit(int slot){return slot==size-1?64:machine.items.getSlotLimit(slot);}
    @Override public boolean isItemValid(int slot,ItemStack stack){return valid(machine,slot,stack);}
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?machine.items.insertItem(slot,stack,simulate):stack;}
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
        if(slot!=size-1||!derived())return machine.items.extractItem(slot,amount,simulate);
        var output=preview(machine);if(amount<=0||output.isEmpty())return ItemStack.EMPTY;
        if(!simulate)consume(machine);return output;
    }
    @Override public void setStackInSlot(int slot,ItemStack stack){machine.items.setStackInSlot(slot,stack);}
}
