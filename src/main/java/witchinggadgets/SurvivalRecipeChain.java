package witchinggadgets;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import thaumcraft.alchemy.*;
import thaumcraft.aspect.*;
import thaumcraft.blockentity.ResearchTableBlockEntity;
import thaumcraft.crafting.*;
import thaumcraft.essentia.*;
import thaumcraft.infusion.*;
import thaumcraft.menu.*;
import thaumcraft.network.PlayerCompletePayload;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.*;
import thaumcraft.wand.*;

/** Prepared TC laboratory and raw resources are fixtures. Every WG intermediate is actually produced. */
final class SurvivalRecipeChain {
    record Job(String key,String research,List<TcIngredient> inputs,TcStack output,TcRecipe tc,RecipeHolder<CraftingRecipe> vanilla,int width,int height){}
    final GameTestHelper h;final ServerLevel level;final ServerPlayer p;final BlockPos origin,bench,crafting,desk,wheel,cut,crucible,center,matrix;
    final List<ItemStack> stock=new ArrayList<>();final Map<String,Job> jobs=new LinkedHashMap<>();final Set<String> active=new HashSet<>();
    final List<Map<String,Object>> trace=new ArrayList<>();final List<BlockPos> pedestals=new ArrayList<>(),jars=new ArrayList<>();
    final Map<String,ItemStack> enchantmentInputs=new HashMap<>();
    int produced,primaryNotes,secondaryPurchases,hexMoves;String current="setup";
    SurvivalRecipeChain(GameTestHelper h){this.h=h;level=h.getLevel();origin=h.absolutePos(BlockPos.ZERO);p=OrdinaryPlayTests.player(h);bench=at(2,2,2);crafting=at(5,2,2);desk=at(2,2,5);wheel=at(2,2,8);cut=at(2,2,11);crucible=at(2,2,16);center=at(15,3,12);matrix=center.above(2);}
    BlockPos at(int x,int y,int z){return origin.offset(x,y,z);}
    static void check(boolean ok,String text){SurvivalResearch.check(ok,text);}
    void log(String type,String key,Object... extra){var row=new LinkedHashMap<String,Object>();row.put("type",type);row.put("key",key);for(int i=0;i<extra.length;i+=2)row.put(extra[i].toString(),extra[i+1]);trace.add(row);System.out.println("WG_CHAIN: "+row);}
    void save(String status,Throwable error){try{var out=new LinkedHashMap<String,Object>();out.put("status",status);out.put("current",current);out.put("twilight",Compat.twilight());out.put("rawFixtureBoundary","Prepared TC knowledge, TC laboratory, vis/essentia, XP and non-WG raw materials; all WG research puzzles and intermediates use native gameplay paths");out.put("produced",produced);out.put("primaryNotesSolved",primaryNotes);out.put("secondaryPurchases",secondaryPurchases);out.put("hexPlacements",hexMoves);out.put("recipes",jobs.keySet());out.put("trace",trace);if(error!=null)out.put("error",error.toString());java.nio.file.Files.writeString(java.nio.file.Path.of("wg-recipe-chain.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    CompletableFuture<Void> waitFor(String name,int limit,BooleanSupplier done,Runnable finish){var result=new CompletableFuture<Void>();poll(result,name,limit,done,finish);return result;}
    void poll(CompletableFuture<Void> result,String name,int remaining,BooleanSupplier done,Runnable finish){if(result.isDone())return;try{check(!p.isDeadOrDying(),"survival player died at "+name);if(done.getAsBoolean()){finish.run();result.complete(null);return;}check(remaining>0,"timed out at "+name);h.runAfterDelay(1,()->poll(result,name,remaining-1,done,finish));}catch(Throwable error){result.completeExceptionally(error);}}
    void put(ItemStack stack){if(stack.isEmpty())return;for(var old:stock)if(ItemStack.isSameItemSameComponents(old,stack)&&old.getMaxStackSize()>1){int n=Math.min(stack.getCount(),old.getMaxStackSize()-old.getCount());old.grow(n);stack.shrink(n);if(stack.isEmpty())return;}stock.add(stack);}
    void stow(){p.closeContainer();for(int i=0;i<p.getInventory().getContainerSize();i++)put(p.getInventory().removeItemNoUpdate(i));}
    int count(TcIngredient ingredient){return stock.stream().filter(ingredient::test).mapToInt(ItemStack::getCount).sum();}
    ItemStack take(TcIngredient ingredient){for(var stack:stock)if(!stack.isEmpty()&&ingredient.test(stack))return stack.split(1);throw new IllegalStateException("produced stock lacks "+ingredient+" at "+current);}
    ItemStack take(String id){return take(TcIngredient.item(id));}
    void near(BlockPos pos){p.moveTo(pos.getX()+1.5,pos.getY(),pos.getZ()+2.5,180,60);p.setYHeadRot(180);}
    void click(ItemStack stack,BlockPos pos){near(pos);GameplayTests.click(p,stack,pos,Direction.UP);}
    void place(Block block,BlockPos pos){GameplayTests.place(h,p,new ItemStack(block),pos);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
    ItemStack wand(){var stack=Wands.create(WandRegistry.cap("gold"),WandRegistry.rod("silverwood"),false);Wands.fill(stack);return stack;}
    void open(BlockPos pos){stow();near(pos);GameplayTests.click(p,ItemStack.EMPTY,pos,Direction.UP);}
    void insert(int slot,ItemStack input){OrdinaryPlayTests.insert(p,slot,input);}
    ItemStack extract(int slot){var menu=p.containerMenu;check(menu.getCarried().isEmpty(),"cursor already occupied");menu.clicked(slot,0,ClickType.PICKUP,p);var output=menu.getCarried();menu.setCarried(ItemStack.EMPTY);return output;}
    static Map<String,Integer> containerCounts(List<ItemStack> items){var counts=new TreeMap<String,Integer>();for(var item:items)if(!item.isEmpty())counts.merge(BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),item.getCount(),Integer::sum);return counts;}
    void init(){
        level.addNewPlayer(p);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setHealth(20);p.getFoodData().setFoodLevel(20);p.giveExperienceLevels(300);
        var knowledge=Knowledge.get(p);knowledge.clearResearch();for(var r:ResearchCategories.allResearch())if(!r.category().equals(Research.CATEGORY))knowledge.addResearch(r.key());
        for(var a:Aspect.values())knowledge.setAspectPool(a,10000);ResearchManager.grantAutoUnlock(p);
        for(var pos:BlockPos.betweenClosed(origin.offset(0,1,0),origin.offset(28,1,25)))level.setBlockAndUpdate(pos,Blocks.STONE_BRICKS.defaultBlockState());
        place(ModBlocks.ARCANE_WORKTABLE.get(),bench);
        place(Blocks.CRAFTING_TABLE,crafting);
        level.setBlock(desk,ModBlocks.RESEARCH_TABLE.get().defaultBlockState().setValue(thaumcraft.block.ResearchTableBlock.FACING,Direction.NORTH),2);
        level.setBlock(desk.north(),ModBlocks.RESEARCH_TABLE_PART.get().defaultBlockState().setValue(thaumcraft.block.ResearchTablePartBlock.FACING,Direction.SOUTH),2);
        place(ModBlocks.ARCANE_PEDESTAL.get(),center);place(ModBlocks.RUNIC_MATRIX.get(),matrix);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){place(ModBlocks.ARCANE_STONE_BRICKS.get(),center.offset(x,0,z));place(ModBlocks.ARCANE_STONE.get(),center.offset(x,1,z));}
        p.gameMode.destroyBlock(matrix.below());
        for(var offset:List.of(new int[]{-2,0},new int[]{2,0},new int[]{0,-2},new int[]{0,2},new int[]{-2,-2},new int[]{2,2},new int[]{-2,2},new int[]{2,-2},new int[]{-3,0},new int[]{3,0},new int[]{0,-3},new int[]{0,3})){var pos=center.offset(offset[0],0,offset[1]);pedestals.add(pos);place(ModBlocks.ARCANE_PEDESTAL.get(),pos);}
        var candle=ModBlocks.CANDLES.values().iterator().next().get();int pairs=0;
        for(int x=1;x<=8&&pairs<120;x++)for(int z=-8;z<=8&&pairs<120;z++)if(x>=3||Math.abs(z)>=3){var a=center.offset(x,0,z);var b=center.offset(-x,0,-z);if(!level.isEmptyBlock(a)||!level.isEmptyBlock(b))continue;place(candle,a);place(candle,b);pairs++;}
        for(int x=-5;x<=5;x++)for(int z:new int[]{-5,5}){var pos=center.offset(x,-1,z);level.removeBlock(pos,false);place(ModBlocks.WARDED_JAR.get(),pos);jars.add(pos);}
        click(wand(),matrix);check(tile().isActive(),"ordinary wand did not form prepared altar");
        place(Blocks.CAULDRON,crucible);click(wand(),crucible);check(level.getBlockEntity(crucible) instanceof CrucibleBlockEntity,"native cauldron wand conversion failed");level.setBlockAndUpdate(crucible.below(),Blocks.LAVA.defaultBlockState());click(new ItemStack(Items.WATER_BUCKET),crucible);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        registerJobs();stow();log("boundary","prepared native TC laboratory","jobs",jobs.size());
    }
    void registerJobs(){
        var refs=new HashSet<ResourceLocation>();
        for(var r:TcRecipes.all())if(r.key().startsWith("WG:")&&!(r instanceof CompoundRecipe)&&!r.key().equals("WG:WALLMIRROR")&&!r.key().equals("WG:SCANCAMERA_DEVELOP")){
            if(r instanceof CraftingRecipeRef ref){var holder=level.getRecipeManager().byKey(ref.recipe()).orElseThrow();@SuppressWarnings("unchecked") var typed=(RecipeHolder<CraftingRecipe>)(Object)holder;nativeJob(r.key(),r.research(),typed,r);refs.add(ref.recipe());continue;}
            var ingredients=r instanceof ShapedArcaneRecipe s?s.grid():r instanceof ShapelessArcaneRecipe s?s.ingredients():r instanceof InfusionRecipe s?java.util.stream.Stream.concat(java.util.stream.Stream.of(s.central()),s.components().stream()).toList():r instanceof InfusionEnchantmentRecipe s?s.components():r instanceof CrucibleRecipe s?List.of(s.catalyst()):List.<TcIngredient>of();
            var output=r instanceof ShapedArcaneRecipe s?s.result():r instanceof ShapelessArcaneRecipe s?s.result():r instanceof InfusionRecipe s?s.result():r instanceof CrucibleRecipe s?s.result():null;
            jobs.put(r.key(),new Job(r.key(),r.research(),ingredients,output,r,null,r instanceof ShapedArcaneRecipe s?s.width():3,r instanceof ShapedArcaneRecipe s?s.height():3));
        }
        for(var holder:level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING))if(holder.id().getNamespace().equals(WitchingGadgets.ID)&&!refs.contains(holder.id())&&!(holder.value() instanceof DyeRecipe))nativeJob("craft:"+holder.id(),"",holder,null);
    }
    void nativeJob(String key,String research,RecipeHolder<CraftingRecipe> holder,TcRecipe tc){var r=holder.value();var input=r.getIngredients().stream().map(i->i.isEmpty()?TcIngredient.EMPTY:TcIngredient.anyOf(Arrays.stream(i.getItems()).map(s->TcIngredient.item(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())).toArray(TcIngredient[]::new))).toList();var output=r.getResultItem(level.registryAccess());jobs.put(key,new Job(key,research,input,TcStack.of(BuiltInRegistries.ITEM.getKey(output.getItem()).toString(),output.getCount()),tc,holder,r instanceof ShapedRecipe s?s.getWidth():3,r instanceof ShapedRecipe s?s.getHeight():3));}
    CompletableFuture<Void> ensure(TcIngredient ingredient){
        if(ingredient==TcIngredient.EMPTY||ingredient instanceof TcIngredient.Empty||count(ingredient)>0)return CompletableFuture.completedFuture(null);
        var example=ingredient.displayStacks().stream().findFirst().orElseThrow(()->new IllegalStateException("no ingredient candidate "+ingredient));var id=BuiltInRegistries.ITEM.getKey(example.getItem());
        boolean custom=!Data.get(example).isEmpty();
        if(!id.getNamespace().equals(WitchingGadgets.ID)&&!custom){put(example.copyWithCount(1));log("raw",id.toString());return CompletableFuture.completedFuture(null);}
        if(id.getPath().equals("wolf_pelt")){
            for(int tries=0;tries<32&&count(ingredient)==0;tries++){var wolf=new net.minecraft.world.entity.animal.Wolf(EntityType.WOLF,level);wolf.moveTo(p.getX()+1,p.getY(),p.getZ());level.addFreshEntity(wolf);wolf.hurt(p.damageSources().playerAttack(p),100);collectDrops(ingredient,Vec3.atCenterOf(p.blockPosition()),5);}
            check(count(ingredient)>0,"ordinary wolf kill produced no pelt");return CompletableFuture.completedFuture(null);
        }
        if(id.getPath().equals("powerless_pearl"))return produce("WG:PRIMORDIALGLOVE").thenRun(()->check(count(ingredient)>0,"real gauntlet infusion did not return powerless pearl"));
        if(id.getPath().equals("yarn")||id.getPath().equals("golden_thread")||id.getPath().equals("thaumium_thread"))return spin(id.getPath());
        if(id.getPath().equals("iconographic_plate"))return photo();
        if(id.getPath().equals("gemstone_dust"))return gemDust();
        var producer=jobs.values().stream().filter(j->j.output()!=null&&ingredient.test(j.output().create())&&!active.contains(j.key())).findFirst().orElseThrow(()->new IllegalStateException("WG ingredient has no survival producer "+ingredient));
        return produce(producer.key()).thenRun(()->check(count(ingredient)>0,"producer output did not match "+ingredient));
    }
    CompletableFuture<Void> learn(String key){
        if(key.isEmpty()||ResearchManager.isComplete(p,key))return CompletableFuture.completedFuture(null);
        var r=ResearchCategories.getResearch(key);check(r!=null,"missing research "+key);check(r.category().equals(Research.CATEGORY),"native TC boundary was not prepared: "+key);
        CompletableFuture<Void> done=CompletableFuture.completedFuture(null);for(var parent:r.parents())done=done.thenCompose(v->learn(parent));for(var parent:r.parentsHidden())done=done.thenCompose(v->learn(parent));
        if(r.isHidden()||r.isLost())done=done.thenCompose(v->{var trigger=r.itemTriggers().stream().findFirst().orElseThrow();var ing=TcIngredient.item(trigger.toString());return ensure(ing).thenRun(()->{var stack=stock.stream().filter(ing::test).findFirst().orElseThrow();check(ScanManager.completeScan(p,ScanManager.resolveStack(stack)),"native scan rejected clue");check(ResearchManager.isComplete(p,"@"+key),"normal scan did not reveal hidden "+key);log("clue",key);});});
        return done.thenRun(()->{
            check(ResearchManager.hasRequisites(p,key),"research prerequisites not earned "+key);stow();current="research "+key;
            if(ResearchManager.isBuyable(r)){
                var before=new EnumMap<Aspect,Integer>(Aspect.class);for(var a:r.tags().getAspects())before.put(a,Knowledge.get(p).getAspectPool(a));ResearchManager.handlePlayerComplete(new PlayerCompletePayload(key,0),p);
                check(ResearchManager.isComplete(p,key),"native research purchase failed "+key);for(var a:before.keySet())check(Knowledge.get(p).getAspectPool(a)==before.get(a)-r.tags().getAmount(a),"wrong paid research cost "+key);secondaryPurchases++;log("purchase",key);
            }else{
                p.getInventory().add(new ItemStack(Items.PAPER,2));p.getInventory().add(new ItemStack(Content.tc("scribing_tools")));ResearchManager.handlePlayerComplete(new PlayerCompletePayload(key,1),p);int noteSlot=ResearchNotes.findNoteSlot(p,key);check(noteSlot>=0,"native book request produced no note "+key);var note=p.getInventory().removeItemNoUpdate(noteSlot);stow();open(desk);check(p.containerMenu instanceof ResearchTableMenu,"ordinary research table did not open");put(extract(0));insert(0,new ItemStack(Content.tc("scribing_tools")));insert(1,note);hexMoves+=SurvivalResearch.solve(p,desk);var discovery=extract(1);check(ResearchNoteKeys.NOTE.get(discovery).complete(),"puzzle was not actually solved");stow();OrdinaryPlayTests.use(p,discovery);check(ResearchManager.isComplete(p,key),"normal discovery use failed "+key);stow();primaryNotes++;log("solved-note",key,"hexPlacementsTotal",hexMoves);
            }
        });
    }
    CompletableFuture<Void> produce(String key){
        var j=jobs.get(key);check(j!=null,"no job "+key);check(active.add(key),"recipe cycle "+active+" -> "+key);
        return learn(j.research()).thenCompose(v->{CompletableFuture<Void> ready=CompletableFuture.completedFuture(null);var reserved=new ArrayList<ItemStack>();for(var ingredient:j.inputs())if(!(ingredient instanceof TcIngredient.Empty))ready=ready.thenCompose(w->ensure(ingredient)).thenRun(()->reserved.add(take(ingredient)));return ready.thenCompose(w->{current=key;return craft(j,reserved);});}).whenComplete((v,e)->active.remove(key));
    }
    CompletableFuture<Void> craft(Job j,List<ItemStack> inputs){
        if(j.tc() instanceof InfusionRecipe||j.tc() instanceof InfusionEnchantmentRecipe)return infuse(j,inputs);
        if(j.tc() instanceof CrucibleRecipe r)return alchemy(j,r,inputs.getFirst());
        var expectedContainers=containerCounts(inputs.stream().map(ItemStack::getCraftingRemainingItem).toList());
        if(j.vanilla()!=null){stow();open(crafting);check(p.containerMenu instanceof CraftingMenu,"ordinary crafting table did not open");int n=0;for(int i=0;i<j.inputs().size();i++)if(!(j.inputs().get(i) instanceof TcIngredient.Empty)){int slot=j.vanilla().value() instanceof ShapedRecipe?1+(i/j.width())*3+i%j.width():1+i;insert(slot,inputs.get(n++));}var output=extract(0);check(j.output().matches(output)&&output.getCount()==j.output().count(),"ordinary vanilla output mismatch "+j.key()+": "+output);var returns=new ArrayList<ItemStack>();for(int i=1;i<=9;i++)returns.add(extract(i));check(containerCounts(returns).equals(expectedContainers),"vanilla ingredient debit/containers incorrect "+j.key());for(var returned:returns)put(returned);var description=output.toString();put(output);stow();produced++;log("crafted",j.key(),"output",description,"station","crafting table");return CompletableFuture.completedFuture(null);}
        stow();open(bench);check(p.containerMenu instanceof ArcaneWorkbenchMenu,"ordinary worktable did not open");put(extract(1));var charged=wand();var expected=charged.copy();insert(1,charged);int n=0;
        for(int i=0;i<j.inputs().size();i++){var ingredient=j.inputs().get(i);if(ingredient instanceof TcIngredient.Empty)continue;int row=i/j.width(),col=i%j.width();int slot=j.tc() instanceof ShapelessArcaneRecipe||j.vanilla()!=null&&!(j.vanilla().value() instanceof ShapedRecipe)?2+i:2+row*3+col;insert(slot,inputs.get(n++));}
        var menu=(ArcaneWorkbenchMenu)p.containerMenu;var cost=menu.getMatchedAspects();if(!cost.isEmpty())check(((WandItem)expected.getItem()).consumeAllVisCrafting(expected,p,cost,true),"fixture wand cannot afford recipe");
        var output=extract(0);check(j.output()!=null&&j.output().matches(output)&&output.getCount()==j.output().count(),"native output mismatch "+j.key()+" -> "+output);
        if(j.key().equals("WG:WGBAUBLES_COIN"))check(LuckyCoin.enchanted(output)&&output.isEnchanted(),"crafted lucky coin is only a marker without original native enchantments");
        for(var a:GemItem.PRIMALS)check(Wands.getVis(menu.getWand(),a)==Wands.getVis(expected,a),"worktable did not pay native vis cost "+j.key()+" "+a);
        var remainingItems=new ArrayList<ItemStack>();for(int slot=2;slot<11;slot++)remainingItems.add(extract(slot));check(containerCounts(remainingItems).equals(expectedContainers),"workbench consumed ingredients/returned containers incorrectly "+j.key());for(var remaining:remainingItems)put(remaining);var description=output.toString();put(output);stow();produced++;log("crafted",j.key(),"output",description,"vis",cost.toString(),"returnedContainers",expectedContainers);return CompletableFuture.completedFuture(null);
    }
    InfusionMatrixBlockEntity tile(){return (InfusionMatrixBlockEntity)level.getBlockEntity(matrix);}
    int essentia(){return jars.stream().mapToInt(pos->((JarBlockEntity)level.getBlockEntity(pos)).getAmount()).sum();}
    CompletableFuture<Void> infuse(Job j,List<ItemStack> inputs){
        stow();ItemStack central;List<ItemStack> components;
        if(j.tc() instanceof InfusionRecipe){central=inputs.getFirst();components=inputs.subList(1,inputs.size());}else{var r=(InfusionEnchantmentRecipe)j.tc();central=enchantmentInputs.remove(j.key());if(central==null)central=new ItemStack(switch(r.enchantment().getPath()){case "backstabbing"->Items.DIAMOND_SWORD;case "unveiling"->Content.tc("goggles_of_revealing");case "throwoff"->Items.DIAMOND_HELMET;case "stealth"->Items.DIAMOND_LEGGINGS;default->Items.DIAMOND_CHESTPLATE;});components=inputs;}
        while(p.getHealth()<p.getMaxHealth()){var potion=new ItemStack(Items.POTION);potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.STRONG_HEALING));put(potion.finishUsingItem(level,p));log("raw","minecraft:strong_healing_potion");}
        int expectedEnchantLevel=j.tc() instanceof InfusionEnchantmentRecipe r?InfusionCrafting.enchantmentLevel(central,InfusionCrafting.enchantment(level,r.enchantment()))+1:0;
        var predicted=j.tc() instanceof InfusionRecipe r&&!r.result().data().isEmpty()?central.copy():j.output()!=null?j.output().create():central.copy();
        int pearlCount=Data.get(predicted).getBoolean("WasCrafted")?0:predicted.getItem() instanceof PrimordialArmor?3:predicted.getItem() instanceof PrimordialItem?2:predicted.is(Content.item("primordial_gauntlet"))?1:0;
        int pearlsBefore=count(TcIngredient.item("witchinggadgets:powerless_pearl"));
        check(!tile().isCrafting(),"previous infusion still active");for(var pos:pedestals)check(((PedestalBlockEntity)level.getBlockEntity(pos)).getStack().isEmpty(),"previous pedestal not empty");
        var plan=j.tc() instanceof InfusionRecipe?InfusionCrafting.findRecipe(components,central,p):InfusionCrafting.findEnchantment(level,components,central,p);check(plan!=null&&plan.key().equals(j.key()),"native matrix selected wrong recipe "+j.key()+": "+plan);
        var expectedContainers=containerCounts(components.stream().map(ItemStack::getCraftingRemainingItem).toList());
        click(central,center);for(int i=0;i<components.size();i++)click(components.get(i),pedestals.get(i));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(var pos:jars){p.setShiftKeyDown(true);click(ItemStack.EMPTY,pos);p.setShiftKeyDown(false);}
        int index=0;for(var aspect:plan.aspects().getAspects()){int need=plan.aspects().getAmount(aspect);while(need>0){var pos=jars.get(index++);int amount=Math.min(64,need);for(int fill=0;fill<amount;fill+=8){var phial=new ItemStack(Content.tc("essentia_phial"));AspectKeys.ASPECTS.set(phial,new AspectList().add(aspect,8));click(phial,pos);}need-=amount;}}
        int before=essentia(),xp=p.experienceLevel;check(before>=plan.aspects().visSize(),"native jars not filled");near(matrix);click(wand(),matrix);check(tile().isCrafting(),"normal wand did not start "+j.key());check(tile().getInstability()<=0,"prepared symmetric altar not stable enough: "+tile().getInstability());
        // Like a player putting the activating wand away; passive wand scans are unrelated to crafting.
        stow();
        return waitFor(j.key(),18000,()->!tile().isCrafting(),()->{
            stow(); // Preserve byproducts received in the now-empty hotbar before picking up the result.
            var output=((PedestalBlockEntity)level.getBlockEntity(center)).getStack().copy();if(j.output()!=null)check(j.output().matches(output),"infusion did not produce "+j.key()+": "+output);
            else{var enchantment=InfusionCrafting.enchantment(level,((InfusionEnchantmentRecipe)j.tc()).enchantment());check(InfusionCrafting.enchantmentLevel(output,enchantment)==expectedEnchantLevel,"real enchantment infusion failed");check(p.experienceLevel==xp-plan.xp(),"enchantment XP not paid");}
            check(before-essentia()==plan.aspects().visSize(),"infusion lost/duplicated essentia "+j.key());var actualContainers=containerCounts(pedestals.stream().map(pos->((PedestalBlockEntity)level.getBlockEntity(pos)).getStack()).toList());check(actualContainers.equals(expectedContainers),"infusion component consumption/containers differ from original "+j.key());for(var pos:pedestals){var rem=((PedestalBlockEntity)level.getBlockEntity(pos)).getStack();if(!rem.isEmpty()){p.setShiftKeyDown(true);click(ItemStack.EMPTY,pos);p.setShiftKeyDown(false);}}
            p.setShiftKeyDown(true);click(ItemStack.EMPTY,center);p.setShiftKeyDown(false);collectPlayer();check(stock.stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,output)),"finished infusion not collectible");check(count(TcIngredient.item("witchinggadgets:powerless_pearl"))==pearlsBefore+pearlCount,"actual infusion returned wrong number of powerless pearls "+j.key());produced++;log("infused",j.key(),"output",output.toString(),"essentiaSpent",before-essentia(),"xpSpent",xp-p.experienceLevel,"powerlessPearlsReturned",pearlCount,"returnedContainers",expectedContainers);
        });
    }
    void collectPlayer(){for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(origin),Vec3.atLowerCornerOf(origin.offset(30,12,28))))){near(drop.blockPosition());drop.setNoPickUpDelay();drop.playerTouch(p);}stow();}
    void collectDrops(TcIngredient item,Vec3 at,double range){for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(range)))if(item.test(drop.getItem())){drop.setNoPickUpDelay();drop.playerTouch(p);}stow();}
    ItemEntity toss(ItemStack stack,BlockPos pos){near(pos);var drop=new ItemEntity(level,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,stack);drop.setThrower(p);drop.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(drop);return drop;}
    CompletableFuture<Void> alchemy(Job j,CrucibleRecipe r,ItemStack catalyst){
        stow();click(new ItemStack(Items.WATER_BUCKET),crucible);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var pot=(CrucibleBlockEntity)level.getBlockEntity(crucible);
        return waitFor("heat "+j.key(),240,()->pot.isBoiling(),()->{}).thenCompose(v->{
            check(pot.getAspects().isEmpty(),"previous crucible residue");for(var a:r.aspects().getAspects()){var crystal=new ItemStack(Content.tc("crystallized_essence"),r.aspects().getAmount(a));AspectKeys.ASPECTS.set(crystal,new AspectList().add(a,1));toss(crystal,crucible);}
            return waitFor("dissolve "+j.key(),80,()->r.aspects().getAspects().stream().allMatch(a->pot.getAspects().getAmount(a)>=r.aspects().getAmount(a)),()->{});
        }).thenCompose(v->{int water=pot.getWaterAmount();toss(catalyst,crucible);return waitFor(j.key(),100,()->level.getEntitiesOfClass(ItemEntity.class,new AABB(crucible).inflate(3)).stream().anyMatch(e->r.result().matches(e.getItem())),()->{collectDrops(TcIngredient.of(r.result()),Vec3.atCenterOf(crucible),4);check(pot.getWaterAmount()==water-50,"wrong crucible water cost");check(pot.getAspects().isEmpty(),"crucible did not pay exact aspects "+j.key()+": "+pot.getAspects());produced++;log("alchemy",j.key(),"output",r.result().toString(),"aspects",r.aspects().toString());});});
    }
    CompletableFuture<Void> spin(String name){
        CompletableFuture<Void> ready=CompletableFuture.completedFuture(null);if(!(level.getBlockEntity(wheel) instanceof MachineEntity))ready=produce("WG:SPINNINGWHEEL").thenRun(()->{var stack=take("witchinggadgets:spinning_wheel");GameplayTests.place(h,p,stack,wheel);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});
        return ready.thenCompose(v->{open(wheel);for(int i=0;i<4;i++)insert(i,new ItemStack(i<2||name.equals("yarn")?Items.STRING:name.equals("golden_thread")?Items.GOLD_NUGGET:Content.tc("thaumium_nugget")));return waitFor("spinning "+name,160,()->p.containerMenu.getSlot(5).hasItem(),()->{var output=extract(5);check(output.is(Content.item(name))&&output.getCount()==2,"wrong ordinary spinning yield "+name);for(int i=0;i<4;i++)check(p.containerMenu.getSlot(i).getItem().isEmpty(),"spinning input not paid");put(output);stow();log("spun",name,"count",2);});});
    }
    CompletableFuture<Void> cuttingTable(){if(level.getBlockEntity(cut) instanceof MachineEntity)return CompletableFuture.completedFuture(null);return produce("WG:GEMCUTTING_TOOLS").thenRun(()->{place(ModBlocks.TABLE.get(),cut);var tools=take("witchinggadgets:gemcutters_tools");click(tools,cut);check(tools.getCount()==1,"table creation consumed reusable original tools");put(tools);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);check(level.getBlockState(cut).is(Content.block("cutting_table")),"normal tools did not form cutting table");log("formed","cutting_table");});}
    CompletableFuture<Void> gemDust(){return learn("GEMCUTTING").thenCompose(v->cuttingTable()).thenRun(()->{open(cut);insert(0,new ItemStack(Items.QUARTZ));var phial=new ItemStack(Content.tc("essentia_phial"));AspectKeys.ASPECTS.set(phial,new AspectList().add(Aspect.ORDER,8));insert(1,phial);var gem=extract(4);check(gem.getItem() instanceof GemItem,"native cutting output absent");stow();p.setHealth(2);OrdinaryPlayTests.use(p,gem);collectPlayer();check(count(TcIngredient.item("witchinggadgets:gemstone_dust"))>0,"normal produced point-gem use did not yield dust");log("cut-and-used","gemstone_dust");});}
    CompletableFuture<Void> photo(){return ensure(TcIngredient.item("witchinggadgets:scan_camera")).thenRun(()->{stow();var target=at(5,2,20);level.setBlockAndUpdate(target,Blocks.STONE.defaultBlockState());p.moveTo(target.getX()+.5,target.getY()-1,target.getZ()+3.5,180,0);p.setYHeadRot(180);p.getInventory().setItem(10,new ItemStack(Content.tc("mirrored_glass")));var camera=take("witchinggadgets:scan_camera");OrdinaryPlayTests.use(p,camera);stow();check(count(TcIngredient.item("witchinggadgets:iconographic_plate"))>0,"crafted camera produced no actual plate; cooldown="+p.getCooldowns().isOnCooldown(Content.item("scan_camera"))+"; ray="+p.pick(10,1,true));log("photographed","iconographic_plate");});}
    CompletableFuture<Void> developing(){return ensure(TcIngredient.item("witchinggadgets:iconographic_plate")).thenRun(()->{
        current="dynamic photo developing";var plate=take("witchinggadgets:iconographic_plate");String photo=Data.get(plate).getString("PhotoKey");open(bench);put(extract(1));insert(1,wand());insert(2,plate);insert(3,new ItemStack(Items.PAPER,2));insert(4,new ItemStack(Items.INK_SAC,2));
        for(int copy=0;copy<2;copy++){var result=extract(0);check(result.is(Content.item("developed_iconograph"))&&Data.get(result).getString("PhotoKey").equals(photo),"photo recipe lost recorded scan data");check(p.containerMenu.getSlot(2).getItem().getCount()==1,"photo recipe consumed reusable plate");put(result);}
        check(p.containerMenu.getSlot(3).getItem().isEmpty()&&p.containerMenu.getSlot(4).getItem().isEmpty(),"photo copies did not consume paper/ink exactly");for(int i=2;i<11;i++)put(extract(i));stow();log("developed","SCANCAMERA_DEVELOP","copies",2,"masterPreserved",true);
    });}
    CompletableFuture<Void> higherEnchantments(){CompletableFuture<Void> done=CompletableFuture.completedFuture(null);
        for(var j:jobs.values())if(j.tc() instanceof InfusionEnchantmentRecipe r){var holder=InfusionCrafting.enchantment(level,r.enchantment());for(int target=2;target<=holder.value().getMaxLevel();target++){int previous=target-1;done=done.thenCompose(v->{var input=stock.stream().filter(s->!s.isEmpty()&&InfusionCrafting.enchantmentLevel(s,holder)==previous).findFirst().orElseThrow(()->new IllegalStateException("missing genuinely enchanted previous level "+j.key()));enchantmentInputs.put(j.key(),input.split(1));return produce(j.key());});}
            done=done.thenRun(()->{var item=stock.stream().filter(s->!s.isEmpty()&&InfusionCrafting.enchantmentLevel(s,holder)==holder.value().getMaxLevel()).findFirst().orElseThrow();var components=r.components().stream().map(i->i.displayStacks().getFirst()).toList();check(InfusionCrafting.findEnchantment(level,components,item,p)==null,"max-level enchantment still craftable");log("max-level-rejected",j.key(),"level",holder.value().getMaxLevel());});
        }return done;
    }
    CompletableFuture<Void> dyeRecipes(){CompletableFuture<Void> done=CompletableFuture.completedFuture(null);
        for(String name:List.of("bag_of_tricks","voidlinked_bag","enderlinked_bag","hungry_bag","cloak","kama","bewitched_robe","bewitched_leggings"))done=done.thenCompose(v->ensure(TcIngredient.item("witchinggadgets:"+name))).thenRun(()->{
            var base=take("witchinggadgets:"+name);var token=java.util.UUID.randomUUID();Data.get(base);var tag=Data.get(base);tag.putUUID("AuditOwner",token);Data.put(base,tag);if(base.isDamageableItem())base.setDamageValue(17);
            for(var dye:DyeColor.values()){
                current="dye "+name+" "+dye;open(crafting);insert(1,base);insert(2,new ItemStack(DyeItem.byColor(dye)));base=extract(0);check(base.is(Content.item(name))&&base.has(net.minecraft.core.component.DataComponents.DYED_COLOR),"ordinary dye recipe failed "+current);check(Data.get(base).getUUID("AuditOwner").equals(token),"dye recipe lost data");if(base.isDamageableItem())check(base.getDamageValue()==17,"dye recipe repaired damage for free");check(p.containerMenu.getSlot(1).getItem().isEmpty()&&p.containerMenu.getSlot(2).getItem().isEmpty(),"dye recipe did not debit ingredients");stow();
            }
            if(base.getItem() instanceof BagItem){open(crafting);insert(1,base);base=extract(0);check(!base.has(net.minecraft.core.component.DataComponents.DYED_COLOR),"bag color reset failed");stow();}
            put(base);log("dyed",name,"colors",16,"dataPreserved",true);
        });return done;
    }
    CompletableFuture<Void> cuttingRecipes(){return learn("GEMCUTTING").thenCompose(v->cuttingTable()).thenRun(()->{
        for(var material:List.of(new ItemStack(Items.DIAMOND),new ItemStack(Items.EMERALD),new ItemStack(Items.QUARTZ),new ItemStack(Content.tc("amber"))))for(var aspect:GemItem.PRIMALS)for(int shape=0;shape<2;shape++)for(int sources=1;sources<=3;sources++)for(boolean phial:new boolean[]{true,false}){
            current="cut "+material+" "+aspect+" shape="+shape+" inputs="+sources+" phial="+phial;open(cut);p.containerMenu.clickMenuButton(p,shape);insert(0,material.copyWithCount(1));
            for(int i=1;i<=sources;i++){var input=new ItemStack(Content.tc(phial?"essentia_phial":"crystallized_essence"));AspectKeys.ASPECTS.set(input,new AspectList().add(aspect,phial?8:1));insert(i,input);}var output=extract(4);boolean affinity=GemMaterials.affinity(GemMaterials.material(material),aspect);int power=affinity?Math.max(0,sources-shape):0,brittle=affinity?0:sources+(GemMaterials.aversion(GemMaterials.material(material),aspect)?1:0);
            check(output.getItem() instanceof GemItem&&GemItem.aspect(output)==aspect&&Data.get(output).getInt("Cut")==shape&&Data.get(output).getInt("Potency")==power&&Data.get(output).getInt("Brittle")==brittle,"cutting differs from original: "+current);check(output.hasFoil()==(power>0||brittle>0),"cut gem lost original enchantment shine "+current);for(int i=0;i<=3;i++)check(p.containerMenu.getSlot(i).getItem().isEmpty(),"cut input not consumed "+current);put(output);stow();
        }log("cutting-grid","all available base gems","variants",288);
    });}
    CompletableFuture<Void> blastFurnace(){return learn("INFERNALBLASTFURNACE").thenCompose(v->{
        current="form infernal blast furnace";stow();var root=at(32,2,12);
        for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
            if(y==2&&x==0&&z==0)continue;var pos=root.offset(x,y,z);var block=y<2?Multiblock.expectedBrick(y*9+(z+1)*3+x+1):Blocks.NETHER_BRICK_STAIRS;var direction=x<0?Direction.EAST:x>0?Direction.WEST:z<0?Direction.SOUTH:Direction.NORTH;
            if(level.isEmptyBlock(pos.below()))level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());p.moveTo(pos.getX()+2.5,pos.getY(),pos.getZ()+2.5,direction.toYRot(),85);p.setShiftKeyDown(true);GameplayTests.click(p,new ItemStack(block),pos.below(),Direction.UP);p.setShiftKeyDown(false);check(level.getBlockState(pos).is(block),"blast furnace ordinary brick placement failed");
        }
        p.moveTo(root.getX()+.5,root.getY()+3,root.getZ()+.5,0,90);OrdinaryPlayTests.use(p,new ItemStack(Items.LAVA_BUCKET));check(level.getBlockState(root.above(2)).is(Blocks.LAVA),"normal lava bucket failed to fill furnace");check(Multiblock.valid(level,root),"hand-built furnace pattern is invalid");click(wand(),root.offset(-1,0,-1));check(level.getBlockEntity(root) instanceof MachineEntity,"ordinary wand did not form furnace");log("formed","infernal_blast_furnace");
        CompletableFuture<Void> results=CompletableFuture.completedFuture(null);
        for(String name:List.of("native_iron_cluster","native_gold_cluster","native_copper_cluster","native_tin_cluster","native_silver_cluster","native_lead_cluster","native_cinnabar_cluster")){
            var input=new ItemStack(Content.tc(name));var blast=BlastRecipes.resolve(input);if(input.isEmpty()||blast==null){log("conditional-unavailable",name);continue;}
            results=results.thenCompose(w->{
                var ingredient=TcIngredient.item("thaumcraft:"+name);CompletableFuture<Void> ready=CompletableFuture.completedFuture(null);
                if(count(ingredient)==0){var recipe=TcRecipes.all(CrucibleRecipe.class).stream().filter(r->r.result().matches(input)&&r.catalyst().exists()).findFirst().orElseThrow(()->new IllegalStateException("no native cluster alchemy "+name));var j=new Job(recipe.key(),recipe.research(),List.of(recipe.catalyst()),recipe.result(),recipe,null,1,1);ready=ensure(recipe.catalyst()).thenCompose(q->alchemy(j,recipe,take(recipe.catalyst())));}
                return ready.thenCompose(q->{current="blast "+name;var catalyst=take(ingredient);var outputIngredient=TcIngredient.item(BuiltInRegistries.ITEM.getKey(blast.output().getItem()).toString());int before=count(outputIngredient);toss(catalyst,root.above(2));return waitFor(current,700,()->level.getEntitiesOfClass(ItemEntity.class,new AABB(root).inflate(4)).stream().anyMatch(e->outputIngredient.test(e.getItem())),()->{collectDrops(outputIngredient,Vec3.atCenterOf(root),5);check(count(outputIngredient)==before+3,"native cluster furnace yield differs from original "+name);log("smelted",name,"output",blast.output().toString(),"yield",3);});});
            });
        }return results;
    });}
    CompletableFuture<Void> all(){init();CompletableFuture<Void> done=CompletableFuture.completedFuture(null);for(var key:jobs.keySet())done=done.thenCompose(v->produce(key));done=done.thenCompose(v->developing()).thenCompose(v->higherEnchantments()).thenCompose(v->dyeRecipes()).thenCompose(v->cuttingRecipes()).thenCompose(v->blastFurnace());for(var r:ResearchCategories.get(Research.CATEGORY).research())if(!r.isStub())done=done.thenCompose(v->learn(r.key()));return done;}
}
