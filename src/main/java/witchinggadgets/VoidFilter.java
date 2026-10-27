package witchinggadgets;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class VoidFilter {
    static void migrate(Player player,ItemStack bag){
        if(Data.get(bag).getBoolean("GhostFilter"))return;
        var storage=new BagItem.Storage(bag,18);
        for(int i=0;i<storage.getContainerSize();i++){
            var previous=storage.getItem(i);if(previous.isEmpty())continue;
            var refund=previous.copy();storage.setItem(i,previous.copyWithCount(1));
            if(!player.addItem(refund))player.drop(refund,false);
        }
        Data.bool(bag,"GhostFilter",true);
    }
}
