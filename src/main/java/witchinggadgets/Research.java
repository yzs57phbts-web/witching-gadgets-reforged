package witchinggadgets;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.aspect.*;
import thaumcraft.crafting.*;
import thaumcraft.research.*;

/** Explicit icons, prerequisites and page order from the released WG 1.1.10 tab. */
public final class Research {
    public static final String CATEGORY="WITCHGADG";
    private static final Map<String,Entry> ENTRIES=new LinkedHashMap<>();
    private static final Map<String,JsonObject> DEFINITIONS=new LinkedHashMap<>();
    private static final Map<String,String> ALIASES=new LinkedHashMap<>();

    public static void init(){
        ResearchCategories.registerCategory(CATEGORY,WitchingGadgets.id("textures/gui/research/WGIcon.png"),WitchingGadgets.id("textures/gui/research/WGResearchBack.png"));
        for(var element:PortData.read("research")){
            var o=element.getAsJsonObject();if(!Compat.enabled(PortData.str(o,"requires")))continue;
            String key=PortData.str(o,"key");var icon=o.getAsJsonObject("icon");
            var tags=PortData.aspects(o.getAsJsonObject("aspects"));
            if(o.has("optionalAspects"))o.getAsJsonObject("optionalAspects").entrySet().forEach(a->{var aspect=Aspect.getAspect(a.getKey());if(aspect!=null)tags.add(aspect,a.getValue().getAsInt());});
            Entry e=icon.has("texture")?new Entry(key,tags,PortData.num(o,"x"),PortData.num(o,"y"),PortData.num(o,"difficulty"),ResourceLocation.parse(PortData.str(icon,"texture"))):new Entry(key,tags,PortData.num(o,"x"),PortData.num(o,"y"),PortData.num(o,"difficulty"),Recipes.output(icon.getAsJsonObject("item")));
            flags(e,o);e.setWarp(PortData.num(o,"warp"));
            e.setParentsHidden(PortData.strings(o,"hiddenParents").toArray(String[]::new));
            e.setSiblings(PortData.strings(o,"siblings").toArray(String[]::new));
            e.setItemTriggers(PortData.strings(o,"itemTriggers").toArray(String[]::new));
            e.setAspectTriggers(PortData.strings(o,"aspectTriggers").stream().map(Aspect::valueOf).toArray(Aspect[]::new));
            ENTRIES.put(key,e);DEFINITIONS.put(key,o);e.registerResearchItem();
        }
        refreshRecipePages();
    }

    private static void flags(ResearchItem e,JsonObject o){
        for(String flag:PortData.strings(o,"flags"))switch(flag){
            case "Concealed"->e.setConcealed();case "Hidden"->e.setHidden();case "Secondary"->e.setSecondary();
            case "AutoUnlock"->e.setAutoUnlock();case "Special"->e.setSpecial();case "Round"->e.setRound();
            case "Lost"->e.setLost();case "Stub"->e.setStub();case "Virtual"->e.setVirtual();
            default->throw new IllegalArgumentException("Unknown research flag "+flag);
        }
    }

    public static void refreshRecipePages(){ENTRIES.forEach((key,e)->{
        var definition=DEFINITIONS.get(key);var parents=new ArrayList<>(PortData.strings(definition,"parents"));
        var optionalParents=PortData.strings(definition,"optionalParents").stream().filter(p->ResearchCategories.getResearch(p)!=null).toList();
        parents.addAll(key.equals("METALLURGICPERFECTION_CLUSTERS")?2:parents.size(),optionalParents);
        e.setParents(parents.toArray(String[]::new));
        List<ResearchPage> pages=new ArrayList<>();
        for(var element:definition.getAsJsonArray("pages")){
            var page=element.getAsJsonObject();
            if(page.has("text")){pages.add(ResearchPage.text(PortData.str(page,"text")));continue;}
            if(page.has("group")){
                String group=PortData.str(page,"group");var recipes=Recipes.BY_RESEARCH.getOrDefault(key,List.of());
                if(group.equals("CLOAKKAMA"))pages.add(ResearchPage.arcane(recipes.stream().filter(r->r.key().startsWith("WG:CLOAKKAMA_")).map(TcRecipe::key).toArray(String[]::new)));
                else for(var recipe:recipes)if(recipe.key().startsWith(group.equals("CLUSTERS")?"WG:CLUSTER_":"WG:TRANSMUTE_"))pages.add(recipePage(recipe));
                continue;
            }
            for(String recipeKey:PortData.strings(page,"recipes")){
                var recipe=Recipes.BY_RESEARCH.getOrDefault(key,List.of()).stream().filter(r->r.key().equals("WG:"+recipeKey)).findFirst();
                pages.add(recipePage(recipe.orElseThrow(()->new IllegalStateException("Missing book recipe WG:"+recipeKey))));
            }
        }
        e.setPages(pages.toArray(ResearchPage[]::new));
    });}

