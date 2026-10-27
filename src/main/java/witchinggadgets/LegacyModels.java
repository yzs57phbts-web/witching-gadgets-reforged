package witchinggadgets;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

public final class LegacyModels {
    static JsonObject machines;
    static JsonObject machine(String kind){if(machines==null)try(var in=LegacyModels.class.getResourceAsStream("/assets/witchinggadgets/port/legacy_models.json")){machines=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new UncheckedIOException(e);}return machines.getAsJsonObject(kind);}
    static float f(JsonArray a,int i){return a.get(i).getAsFloat();}
    static PartDefinition add(PartDefinition parent,String name,JsonObject o){var box=o.getAsJsonArray("box");var uv=o.getAsJsonArray("uv");var pos=o.getAsJsonArray("offset");var rot=o.getAsJsonArray("rotation");var cubes=CubeListBuilder.create().texOffs(uv.get(0).getAsInt(),uv.get(1).getAsInt());if(PortData.flag(o,"mirror"))cubes.mirror();float w=f(box,3),h=f(box,4),d=f(box,5);if(w==0||h==0||d==0)cubes.addBox(f(box,0),f(box,1),f(box,2),w,h,d,java.util.Set.of(h==0?net.minecraft.core.Direction.UP:w==0?net.minecraft.core.Direction.EAST:net.minecraft.core.Direction.NORTH));else cubes.addBox(f(box,0),f(box,1),f(box,2),w,h,d,new CubeDeformation(o.has("deform")?o.get("deform").getAsFloat():0));return parent.addOrReplaceChild(name,cubes,PartPose.offsetAndRotation(f(pos,0),f(pos,1),f(pos,2),f(rot,0),f(rot,1),f(rot,2)));}
    static final Map<String,ModelPart> MACHINE_CACHE=new HashMap<>();
    static ModelPart machineModel(String kind){return MACHINE_CACHE.computeIfAbsent(kind,k->{var model=machine(kind);var mesh=new MeshDefinition();int i=0;for(var o:model.getAsJsonArray("cubes"))add(mesh.getRoot(),"part"+(i++),o.getAsJsonObject());var size=model.getAsJsonArray("cubes").get(0).getAsJsonObject().getAsJsonArray("size");return LayerDefinition.create(mesh,size.get(0).getAsInt(),size.get(1).getAsInt()).bakeRoot();});}
    public static final class Armor extends HumanoidModel<LivingEntity>{
        final ModelPart root;
        final String kind;
        final Map<String,ModelPart> parts=new HashMap<>();
        Armor(String kind){this(bakeArmor(kind),kind);}
        Armor(String kind,int mask,boolean boots){this(bakeArmor(kind,mask,boots),kind);}
        Armor(ModelPart root,String kind){super(root);this.root=root;this.kind=kind;for(var element:PortData.read(kind+"_armor")){var o=element.getAsJsonObject();String name=PortData.str(o,"name"),parent=PortData.str(o,"parent");ModelPart pp=parts.get(parent);if(pp==null&&root.hasChild(parent))pp=root.getChild(parent);if(pp!=null&&pp.hasChild(name))parts.put(name,pp.getChild(name));}}
        void prepare(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.EquipmentSlot slot){
            for(var p:parts.values())p.visible=true;setAllVisible(false);
            switch(slot){case HEAD->head.visible=true;case CHEST->{body.visible=leftArm.visible=rightArm.visible=true;if(kind.equals("advanced_robes"))head.visible=true;}case LEGS->{body.visible=leftLeg.visible=rightLeg.visible=true;}case FEET->leftLeg.visible=rightLeg.visible=true;default->{}}
            if(kind.equals("primordial")){
                if(parts.containsKey("bd_belt"))parts.get("bd_belt").visible=slot==net.minecraft.world.entity.EquipmentSlot.LEGS;
                if(parts.containsKey("bd_chainmail"))parts.get("bd_chainmail").visible=slot==net.minecraft.world.entity.EquipmentSlot.CHEST;
                for(String n:List.of("lb_base","rb_base"))if(parts.containsKey(n))parts.get(n).visible=slot==net.minecraft.world.entity.EquipmentSlot.FEET;
                for(String n:List.of("ll_chainmail","rl_chainmail"))if(parts.containsKey(n))parts.get(n).visible=slot==net.minecraft.world.entity.EquipmentSlot.LEGS;
            }
            if(parts.containsKey("face_mask"))parts.get("face_mask").visible=Data.get(stack).contains("mask");if(parts.containsKey("face_goggles"))parts.get("face_goggles").visible=Data.get(stack).getInt("goggles")>0;
        }
    }
    static ModelPart bakeArmor(String kind){
        return bakeArmor(kind,0,false);
    }
    static ModelPart bakeArmor(String kind,int mask,boolean boots){
        var mesh=new MeshDefinition();var root=mesh.getRoot();Map<String,PartDefinition> built=new HashMap<>();
        built.put("head",root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO));built.put("hat",root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO));built.put("body",root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO));
        built.put("right_arm",root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0)));built.put("left_arm",root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0)));built.put("right_leg",root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9F,12,0)));built.put("left_leg",root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9F,12,0)));
        List<JsonObject> remaining=new ArrayList<>();PortData.read(kind+"_armor").forEach(element->{var o=element.getAsJsonObject().deepCopy();if(PortData.str(o,"name").equals("face_mask")){var uv=new JsonArray();uv.add(90);uv.add(8+Math.clamp(mask,0,2)*7);o.add("uv",uv);}if(boots&&o.has("deform")&&Math.abs(o.get("deform").getAsFloat()-.0625F)<.001)o.addProperty("deform",.3125F);remaining.add(o);});
        for(int pass=0;pass<10&&!remaining.isEmpty();pass++)for(var iterator=remaining.iterator();iterator.hasNext();){var o=iterator.next();String parent=PortData.str(o,"parent"),name=PortData.str(o,"name");if(built.containsKey(parent)){built.put(name,add(built.get(parent),name,o));iterator.remove();}}
        return LayerDefinition.create(mesh,128,64).bakeRoot();
    }
}
