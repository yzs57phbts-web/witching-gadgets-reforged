package witchinggadgets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;
import thaumcraft.aspect.*;
import thaumcraft.essentia.EssentiaTransport;
import thaumcraft.research.*;

public final class MachineEntity extends BlockEntity implements EssentiaTransport, AspectContainer {
    public final String kind;
    public int progress,steamTicks,gemCut,amount,age;
    public boolean active;
    private ItemStack processing=ItemStack.EMPTY;
    private final List<ItemStack> blastQueue=new ArrayList<>();
    int blastTime(){var r=BlastRecipes.resolve(items.getStackInSlot(0));return r==null?440:Math.max(40,r.ticks()/(speedup>0?2:1)-Multiblock.bellows(this)*40);}
    public Aspect essentia;
    public AspectList reformerEssentia=new AspectList();
    private final EssentiaPump pumpState=new EssentiaPump();
    public BlockPos master;
    public BlockState original;
    public int position=4,speedup;
    private boolean inventoryDirty;
    public final ItemStackHandler items=new ItemStackHandler(27){@Override protected void onContentsChanged(int slot){setChanged();inventoryDirty=true;}};
    public final FluidTank water=new FluidTank(4000,f->f.is(Fluids.WATER)){@Override protected void onContentsChanged(){sync();}};
    private final Set<BlockPos> steamArea=new HashSet<>();
    public MachineEntity(BlockPos p,BlockState s){super(Content.MACHINE.get(),p,s);kind=((MachineBlock)s.getBlock()).kind;}
    public Direction facing(){return getBlockState().getValue(MachineBlock.FACING);}
    public MachineEntity controller(){return master!=null&&!master.equals(worldPosition)&&level!=null&&level.getBlockEntity(master) instanceof MachineEntity root?root:this;}
    public net.neoforged.neoforge.items.IItemHandler automation(){var handler=new MachineCrafting(this);return handler.getSlots()==0?null:handler;}
    public void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level l,BlockPos pos,BlockState state,MachineEntity m){m.age++;if(l.isClientSide()){if(m.steamTicks>0)m.steamTicks--;if(m.kind.equals("stone_extruder")&&!l.hasNeighborSignal(pos))m.progress=(m.progress+1)%40;Effects.clientMachine.accept(m);}else m.serverTick((ServerLevel)l);}
    void serverTick(ServerLevel l){
        if(inventoryDirty){inventoryDirty=false;sync();}
        switch(kind){
            case "stone_extruder","snow_generator"->{if(l.hasNeighborSignal(worldPosition)||!canEmit(new ItemStack(kind.equals("stone_extruder")?Items.COBBLESTONE:Items.SNOWBALL))){if(progress!=0){progress=0;sync();}break;}progress++;if(progress==1)sync();if(kind.equals("stone_extruder")&&progress==32)Effects.sound(l,Vec3.atCenterOf(worldPosition),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,.5F,Effects.fizzPitch(l));if(progress>=40){progress=0;sync();emit(new ItemStack(kind.equals("stone_extruder")?Items.COBBLESTONE:Items.SNOWBALL));if(kind.equals("stone_extruder"))Effects.sound(l,Vec3.atCenterOf(worldPosition),net.minecraft.sounds.SoundEvents.STONE_BREAK,.5F,Effects.fizzPitch(l));}}
            case "spinning_wheel"->spin();
            case "cutting_table"->{if(active)cutGem();}
            case "sauna_stove"->sauna(l);
            case "ageing_stone"->{for(AgeableMob mob:l.getEntitiesOfClass(AgeableMob.class,new AABB(worldPosition).inflate(3,2,3))){int a=mob.getAge();if(a!=0)mob.setAge(a+(a<0?1:-1));}}
            case "ethereal_wall"->{if(age%5==0){boolean next=WallNet.powered(l,worldPosition);if(next!=active){active=next;sync();}}}
            case "glimmer_of_light"->{if(++progress>=Math.max(3600,steamTicks))l.removeBlock(worldPosition,false);}
            case "infernal_blast_furnace"->{if(master!=null&&position==4)blast(l);if(master!=null){var root=controller();boolean lit=root.progress>0;boolean special=lit&&BlastRecipes.resolve(root.items.getStackInSlot(0))!=null&&BlastRecipes.resolve(root.items.getStackInSlot(0)).special();var state=getBlockState();int part=Math.clamp(position,0,26);if(state.getValue(FurnaceBlock.PART)!=part||state.getValue(FurnaceBlock.LIT)!=lit||state.getValue(FurnaceBlock.SPECIAL)!=special)l.setBlock(worldPosition,state.setValue(FurnaceBlock.PART,part).setValue(FurnaceBlock.LIT,lit).setValue(FurnaceBlock.SPECIAL,special),3);}}
            case "essentia_pump"->pump(l);
            case "terrestrial_reformer"->terraform(l);
            case "rose_vines"->{}
        }
    }
    void growVines(boolean ignoreRandom){if(progress<5){progress++;sync();return;}var above=worldPosition.above();if(!level.hasChunkAt(above)||!level.isEmptyBlock(above)||!ignoreRandom&&level.random.nextInt(4)!=0)return;for(Direction d:Direction.Plane.HORIZONTAL)if(level.getBlockState(above.relative(d)).isFaceSturdy(level,above.relative(d),d.getOpposite())){level.setBlockAndUpdate(above,getBlockState());return;}}
    boolean canEmit(ItemStack stack){var target=worldPosition.relative(facing());var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,target,facing().getOpposite());return handler==null||ItemHandlerHelper.insertItemStacked(handler,stack.copy(),true).isEmpty();}
    void emit(ItemStack stack){var target=worldPosition.relative(facing());var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,target,facing().getOpposite());if(handler!=null)stack=ItemHandlerHelper.insertItemStacked(handler,stack,false);if(!stack.isEmpty()){double x=worldPosition.getX()+.5+facing().getStepX()*.66,y=worldPosition.getY()+(facing().getAxis().isHorizontal()?.4:.5+facing().getStepY()*.66),z=worldPosition.getZ()+.5+facing().getStepZ()*.66;var entity=new ItemEntity(level,x,y,z,stack);entity.setDeltaMovement(facing().getStepX()*.075,facing().getAxis().isHorizontal()?.025:facing().getStepY()*.075,facing().getStepZ()*.075);level.addFreshEntity(entity);}}
    boolean output(ItemStack out,int slot){ItemStack current=items.getStackInSlot(slot);if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,out)||current.getCount()+out.getCount()>out.getMaxStackSize())return false;return true;}
    void produce(ItemStack out,int slot){ItemStack current=items.getStackInSlot(slot);if(current.isEmpty())items.setStackInSlot(slot,out.copy());else{current.grow(out.getCount());items.setStackInSlot(slot,current);}sync();}
    void spin(){
        int strings=0,gold=0,thaumium=0;boolean invalid=false;
        for(int i=0;i<5;i++){ItemStack s=items.getStackInSlot(i);if(s.isEmpty())continue;if(s.is(Items.STRING))strings++;else if(s.is(Items.GOLD_NUGGET))gold++;else if(s.is(Content.tc("thaumium_nugget")))thaumium++;else invalid=true;}
        String result=!invalid&&strings==4&&gold+thaumium==0?"yarn":!invalid&&strings==2&&gold==2&&thaumium==0?"golden_thread":!invalid&&strings==2&&thaumium==2&&gold==0?"thaumium_thread":null;
        if(result==null){progress=0;return;}ItemStack out=new ItemStack(Content.item(result),2);if(!output(out,5)){progress=0;return;}if(++progress<121)return;
        for(int i=0;i<5;i++)items.extractItem(i,1,false);produce(out,5);progress=0;
    }
    void cutGem(){var output=MachineCrafting.preview(this);if(output.isEmpty()){active=false;return;}if(!output(output,4))return;MachineCrafting.consume(this);produce(output,4);active=false;}
    void sauna(ServerLevel l){
        if(steamTicks<=0&&water.getFluidAmount()>=1000){water.drain(1000,IFluidHandler.FluidAction.EXECUTE);steamTicks=12000;findSteamArea();}
        if(steamTicks<=0)return;steamTicks--;if(age==1||age%200==0)findSteamArea();
        for(var p:l.getEntitiesOfClass(ServerPlayer.class,new AABB(worldPosition).inflate(8,6,8))){
            if(!steamArea.contains(p.blockPosition()))continue;if(age%20==0)Effects.near(l,Effects.Kind.SAUNA_PLAYER,p.position(),Vec3.ZERO,0,p.getId());
            if(!p.isInWater())p.addEffect(new MobEffectInstance(thaumcraft.registry.ModEffects.WARP_WARD,40,0,true,false));
            if(l.random.nextInt(200)==0)Warp.add(p,-1,WarpType.TEMPORARY);
            if(l.random.nextInt(100)==0&&p.getFoodData().getFoodLevel()>6)p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);
        }
        if(steamTicks%100==0)sync();
    }
    void findSteamArea(){
        steamArea.clear();ArrayDeque<BlockPos> open=new ArrayDeque<>();open.add(worldPosition.above());for(Direction d:Direction.Plane.HORIZONTAL)open.add(worldPosition.relative(d));
        while(!open.isEmpty()&&steamArea.size()<1200){BlockPos pos=open.removeFirst();if(steamArea.contains(pos)||Math.pow(pos.getX()-worldPosition.getX(),2)+Math.pow(pos.getZ()-worldPosition.getZ(),2)>=64||Math.abs(pos.getY()-worldPosition.getY())>5||!level.hasChunkAt(pos)||!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty())continue;boolean overWater=false;for(int y=-6;y<=6;y++)if(level.getFluidState(new BlockPos(pos.getX(),worldPosition.getY()+y,pos.getZ())).is(net.minecraft.tags.FluidTags.WATER)){overWater=true;break;}if(overWater&&pos.getY()-worldPosition.getY()>=2)continue;steamArea.add(pos);for(Direction d:Direction.values())open.add(pos.relative(d));}
    }
    void blast(ServerLevel l){
        if(master==null)return;
        // Port.4 output buffers are drained once, preserving products already in old saves.
        for(int slot=1;slot<=2;slot++){var old=items.extractItem(slot,Integer.MAX_VALUE,false);if(!old.isEmpty())blastEmit(old);}
        if(items.getStackInSlot(0).isEmpty()&&!blastQueue.isEmpty()){items.setStackInSlot(0,blastQueue.removeFirst());setChanged();}
        if(age%5==0)for(ItemEntity entity:l.getEntitiesOfClass(ItemEntity.class,new AABB(worldPosition.above(2)).inflate(.1,.5,.1)))if(acceptBlastInput(entity.getItem()))entity.discard();
        if(l.hasNeighborSignal(worldPosition)){progress=0;setChanged();return;}ItemStack in=items.getStackInSlot(0);var recipe=BlastRecipes.resolve(in);
        if(recipe==null){progress=0;processing=ItemStack.EMPTY;setChanged();return;}
        if(!ItemStack.isSameItemSameComponents(in,processing)){progress=0;processing=in.copyWithCount(1);setChanged();}
        ItemStack out=recipe.output();
        if(speedup>0)speedup--;else speedup=thaumcraft.vis.VisNet.drainVis(l,worldPosition,Aspect.FIRE,5);int bellows=Multiblock.bellows(this);
        if(age%5==0&&speedup<40)for(Direction d:Direction.Plane.HORIZONTAL){BlockPos at=worldPosition.relative(d,2);if(l.getBlockEntity(at) instanceof EssentiaTransport t&&t.getSuctionAmount(d.getOpposite())<128&&t.takeEssentia(Aspect.FIRE,1,d.getOpposite())==1){speedup+=600;break;}}
        setChanged();if(++progress<blastTime())return;items.extractItem(0,1,false);blastEmit(out.copy());progress=0;sync();
        if(!recipe.bonus().isEmpty()){int count=0;if(bellows==0){if(l.random.nextInt(4)==0)count=1;}else for(int i=0;i<bellows;i++)if(l.random.nextFloat()<.44F)count++;if(count>0)blastEmit(recipe.bonus().copyWithCount(count));}

    }
    boolean acceptBlastInput(ItemStack stack){if(!kind.equals("infernal_blast_furnace")||master==null||BlastRecipes.resolve(stack)==null)return false;var root=controller();if(root==this&&!master.equals(worldPosition))return false;if(root!=this)return root.acceptBlastInput(stack);var rest=items.insertItem(0,stack.copy(),false);if(!rest.isEmpty())blastQueue.add(rest.copy());setChanged();return true;}
    void blastEmit(ItemStack stack){var target=worldPosition.relative(facing(),2).above();var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,target,facing().getOpposite());if(handler!=null)stack=ItemHandlerHelper.insertItemStacked(handler,stack,false);if(!stack.isEmpty()){var out=new ItemEntity(level,worldPosition.getX()+.5+facing().getStepX()*1.66,worldPosition.getY()+1.4,worldPosition.getZ()+.5+facing().getStepZ()*1.66,stack);out.setDeltaMovement(facing().getStepX()*.075,.025,facing().getStepZ()*.075);level.addFreshEntity(out);}}
    void dropBlastQueue(){for(var stack:blastQueue)Containers.dropItemStack(level,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),stack);blastQueue.clear();}
    void pump(ServerLevel l){pumpState.tick(this,l);}
    void terraform(ServerLevel l){Reformer.tick(this,l);}
    public boolean use(Player p,InteractionHand hand){
        if(kind.equals("infernal_blast_furnace"))return true;
        var held=p.getItemInHand(hand);
        if(kind.equals("ethereal_wall")){if(!level.isClientSide()){if(p.isShiftKeyDown()&&held.isEmpty())original=null;sync();}return true;}
        if(kind.equals("sauna_stove")&&held.is(Items.WATER_BUCKET)){if(!level.isClientSide()&&water.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.SIMULATE)==1000){water.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);Effects.sound(level,Vec3.atCenterOf(worldPosition),net.minecraft.sounds.SoundEvents.GENERIC_SWIM,.33F,1+(level.random.nextFloat()-level.random.nextFloat())*.3F);if(!p.isCreative())p.setItemInHand(hand,new ItemStack(Items.BUCKET));sync();}return true;}
        if(Set.of("spinning_wheel","cutting_table","label_library").contains(kind)){
            if(p.isShiftKeyDown()){if(!level.isClientSide()){gemCut=(gemCut+1)%2;activate(p);}return true;}
            if(!level.isClientSide())p.openMenu(new SimpleMenuProvider((id,inv,who)->new MachineMenu(id,inv,this),Component.translatable("block.witchinggadgets."+kind)),buffer->{buffer.writeBlockPos(worldPosition);buffer.writeUtf(kind);});
            return true;
        }return false;
    }
    public void activate(Player p){if(kind.equals("cutting_table")){active=true;cutGem();}else if(kind.equals("label_library")){var out=MachineCrafting.preview(this);if(!out.isEmpty()&&output(out,1)){MachineCrafting.consume(this);produce(out,1);}}else if(!kind.equals("infernal_blast_furnace")){active=!active;sync();}}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.put("Items",items.serializeNBT(r));t.put("Water",water.writeToNBT(r,new CompoundTag()));t.putInt("Progress",progress);t.putInt("Steam",steamTicks);t.putInt("Cut",gemCut);t.putBoolean("Active",active);t.putInt("Amount",amount);CompoundTag stored=new CompoundTag();for(var aspect:reformerEssentia.getAspects())stored.putInt(aspect.name(),reformerEssentia.getAmount(aspect));t.put("ReformerEssentia",stored);if(essentia!=null)t.putString("Aspect",essentia.name());if(master!=null)t.putLong("Master",master.asLong());if(original!=null)t.put("Original",net.minecraft.nbt.NbtUtils.writeBlockState(original));t.putInt("Position",position);t.putInt("Speedup",speedup);if(!processing.isEmpty())t.put("Processing",processing.save(r));net.minecraft.nbt.ListTag queue=new net.minecraft.nbt.ListTag();for(var stack:blastQueue)queue.add(stack.save(r));t.put("BlastQueue",queue);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);items.deserializeNBT(r,t.getCompound("Items"));water.readFromNBT(r,t.getCompound("Water"));progress=t.getInt("Progress");steamTicks=t.getInt("Steam");gemCut=t.getInt("Cut");active=t.getBoolean("Active");amount=t.getInt("Amount");essentia=t.contains("Aspect")?Aspect.valueOf(t.getString("Aspect")):null;reformerEssentia=new AspectList();var stored=t.getCompound("ReformerEssentia");for(String key:stored.getAllKeys()){try{reformerEssentia.add(Aspect.valueOf(key),stored.getInt(key));}catch(IllegalArgumentException ignored){}}if(kind.equals("terrestrial_reformer")&&!t.contains("ReformerEssentia")&&essentia!=null&&amount>0)reformerEssentia.add(essentia,amount);master=t.contains("Master")?BlockPos.of(t.getLong("Master")):null;original=t.contains("Original")?net.minecraft.nbt.NbtUtils.readBlockState(r.lookupOrThrow(Registries.BLOCK),t.getCompound("Original")):null;position=t.getInt("Position");speedup=t.getInt("Speedup");processing=t.contains("Processing")?ItemStack.parseOptional(r,t.getCompound("Processing")):ItemStack.EMPTY;blastQueue.clear();for(var entry:t.getList("BlastQueue",net.minecraft.nbt.Tag.TAG_COMPOUND)){var stack=ItemStack.parseOptional(r,(CompoundTag)entry);if(!stack.isEmpty())blastQueue.add(stack);}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public boolean isConnectable(Direction f){return kind.equals("infernal_blast_furnace")?master!=null&&f!=null&&f.getAxis().isHorizontal():kind.equals("terrestrial_reformer")?f==Direction.DOWN:kind.equals("essentia_pump")&&f==facing();}
    @Override public boolean canInputFrom(Direction f){return kind.equals("infernal_blast_furnace")?isConnectable(f):kind.equals("terrestrial_reformer")&&f==Direction.DOWN;}
    @Override public boolean canOutputTo(Direction f){return kind.equals("essentia_pump")&&f==facing();}
    @Override public void setSuction(Aspect a,int n){}
    @Override public Aspect getSuctionType(Direction f){return kind.equals("infernal_blast_furnace")&&canInputFrom(f)?Aspect.FIRE:kind.equals("terrestrial_reformer")?requiredAspect():null;}
    @Override public int getSuctionAmount(Direction f){return kind.equals("infernal_blast_furnace")&&canInputFrom(f)&&controller().speedup<40||kind.equals("terrestrial_reformer")&&requiredAspect()!=null?128:0;}
    @Override public int takeEssentia(Aspect a,int n,Direction f){if(!canOutputTo(f)||a!=essentia)return 0;int taken=Math.min(n,amount);amount-=taken;if(amount==0)essentia=null;sync();return taken;}
    @Override public int addEssentia(Aspect a,int n,Direction f){if(kind.equals("infernal_blast_furnace")){if(!canInputFrom(f)||a!=Aspect.FIRE||n<=0||controller().speedup>=40)return 0;controller().speedup+=600;controller().sync();return 1;}if(!canInputFrom(f)||a==null||a!=requiredAspect()||n<=0)return 0;return n-addToContainer(a,n);}
    @Override public Aspect getEssentiaType(Direction f){return kind.equals("terrestrial_reformer")?null:essentia;}
    @Override public int getEssentiaAmount(Direction f){return kind.equals("terrestrial_reformer")?0:amount;}
    @Override public int getMinimumSuction(){return 0;}
    @Override public boolean renderExtendedTube(){return false;}
    private Aspect requiredAspect(){var focus=Reformer.focus(this);return focus==null?null:focus.aspect();}
    @Override public AspectList getAspects(){return kind.equals("terrestrial_reformer")?reformerEssentia.copy():new AspectList();}
    @Override public void setAspects(AspectList aspects){if(kind.equals("terrestrial_reformer")){reformerEssentia=aspects.copy();amount=reformerEssentia.getAmount(requiredAspect());sync();}}
    @Override public boolean doesContainerAccept(Aspect aspect){return kind.equals("terrestrial_reformer")&&aspect!=null&&aspect==requiredAspect();}
    @Override public int addToContainer(Aspect aspect,int count){if(!kind.equals("terrestrial_reformer")||aspect==null||count<=0)return count;reformerEssentia.add(aspect,count);amount=reformerEssentia.getAmount(requiredAspect());sync();return 0;}
    @Override public boolean takeFromContainer(Aspect aspect,int count){if(!kind.equals("terrestrial_reformer")||count<=0||reformerEssentia.getAmount(aspect)<count)return false;reformerEssentia.remove(aspect,count);amount=reformerEssentia.getAmount(requiredAspect());sync();return true;}
    @Override public boolean doesContainerContainAmount(Aspect aspect,int count){return kind.equals("terrestrial_reformer")&&reformerEssentia.getAmount(aspect)>=count;}
    @Override public int containerContains(Aspect aspect){return kind.equals("terrestrial_reformer")?reformerEssentia.getAmount(aspect):0;}
}

