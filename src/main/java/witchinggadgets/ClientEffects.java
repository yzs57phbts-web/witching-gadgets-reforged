package witchinggadgets;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.*;
import thaumcraft.client.platform.*;
import thaumcraft.fx.Fx;

public final class ClientEffects {
    static final Map<BlockPos,Long> ores=new LinkedHashMap<>();
    static final Map<Integer,Long> saunaPlayers=new HashMap<>();
    static Object lastLevel;
    static int oreIndex;
    static final EnumMap<Effects.Kind,Integer> received=new EnumMap<>(Effects.Kind.class);
    static int beams,ambientWisps,sweat;
    static void init(){Effects.client=ClientEffects::receive;Effects.clientMachine=ClientEffects::machine;}
    static void receive(Effects.Payload p){
        var mc=Minecraft.getInstance();var l=mc.level;if(l==null)return;
        received.merge(p.kind(),1,Integer::sum);var v=p.from();
        switch(p.kind()){
            case CAMERA_RUNES->Fx.get().blockRunes(l,v.x,v.y,v.z,.3F+l.random.nextFloat()*.7F,0,.3F+l.random.nextFloat()*.7F,p.value(),.03F);
            case REFORMER_VENT->{for(int j=0;j<4;j++)for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)Fx.get().ventParticles(l,v.x+.6875*d.getStepX(),v.y+.375,v.z+.6875*d.getStepZ(),.1875,.25,.1875,p.color(),2);}
            case SAUNA_PLAYER->saunaPlayers.put(p.value(),l.getGameTime()+30);
            case ORE_MARK->{if(ores.size()<4096)ores.put(BlockPos.containing(v),l.getGameTime()+1000);}
            case NODE_WISP->{wisp(v,p.to(),.5F,p.color(),false);if(l.random.nextInt(3)==0)l.playLocalSound(v.x,v.y,v.z,net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.02F,.5F*((l.random.nextFloat()-l.random.nextFloat())*.6F+2),false);}
            case FURNACE_BREAK->{for(int i=0;i<3;i++)l.addParticle(ParticleTypes.LAVA,v.x,v.y+.4,v.z,0,0,0);}
            case ESSENTIA_SOURCE->{var from=BlockPos.containing(v);var to=BlockPos.containing(p.to());Fx.get().essentiaSource(l,from,from.getX()-to.getX(),from.getY()-to.getY(),from.getZ()-to.getZ(),p.color());}
            case GEM_RECHARGE->{var player=l.getEntity(p.value());if(player!=null){var hand=player==mc.player?hand():player.getEyePosition().add(player.getLookAngle().scale(.35)).add(0,-.25,0);power(v,hand,p.color());}}
        }
    }
    static void wisp(Vec3 from,Vec3 to,float scale,int color,boolean shrink){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var wisp=new WispFx(mc.level.random,from.x,from.y,from.z,scale,3,null).moveTo(to.x,to.y,to.z).shrink(shrink);
        wisp.color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F);wisp.gravity(0);WorldFxRenderer.add(wisp);ambientWisps++;
    }
    static void machine(MachineEntity m){
        var l=m.getLevel();var at=Vec3.atCenterOf(m.getBlockPos());
        if(m.kind.equals("sauna_stove")&&m.steamTicks>0&&l.random.nextInt(7)==0){wisp(at.add(0,.375,0),at.add((l.random.nextFloat()-.5)*.4,0,(l.random.nextFloat()-.5)*.4),.5F,0xEE3333,false);}
        if(m.kind.equals("glimmer_of_light")&&l.random.nextInt(3)==0){wisp(at.add((l.random.nextFloat()-.5)*.5,(l.random.nextFloat()-.5)*.5,(l.random.nextFloat()-.5)*.5),at,.1F+l.random.nextFloat()*.1F,0xDDAA66,false);}
    }
    static Vec3 hand(){var p=Minecraft.getInstance().player;return p.getEyePosition().add(p.getLookAngle().scale(.35)).add(-.15,-.25,0);}
    static void tick(){
        var mc=Minecraft.getInstance();var l=mc.level;
        if(l!=lastLevel){ores.clear();saunaPlayers.clear();oreIndex=0;lastLevel=l;}
        if(l==null||mc.player==null||mc.isPaused())return;long time=l.getGameTime();
        ores.entrySet().removeIf(e->e.getValue()<=time||!l.getBlockState(e.getKey()).isSolid());
        if(!ores.isEmpty()&&time%30==0){var keys=new ArrayList<>(ores.keySet());var pos=keys.get(Math.floorMod(oreIndex++,keys.size()));wisp(hand(),Vec3.atCenterOf(pos).add(0,.25,0),.05F,0x44DD44,true);}
        saunaPlayers.entrySet().removeIf(e->e.getValue()<=time||l.getEntity(e.getKey())==null);
        for(int id:saunaPlayers.keySet())if(l.random.nextInt(3)==0){var p=l.getEntity(id);var drop=mc.particleEngine.createParticle(ParticleTypes.FALLING_WATER,p.getX()-.2+l.random.nextFloat()/2,p.getY()+.5+l.random.nextFloat(),p.getZ()-.2+l.random.nextFloat()/2,0,0,0);if(drop!=null){drop.setColor(.2F,.2F,.3F+l.random.nextFloat()*.5F);drop.setLifetime(20);sweat++;}}
        var held=mc.player.getMainHandItem();
        if(held.is(Content.item("guiding_thread")))guidance(held);
        if(time%4==0&&Events.enchant(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),"unveiling",mc.player)>0)for(var p:l.players())if(p!=mc.player&&p.isInvisible())Fx.get().sparkle(l,(float)p.getX()+l.random.nextFloat()-.5F,(float)p.getY()+.5F+l.random.nextFloat(),(float)p.getZ()+l.random.nextFloat()-.5F,1,0,0);
    }
    static void guidance(net.minecraft.world.item.ItemStack held){
        var mc=Minecraft.getInstance();var t=Data.get(held);if(!mc.level.dimension().location().toString().equals(t.getString("PathDimension")))return;
        var path=t.getList("Waypoints",net.minecraft.nbt.Tag.TAG_COMPOUND);Vec3 previous=null;
        double radius=mc.options.getEffectiveRenderDistance()*16.0+16;
        for(int i=0;i<path.size();i++){var n=path.getCompound(i);Vec3 point=new Vec3(n.getDouble("X"),n.getDouble("Y")+1,n.getDouble("Z"));if(previous!=null&&nearSegment(previous,point,mc.player.position(),radius))power(previous,point,0xFFFF80);previous=point;}
        if(path.size()>1&&t.getBoolean("Guiding")&&previous!=null&&nearSegment(previous,hand(),mc.player.position(),radius))power(previous,hand(),0xFFFF80);
    }
    static boolean nearSegment(Vec3 from,Vec3 to,Vec3 viewer,double radius){var delta=to.subtract(from);double length=delta.lengthSqr();double at=length<1E-12?0:net.minecraft.util.Mth.clamp(viewer.subtract(from).dot(delta)/length,0,1);return viewer.distanceToSqr(from.add(delta.scale(at)))<=radius*radius;}
    static void power(Vec3 from,Vec3 to,int color){WorldFxRenderer.add(new PowerLine(from,to,color));beams++;}
    static final class PowerLine extends WorldFx {
        final Vec3 target;final int color;
        PowerLine(Vec3 from,Vec3 to,int color){super(from.x,from.y,from.z,2);target=to;this.color=color;}
        @Override public ResourceLocation texture(){return ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/beam1.png");}
        @Override public int blendMode(){return 1;}
        @Override public void render(thaumcraft.client.geom.QuadSink sink,float partial,Vec3 camera,float yaw,float pitch){var a=new Vec3(x,y,z).subtract(camera);var b=target.subtract(camera);var direction=b.subtract(a);double length=Math.max(1E-8,direction.lengthSqr());thaumcraft.client.geom.PowerBeamGeometry.render((float)a.x,(float)a.y,(float)a.z,(float)b.x,(float)b.y,(float)b.z,Minecraft.getInstance().level.getGameTime()+partial,.5F,color,(vx,vy,vz,u,v,argb,light)->{double t=((vx-a.x)*direction.x+(vy-a.y)*direction.y+(vz-a.z)*direction.z)/length;double cx=a.x+t*direction.x,cy=a.y+t*direction.y,cz=a.z+t*direction.z;sink.vertex((float)(cx+(vx-cx)*.1),(float)(cy+(vy-cy)*.1),(float)(cz+(vz-cz)*.1),u,v,argb,light);});}
    }
    static boolean inSauna(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&saunaPlayers.getOrDefault(mc.player.getId(),0L)>mc.level.getGameTime();}
}
