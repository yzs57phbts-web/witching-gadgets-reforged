package witchinggadgets;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;

final class CapsuleRendering {
    static boolean[][] mask;
    static void render(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();pose.pushPose();pose.translate(-.5,0,-.5);
        var fluid=Capsule.handler(stack.copyWithCount(1)).getFluidInTank(0);
        if(!fluid.isEmpty()){
            var extension=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
            var sprite=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(extension.getStillTexture(fluid));int tint=extension.getTintColor(fluid);
            if(mask==null)try(var in=mc.getResourceManager().open(WitchingGadgets.id("textures/items/crystalcapsule_mask.png"));var image=NativeImage.read(in)){
                mask=new boolean[image.getWidth()][image.getHeight()];for(int x=0;x<mask.length;x++)for(int y=0;y<mask[x].length;y++)mask[x][y]=(image.getPixelRGBA(x,y)>>>24)>0;
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            var buffer=buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
            for(int x=0;x<mask.length;x++)for(int y=0;y<mask[x].length;y++)if(mask[x][y]){
                float left=x/(float)mask.length,right=(x+1F)/mask.length,top=1-y/(float)mask[x].length,bottom=1-(y+1F)/mask[x].length;
                for(int side:new int[]{-1,1})for(int i=0;i<4;i++){
                    float px=i==0||i==3?left:right,py=i<2?bottom:top;
                    buffer.addVertex(pose.last(),px,py,.5F+side*.022F).setColor(tint).setUv(sprite.getU(px),sprite.getV(1-py)).setOverlay(overlay).setLight(light).setNormal(pose.last(),0,0,side);
                }
            }
        }
        var model=mc.getModelManager().getModel(ModelResourceLocation.standalone(WitchingGadgets.id("item/capsule_shell")));
        mc.getItemRenderer().renderModelLists(model,stack,light,overlay,pose,buffers.getBuffer(Sheets.translucentItemSheet()));pose.popPose();
    }
}
