package witchinggadgets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record AbilityPayload(int action) implements CustomPacketPayload {
    public static final Type<AbilityPayload> TYPE=new Type<>(WitchingGadgets.id("ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AbilityPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AbilityPayload::action,AbilityPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToServer(TYPE,CODEC,AbilityPayload::handle);}
    private static void handle(AbilityPayload message,IPayloadContext context){context.enqueueWork(()->{
        var p=context.player();long tick=p.level().getGameTime();if(p.getPersistentData().contains("WGLastAbility")&&tick-p.getPersistentData().getLong("WGLastAbility")<2)return;p.getPersistentData().putLong("WGLastAbility",tick);
        if(message.action==3&&p.getMainHandItem().is(Content.item("primordial_gauntlet"))){BagItem.open(p,p.getMainHandItem(),9);return;}
        if(message.action>=10&&message.action<15&&p.getMainHandItem().is(Content.item("primordial_gauntlet"))){Data.integer(p.getMainHandItem(),"Affinity",message.action-10);return;}
        if(message.action==2){if(p.onGround()||p.getAbilities().flying||p.getPersistentData().getBoolean("WGDoubleJump")||!Gear.has(p,"double_jump_shoulders"))return;p.getPersistentData().putBoolean("WGDoubleJump",true);EquipmentRules.extraJump(p);return;}
        if(message.action==1){GearAbilities.quick(p);return;}
        if(message.action==0){var entries=GearAbilities.entries(p);if(entries.size()==1)GearAbilities.activate(p,entries.getFirst().action());return;}
        if(message.action>=100)GearAbilities.activate(p,message.action);
    });}
}
