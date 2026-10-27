package witchinggadgets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import thaumcraft.aspect.*;
import thaumcraft.research.*;

public final class GemItem extends Item {
    static final Aspect[] PRIMALS={Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY};
    public GemItem(Properties p){super(p);}
    @Override public boolean isFoil(ItemStack stack){return Data.get(stack).getInt("Potency")>0||Data.get(stack).getInt("Brittle")>0||super.isFoil(stack);}
    public static ItemStack create(Aspect aspect,int cut,int potency,int brittle){var s=Content.stack("infused_gem");var t=Data.get(s);t.putString("Aspect",aspect.name());t.putInt("Cut",cut);t.putInt("Potency",potency);t.putInt("Brittle",brittle);Data.put(s,t);return s;}
    public static Aspect aspect(ItemStack s){try{return Aspect.valueOf(Data.get(s).getString("Aspect"));}catch(IllegalArgumentException e){return null;}}
    public static boolean affinity(String gem,Aspect a){return switch(gem){case "diamond"->a==Aspect.ORDER||a==Aspect.ENTROPY;case "emerald"->a==Aspect.ORDER||a==Aspect.EARTH;case "quartz"->a==Aspect.ENTROPY||a==Aspect.FIRE;case "amber"->a==Aspect.AIR||a==Aspect.EARTH;default->false;};}
    public static boolean aversion(String gem,Aspect a){return switch(gem){case "emerald"->a==Aspect.WATER||a==Aspect.ENTROPY;case "quartz"->a==Aspect.ORDER||a==Aspect.WATER;case "amber"->a==Aspect.ENTROPY;default->false;};}
    public static int cost(Aspect a,int cut){if(cut==0)return 32;return a==Aspect.FIRE||a==Aspect.AIR||a==Aspect.EARTH?1:a==Aspect.ENTROPY?16:2;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){var s=p.getItemInHand(hand);if(!level.isClientSide())activate(s,p,false);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide());}
    public static boolean activate(ItemStack s,Player p,boolean stabilized){
        Aspect a=aspect(s);if(a==null)return false;var t=Data.get(s);int cut=t.getInt("Cut"),potency=Math.clamp(t.getInt("Potency"),0,3),brittle=t.getInt("Brittle"),spent=t.getInt("Spent"),cost=cost(a,cut);
        if(spent+cost>32)return false;
        if(!stabilized&&(brittle>=3||p.getRandom().nextInt(20)<brittle)){shatter(s,p);return false;}
        if(!effect(p,a,cut,potency))return false;
        if(!p.isCreative()){t.putInt("Spent",spent+cost);Data.put(s,t);if(cut==0&&!stabilized)shatter(s,p);}
        return true;
    }
    static void shatter(ItemStack s,Player p){s.shrink(1);var dust=Content.stack("gemstone_dust");if(!p.addItem(dust))p.drop(dust,false);}
    static boolean effect(Player p,Aspect a,int cut,int potency){
        Level l=p.level();BlockPos center=p.blockPosition().above();
        if(cut==0){
            switch(a){
                case AIR->p.addEffect(new MobEffectInstance(MobEffects.JUMP,300+20*potency,4+(potency>1?1:0)));
                case ORDER->p.heal(6+potency*6);
                case EARTH->p.addEffect(new MobEffectInstance(StatusEffects.KNOCKBACK,100+potency*100,1+(potency>0?1:0)));
                case ENTROPY->{if(p instanceof ServerPlayer sp)Warp.add(sp,3+potency*3,WarpType.TEMPORARY);}
                case WATER->{int r=2+potency;for(BlockPos pos:BlockPos.betweenClosed(center.offset(-r,-3,-r),center.offset(r,0,r))){if(!l.hasChunkAt(pos))continue;var fluid=l.getFluidState(pos);if(fluid.is(Fluids.WATER))l.setBlockAndUpdate(pos,Blocks.ICE.defaultBlockState());else if(fluid.is(Fluids.LAVA)||fluid.is(Fluids.FLOWING_LAVA))l.setBlockAndUpdate(pos,(fluid.isSource()?Blocks.OBSIDIAN:Blocks.COBBLESTONE).defaultBlockState());}}
                case FIRE->{int[] offsets=potency>2?new int[]{-8,-6,-4,4,6,8}:potency>1?new int[]{-6,-4,4,6}:potency>0?new int[]{-4,4}:new int[]{-2,2};for(int x:offsets)for(int z:offsets)light(l,center.offset(x,0,z),potency);light(l,center.above(2),potency);}
                default->{return false;}
            }return true;
        }
        var hit=p.pick(6,1,false);BlockPos target=hit.getType()==HitResult.Type.BLOCK&&hit instanceof BlockHitResult b?b.getBlockPos().relative(b.getDirection()):null;
        switch(a){
            case AIR->{p.addDeltaMovement(p.getLookAngle().scale(1+potency));p.fallDistance=0;p.hurtMarked=true;return true;}
            case FIRE->{if(target==null||!l.isEmptyBlock(target))return false;l.setBlockAndUpdate(target,Blocks.FIRE.defaultBlockState());Effects.sound(l,Vec3.atCenterOf(target),net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,1,.8F+l.random.nextFloat()*.4F);return true;}
            case WATER->{if(target==null||!l.getBlockState(target).canBeReplaced())return false;if(!l.dimensionType().ultraWarm())l.setBlockAndUpdate(target,Blocks.WATER.defaultBlockState());else{Effects.sound(l,Vec3.atCenterOf(target),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,1,.8F+l.random.nextFloat()*.4F);if(l instanceof net.minecraft.server.level.ServerLevel sl)sl.sendParticles(ParticleTypes.LARGE_SMOKE,target.getX()+.5,target.getY()+.5,target.getZ()+.5,8,.5,.5,.5,0);}return true;}
            case EARTH->{if(target==null||!(hit instanceof BlockHitResult block))return false;for(var pos:GemMechanics.ores(l,block.getBlockPos()))if(p instanceof ServerPlayer server)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server,new Effects.Payload(Effects.Kind.ORE_MARK,Vec3.atCenterOf(pos),Vec3.ZERO,0,1000));return true;}
            case ORDER->{BlockPos best=null;double dist=Double.MAX_VALUE;for(BlockPos pos:BlockPos.betweenClosed(center.offset(-16,-8,-16),center.offset(16,8,16))){if(l.hasChunkAt(pos)&&l.getBlockEntity(pos) instanceof thaumcraft.node.NodeBlockEntity&&pos.distSqr(center)<dist){best=pos.immutable();dist=pos.distSqr(center);}}if(best==null)return false;int color=0xFFFFFF;if(l.getBlockEntity(best) instanceof thaumcraft.node.NodeView n){for(Aspect aa:n.getAspects().getAspects())color=((color&0xFEFEFE)>>1)+((aa.getColor()&0xFEFEFE)>>1);}Effects.near(l,Effects.Kind.NODE_WISP,p.getEyePosition().add(p.getLookAngle().scale(.35)).add(0,-.25,0),Vec3.atCenterOf(best),color,0);var d=Vec3.atCenterOf(best).subtract(p.getEyePosition());p.setYRot((float)(Math.toDegrees(Math.atan2(d.z,d.x))-90));p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));if(p instanceof ServerPlayer sp)sp.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());return true;}
            case ENTROPY->{if(!(hit instanceof BlockHitResult b)||!(l.getBlockEntity(b.getBlockPos()) instanceof thaumcraft.device.LinkedMirrorBlockEntity mirror)||!mirror.isLinkValid(true)||!(p instanceof ServerPlayer sp))return false;var link=mirror.getLink();var dest=mirror.targetLevel();if(link==null||dest==null)return false;BlockPos pos=link.pos().relative(mirror.target(true).getFacing());sp.teleportTo(dest,pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),sp.getYRot(),sp.getXRot());return true;}
            default->{return false;}
        }
    }
    static void light(Level l,BlockPos p,int potency){if(l.isEmptyBlock(p)&&l.getMaxLocalRawBrightness(p)<10){l.setBlockAndUpdate(p,Content.block("glimmer_of_light").defaultBlockState());if(l.getBlockEntity(p) instanceof MachineEntity m)m.steamTicks=3600+potency*800;}}
    @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(l.isClientSide()||!(e instanceof Player p))return;var t=Data.get(s);if(l.random.nextInt(1000)<t.getInt("Brittle")){shatter(s,p);return;}if(selected&&t.getInt("Cut")==1&&t.getInt("Spent")>0&&e.tickCount%4==0&&aspect(s)!=null&&l.random.nextInt(20)>=t.getInt("Brittle"))GemMechanics.recharge(s,p,aspect(s));}
    @Override public boolean isBarVisible(ItemStack s){return Data.get(s).getInt("Spent")>0;}
    @Override public int getBarWidth(ItemStack s){return Math.max(0,13-Data.get(s).getInt("Spent")*13/32);}
    @Override public int getBarColor(ItemStack s){return aspect(s)==null?0xFFFFFF:aspect(s).getColor();}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){text.add(Component.translatable("wg.tooltip.gem",aspect(s)==null?Component.translatable("item.WG_InfusedGem.innert.name"):Component.translatable("tc.aspect."+aspect(s).getTag()),Component.translatable("wg.desc.gemcut."+(Data.get(s).getInt("Cut")==0?"POINT":"OVAL")),32-Data.get(s).getInt("Spent")));}
}
