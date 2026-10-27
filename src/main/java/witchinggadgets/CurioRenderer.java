package witchinggadgets;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class CurioRenderer implements ICurioRenderer {
    @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,PoseStack pose,RenderLayerParent<T,M> parent,MultiBufferSource buffers,int light,float limbSwing,float limbAmount,float partial,float age,float headYaw,float headPitch){
        if(EquipmentRules.hidden(stack,context.entity(),net.minecraft.client.Minecraft.getInstance().player)||SpectralRendering.hidden(context.entity(),net.minecraft.client.Minecraft.getInstance().player))return;
        if(!(stack.getItem() instanceof Gear gear)||!(parent.getModel() instanceof HumanoidModel<?> model))return;String kind=gear.kind;
        if(kind.endsWith("shoulders")||kind.endsWith("vambraces")){arms(kind,model,pose,buffers,light,Gear.spectral(context.entity())?SpectralRendering.tint(context.entity(),net.minecraft.client.Minecraft.getInstance().player):-1);return;}
        boolean cape=kind.contains("cloak")||kind.contains("mantle"),kama=kind.contains("kama");if(!cape&&!kama)return;
        pose.pushPose();model.body.translateAndRotate(pose);pose.translate(0,kama?.72:0,.125);if(kama)pose.scale(1,.5F,1);
        float glide=glide(stack,context.entity(),partial);pose.translate(0,-.06F*glide,0);float rest=6+Math.min(35,Math.abs(model.leftLeg.xRot)*22);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(net.minecraft.util.Mth.lerp(glide,rest,82)+(float)Math.sin(age*.09F)*2));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
        String texture=kind.contains("raven")?"raven":kind.contains("wolven")?"wolven":kind.startsWith("spectral_")?"spectral":kind.contains("voluminous")?"voluminous":"plain";
        int alpha=context.entity().isInvisible()?30:255;int color=alpha<<24|Client.clothColor(stack)&0xFFFFFF;if(Gear.spectral(context.entity()))color=SpectralRendering.tint(context.entity(),net.minecraft.client.Minecraft.getInstance().player);
        CAPE.render(pose,buffers.getBuffer(RenderType.entityTranslucent(WitchingGadgets.id("textures/capes/"+texture+".png"))),light,OverlayTexture.NO_OVERLAY,color);pose.popPose();
    }
    static final net.minecraft.client.model.geom.ModelPart CAPE=cape();
    static final java.util.Map<LivingEntity,float[]> GLIDE=new java.util.WeakHashMap<>();
    static float glide(ItemStack stack,LivingEntity entity,float partial){var v=GLIDE.computeIfAbsent(entity,e->new float[]{-1,0,0});if(v[0]!=entity.tickCount){v[0]=entity.tickCount;v[1]=v[2];boolean target=stack.getItem() instanceof Gear g&&Compat.raven(g.kind)&&!Data.get(stack).getBoolean("NoGlide")&&!entity.onGround()&&!entity.isInWater()&&(entity.getDeltaMovement().y<-.01||entity.isFallFlying()||entity instanceof net.minecraft.world.entity.player.Player p&&p.getAbilities().flying);v[2]=net.minecraft.util.Mth.approach(v[2],target?1:0,.1F);}return net.minecraft.util.Mth.lerp(partial,v[1],v[2]);}
    static net.minecraft.client.model.geom.ModelPart cape(){var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();mesh.getRoot().addOrReplaceChild("cape",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,0).addBox(-5,0,-1,10,16,1),net.minecraft.client.model.geom.PartPose.ZERO);return net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,32).bakeRoot();}
    static final java.util.Map<String,net.minecraft.client.model.geom.ModelPart> ARMS=new java.util.HashMap<>();
    static void arms(String kind,HumanoidModel<?> model,PoseStack pose,MultiBufferSource buffers,int light,int tint){
        boolean shoulders=kind.endsWith("shoulders");int u=kind.equals("knockback_shoulders")||kind.equals("wolven_vambraces")?40:24,v=kind.endsWith("vambraces")?24:16;
        var root=ARMS.computeIfAbsent(kind,k->{var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();for(boolean left:new boolean[]{false,true})mesh.getRoot().addOrReplaceChild(left?"left":"right",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(u,v).mirror(left).addBox(left?-1:-3,shoulders?-2:5,-2,4,4,4,new net.minecraft.client.model.geom.builders.CubeDeformation(shoulders?1.125F:.125F)),net.minecraft.client.model.geom.PartPose.ZERO);return net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,32).bakeRoot();});
        var b=buffers.getBuffer(RenderType.entityTranslucent(WitchingGadgets.id("textures/models/magicalbaubles.png")));
        for(boolean left:new boolean[]{false,true}){pose.pushPose();(left?model.leftArm:model.rightArm).translateAndRotate(pose);root.getChild(left?"left":"right").render(pose,b,light,OverlayTexture.NO_OVERLAY,tint);pose.popPose();}
    }
    private static void vertex(VertexConsumer v,PoseStack p,float x,float y,float z,float u,float w,int color,int light){v.addVertex(p.last(),x,y,z).setColor(color).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p.last(),0,0,1);}
}
