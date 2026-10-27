package witchinggadgets;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import thaumcraft.aspect.*;
public final class PortData {
    public static JsonArray read(String file){
        try(var in=PortData.class.getResourceAsStream("/assets/witchinggadgets/port/"+file+".json")){
            if(in==null)throw new IllegalStateException("Missing port data "+file);
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray();
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    public static String str(JsonObject o,String k){return o.has(k)?o.get(k).getAsString():"";}
    public static int num(JsonObject o,String k){return o.has(k)?o.get(k).getAsInt():0;}
    public static boolean flag(JsonObject o,String k){return o.has(k)&&o.get(k).getAsBoolean();}
    public static List<String> strings(JsonObject o,String k){if(!o.has(k))return List.of();List<String> r=new ArrayList<>();o.getAsJsonArray(k).forEach(v->r.add(v.getAsString()));return r;}
    public static AspectList aspects(JsonObject o){var r=new AspectList();if(o!=null)o.entrySet().forEach(e->r.add(Aspect.valueOf(e.getKey()),e.getValue().getAsInt()));return r;}
}
