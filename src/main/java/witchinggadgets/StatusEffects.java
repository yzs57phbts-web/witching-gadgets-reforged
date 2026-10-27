package witchinggadgets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class StatusEffects {
    private static final DeferredRegister<MobEffect> REGISTRY=DeferredRegister.create(Registries.MOB_EFFECT,WitchingGadgets.ID);
    public static final DeferredHolder<MobEffect,MobEffect> KNOCKBACK=REGISTRY.register("knockback_resistance",()->new Effect(0,7237230).addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE,WitchingGadgets.id("knockback_resistance"),.34,AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect,MobEffect> DISSOLVE=REGISTRY.register("dissolve",()->new Effect(1,4524869));
    public static final DeferredHolder<MobEffect,MobEffect> CINDER=REGISTRY.register("cinder_coat",()->new Effect(2,9387807));
    public static void init(IEventBus bus){REGISTRY.register(bus);}
    static int icon(MobEffectInstance e){return e.is(KNOCKBACK)?1:e.is(DISSOLVE)?2:3;}
    static final class Effect extends MobEffect {
        final int kind;
        Effect(int kind,int color){super(kind==0?MobEffectCategory.BENEFICIAL:MobEffectCategory.HARMFUL,color);this.kind=kind;}
        @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){int rate=40>>Math.clamp(amplifier,0,30);return kind==1&&(rate<=0||duration%rate==0);}
        @Override public boolean applyEffectTick(LivingEntity e,int amplifier){if(kind==1&&!e.level().isClientSide())e.hurt(e.damageSources().source(thaumcraft.damage.ModDamageTypes.DISSOLVE),1);return true;}
    }
}
