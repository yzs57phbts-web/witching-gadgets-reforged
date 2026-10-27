package witchinggadgets;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import thaumcraft.aspect.*;
import thaumcraft.research.*;
public final class Tools {
    public static void photo(ServerPlayer p){
        var l=p.serverLevel();var from=p.getEyePosition();var look=p.getLookAngle().scale(10);var end=from.add(look);var block=p.pick(10,1,true);
        var entity=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(l,p,from,end,p.getBoundingBox().expandTowards(look).inflate(1),e->e!=p&&e.isAlive()&&e.isPickable());
        Vec3 runePos=block.getLocation();int runeDuration=15;ScanManager.Target target=null;
        if(entity!=null&&entity.getLocation().distanceToSqr(from)<block.getLocation().distanceToSqr(from)){var e=entity.getEntity();runePos=e.position().add(-.5,e.getEyeHeight()/2,-.5);runeDuration=(int)(e.getBbHeight()*15);target=ScanManager.resolveEntity(e);}
        else if(block instanceof BlockHitResult b&&block.getType()==HitResult.Type.BLOCK){runePos=Vec3.atLowerCornerOf(b.getBlockPos()).add(0,.25,0);target=l.getBlockEntity(b.getBlockPos()) instanceof thaumcraft.node.NodeView?ScanManager.resolveNode(l,b.getBlockPos()):ScanManager.resolveBlock(l,b.getBlockPos());}
        if(target!=null)Effects.near(l,Effects.Kind.CAMERA_RUNES,runePos,Vec3.ZERO,0,runeDuration);
        if(target==null||!ScanManager.validScan(target.aspects(),p))return;int slot=-1;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(Content.tc("mirrored_glass"))){slot=i;break;}if(slot<0)return;
        var plate=Content.stack("iconographic_plate");var t=Data.get(plate);t.putString("PhotoKind",target.kind().name());t.putString("PhotoKey",target.key());CompoundTag aspects=new CompoundTag();if(target.aspects()!=null)for(var a:target.aspects().getAspects())aspects.putInt(a.name(),target.aspects().getAmount(a));t.put("PhotoAspects",aspects);if(!target.clueStack().isEmpty())t.putString("PhotoClue",BuiltInRegistries.ITEM.getKey(target.clueStack().getItem()).toString());if(target.clueEntity()!=null)t.putString("PhotoEntity",target.clueEntity().toString());Data.put(plate,t);
        p.getInventory().getItem(slot).shrink(1);if(!p.addItem(plate))p.drop(plate,false);p.getCooldowns().addCooldown(Content.item("scan_camera"),20);Effects.sound(l,p.position(),thaumcraft.registry.ModSounds.CAMERATICKS.get(),.3F,1.9F+l.random.nextFloat()*.2F);
    }
    public static boolean developScan(ServerPlayer p,ItemStack s){
        var t=Data.get(s);if(!t.contains("PhotoKey"))return false;var list=new AspectList();var saved=t.getCompound("PhotoAspects");for(String key:saved.getAllKeys()){try{list.add(Aspect.valueOf(key),saved.getInt(key));}catch(IllegalArgumentException ignored){}}
        var clue=t.contains("PhotoClue")?new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(t.getString("PhotoClue")))):ItemStack.EMPTY;
        var entity=t.contains("PhotoEntity")?net.minecraft.resources.ResourceLocation.parse(t.getString("PhotoEntity")):null;ScanKind kind;try{kind=ScanKind.valueOf(t.getString("PhotoKind"));}catch(IllegalArgumentException e){return false;}
        var target=new ScanManager.Target(kind,t.getString("PhotoKey"),list,clue,entity);if(ScanManager.hasBeenScanned(p,target))return false;if(ScanManager.completeScan(p,target)){s.shrink(1);return true;}return false;
    }
    public static void toggleGuidance(Player p,ItemStack s){var t=Data.get(s);if(p.isShiftKeyDown()){t.remove("Waypoints");t.putBoolean("Guiding",false);}else{t.putBoolean("Guiding",!t.getBoolean("Guiding"));var path=t.getList("Waypoints",Tag.TAG_COMPOUND);var pos=new CompoundTag();pos.putDouble("X",p.getX());pos.putDouble("Y",p.getY());pos.putDouble("Z",p.getZ());pos.putInt("Heading",net.minecraft.util.Mth.floor(p.getYRot()*8/360+.5)&7);path.add(pos);t.put("Waypoints",path);t.putString("PathDimension",p.level().dimension().location().toString());}Data.put(s,t);}
    public static void tickGuidance(ServerPlayer p,ItemStack s,boolean selected){
        var t=Data.get(s);var path=t.getList("Waypoints",Tag.TAG_COMPOUND);if(!p.level().dimension().location().toString().equals(t.getString("PathDimension"))||path.isEmpty())return;
        var last=path.getCompound(path.size()-1);int heading=net.minecraft.util.Mth.floor(p.getYRot()*8/360+.5)&7;double distance=Math.pow(last.getDouble("X")-p.getX(),2)+Math.pow(last.getDouble("Y")-p.getY(),2)+Math.pow(last.getDouble("Z")-p.getZ(),2);
        if(t.getBoolean("Guiding")&&heading!=last.getInt("Heading")&&distance>1){var pos=new CompoundTag();pos.putDouble("X",p.getX());pos.putDouble("Y",p.getY());pos.putDouble("Z",p.getZ());pos.putInt("Heading",heading);path.add(pos);t.put("Waypoints",path);Data.put(s,t);}

    }
    public static boolean abacus(Player p,net.minecraft.world.level.Level l,BlockPos pos){
        if(l.getBlockEntity(pos) instanceof thaumcraft.node.NodeView node){if(!l.isClientSide())p.displayClientMessage(AbacusReport.node(node,pos),false);return true;}
        if(l.getBlockEntity(pos) instanceof thaumcraft.infusion.InfusionMatrixBlockEntity){if(!l.isClientSide())InfusionAnalysis.display(p,pos);return true;}return false;
    }
}