    private static ResearchPage recipePage(TcRecipe r){
        if(r instanceof ShapedArcaneRecipe||r instanceof ShapelessArcaneRecipe)return ResearchPage.arcane(r.key());
        if(r instanceof CrucibleRecipe)return ResearchPage.crucible(r.key());
        if(r instanceof InfusionRecipe)return ResearchPage.infusion(r.key());
        if(r instanceof InfusionEnchantmentRecipe)return ResearchPage.infusionEnchantment(r.key());
        if(r instanceof CraftingRecipeRef)return ResearchPage.crafting(r.key());
        if(r instanceof CompoundRecipe)return ResearchPage.compound(r.key());
        throw new IllegalArgumentException("Unsupported book recipe "+r.key());
    }

    public static void bindProxies(){for(var element:PortData.read("proxies")){
        var o=element.getAsJsonObject();String source=PortData.str(o,"original"),key=PortData.str(o,"key");var r=ResearchCategories.getResearch(source);
        if(r==null){WitchingGadgets.LOG.warn("Source Thaumcraft research missing: {}",source);continue;}
        var icon=o.getAsJsonObject("icon");
        var e=new ResearchItem(key,CATEGORY,new AspectList(),PortData.num(o,"x"),PortData.num(o,"y"),1,icon.has("item")?Recipes.output(icon.getAsJsonObject("item")):TcStack.of("thaumcraft:thaumonomicon")){
            @Override public ResourceLocation iconTexture(){return icon.has("texture")?ResourceLocation.parse(PortData.str(icon,"texture")):null;}
            @Override public TcStack iconItem(){return icon.has("item")?super.iconItem():null;}
            @Override public String nameKey(){return r.nameKey();}
            @Override public String textKey(){return r.textKey();}
            @Override public List<ResearchPage> pages(){return r.pages();}
        };
        flags(e,o);e.setStub().setHidden().registerResearchItem();ALIASES.put(source,key);
        // WGFakeResearchItem.setupOriginal did this in 1.7.10. Preserve existing native siblings.
        var siblings=new LinkedHashSet<>(r.siblings());siblings.add(key);r.setSiblings(siblings.toArray(String[]::new));
    }}

    /** Repair missing cross-tab aliases without granting WG children or resetting progress. */
    public static void syncAliases(ServerPlayer player){
        var knowledge=Knowledge.get(player);boolean changed=false;
        for(var alias:ALIASES.entrySet())if(knowledge.isResearchComplete(alias.getKey()))changed|=knowledge.addResearch(alias.getValue());
        if(changed)Knowledge.sync(player);
    }

    static final class Entry extends ResearchItem{
        Entry(String k,AspectList a,int x,int y,int d,TcStack icon){super(k,CATEGORY,a,x,y,d,icon);}
        Entry(String k,AspectList a,int x,int y,int d,ResourceLocation icon){super(k,CATEGORY,a,x,y,d,icon);}
        @Override public String nameKey(){return "witchinggadgets_research_name."+key();}
        @Override public String textKey(){return "witchinggadgets_research_text."+key();}
    }
}
