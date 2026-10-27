package witchinggadgets;

import thaumcraft.aspect.AspectSync;

/** Explicit tags from WGContent.postInitThaumcraft, registered before native derivation and sync. */
public final class Aspects {
    public static void init(){AspectSync.addRegistrar(registry->{
        for(var element:PortData.read("object_aspects")){
            var o=element.getAsJsonObject();var item=Recipes.output(o.getAsJsonObject("item")).create();
            if(!item.isEmpty())registry.register(item.getItem(),PortData.aspects(o.getAsJsonObject("aspects")));
        }
    });}
}
