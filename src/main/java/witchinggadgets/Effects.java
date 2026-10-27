package witchinggadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.*;

/** Release 1.1.10 feedback. Server authority; no client classes on a dedicated server. */
public final class Effects {
    public static java.util.function.Consumer<Payload> client=p->{};
    public static java.util.function.Consumer<MachineEntity> clientMachine=m->{};
    public enum Kind { CAMERA_RUNES, REFORMER_VENT, SAUNA_PLAYER, ORE_MARK, NODE_WISP, FURNACE_BREAK, ESSENTIA_SOURCE, GEM_RECHARGE }
    public static void init(IEventBus bus){bus.addListener(Effects::register);}
    private static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(Payload.TYPE,Payload.CODEC,(p,c)->c.enqueueWork(()->client.accept(p)));}
    public record Payload(Kind kind,Vec3 from,Vec3 to,int color,int value) implements CustomPacketPayload {
        public static final Type<Payload> TYPE=new Type<>(WitchingGadgets.id("effects"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Payload> CODEC=StreamCodec.of((b,p)->{
            b.writeEnum(p.kind);b.writeDouble(p.from.x);b.writeDouble(p.from.y);b.writeDouble(p.from.z);b.writeDouble(p.to.x);b.writeDouble(p.to.y);b.writeDouble(p.to.z);b.writeInt(p.color);b.writeVarInt(p.value);
        },b->new Payload(b.readEnum(Kind.class),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readInt(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void near(Level level,Kind kind,Vec3 from,Vec3 to,int color,int value){if(level instanceof ServerLevel server)PacketDistributor.sendToPlayersNear(server,null,from.x,from.y,from.z,32,new Payload(kind,from,to,color,value));}
    public static void sound(Level l,Vec3 at,SoundEvent sound,float volume,float pitch){if(!l.isClientSide())l.playSound(null,at.x,at.y,at.z,sound,SoundSource.BLOCKS,volume,pitch);}
    public static float fizzPitch(Level l){return 2.6F+(l.random.nextFloat()-l.random.nextFloat())*.8F;}
    public static void furnaceReject(Level l,BlockPos lava){near(l,Kind.FURNACE_BREAK,Vec3.atCenterOf(lava),Vec3.ZERO,0,0);sound(l,Vec3.atCenterOf(lava).add(0,2,0),SoundEvents.FIRE_EXTINGUISH,.5F,fizzPitch(l));}
}
