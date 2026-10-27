package witchinggadgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** OBJ faces are converted to independent quads; Blender reference groups are never drawn implicitly. */
public final class WgObjModel {
    private static final Map<ResourceLocation,WgObjModel> CACHE=new HashMap<>();
    private final Map<String,List<float[][]>> groups=new LinkedHashMap<>();
    public static WgObjModel get(ResourceLocation path){return CACHE.computeIfAbsent(path,WgObjModel::new);}
    public static void clear(){CACHE.clear();}
    private WgObjModel(ResourceLocation path){
        List<float[]> positions=new ArrayList<>(),uvs=new ArrayList<>(),normals=new ArrayList<>();String group="";
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(path)){
            String line;while((line=reader.readLine())!=null){String[] words=line.trim().split("\\s+");if(words.length==0)continue;
                switch(words[0]){
                    case "v"->positions.add(numbers(words,3));case "vt"->uvs.add(numbers(words,2));case "vn"->normals.add(numbers(words,3));
                    case "g","o"->{group=words.length>1?words[1]:"";groups.computeIfAbsent(group,k->new ArrayList<>());}
                    case "f"->{
                        List<float[]> face=new ArrayList<>();for(int i=1;i<words.length&&!words[i].startsWith("#");i++){
                            String[] ids=words[i].split("/",-1);float[] p=positions.get(index(ids[0],positions.size())),uv=ids.length>1&&!ids[1].isEmpty()?uvs.get(index(ids[1],uvs.size())):new float[2],n=ids.length>2&&!ids[2].isEmpty()?normals.get(index(ids[2],normals.size())):new float[]{0,1,0};
                            face.add(new float[]{p[0],p[1],p[2],uv[0],1-uv[1],n[0],n[1],n[2]});
                        }
                        if(face.size()<3)throw new IOException("Invalid face");var output=groups.computeIfAbsent(group,k->new ArrayList<>());
                        if(face.size()==4)output.add(face.toArray(float[][]::new));else for(int i=1;i<face.size()-1;i++)output.add(new float[][]{face.get(0),face.get(i),face.get(i+1),face.get(i+1)});
                    }
                    default->{}
                }
            }
        }catch(IOException|RuntimeException e){throw new IllegalStateException("Invalid Witching Gadgets OBJ: "+path,e);}
    }
    static int index(String text,int size){int i=Integer.parseInt(text);return i<0?size+i:i-1;}
    static float[] numbers(String[] words,int size){float[] out=new float[size];for(int i=0;i<size;i++){out[i]=Float.parseFloat(words[i+1]);if(!Float.isFinite(out[i]))throw new IllegalArgumentException("Non-finite OBJ coordinate");}return out;}
    public int quadCount(String name){return require(name).size();}
    public Set<String> groups(){return Collections.unmodifiableSet(groups.keySet());}
    List<float[][]> require(String name){var faces=groups.get(name);if(faces==null||faces.isEmpty())throw new IllegalArgumentException("Missing OBJ group: "+name);return faces;}
    public void renderPart(String name,PoseStack.Pose pose,VertexConsumer buffer,int argb,int light){
        for(float[][] face:require(name))for(float[] v:face)buffer.addVertex(pose,v[0],v[1],v[2]).setColor(argb).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,v[5],v[6],v[7]);
    }
}
