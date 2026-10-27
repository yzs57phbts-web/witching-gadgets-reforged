package witchinggadgets;
import java.util.*;
/** Availability comes from the published 1.1.10 JAR, not from the presence of a source file. */
public final class ReleaseContent {
    private static final Map<String,String> STATUS=new LinkedHashMap<>();
    static {for(var row:PortData.read("release_content")){var o=row.getAsJsonObject();STATUS.put(PortData.str(o,"id"),PortData.str(o,"status"));}}
    public static String status(String name){return STATUS.getOrDefault(name,"unknown");}
    public static boolean creative(String name){return (status(name).startsWith("release_gameplay")||status(name).startsWith("release_conditional_")||status(name).equals("port_bridge"))&&!status(name).equals("release_conditional_tconstruct")&&Compat.visible(name);}
}
