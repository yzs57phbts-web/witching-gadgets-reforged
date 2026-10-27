package witchinggadgets;

import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** The block loot event has a copied tool but no player; retain the native drop call's owner. */
public final class HarvestContext {
    private static final ThreadLocal<Player> PLAYER=new ThreadLocal<>();
    public static Player player(){return PLAYER.get();}
    public static <T> T during(Entity entity,Supplier<T> drops){var previous=PLAYER.get();if(entity instanceof Player player)PLAYER.set(player);else PLAYER.remove();try{return drops.get();}finally{if(previous==null)PLAYER.remove();else PLAYER.set(previous);}}
}
