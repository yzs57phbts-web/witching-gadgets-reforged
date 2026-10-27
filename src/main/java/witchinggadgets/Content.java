package witchinggadgets;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.capabilities.*;
import thaumcraft.platform.*;

public final class Content {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(WitchingGadgets.ID);
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(WitchingGadgets.ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,WitchingGadgets.ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS=DeferredRegister.create(Registries.ARMOR_MATERIAL,WitchingGadgets.ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,WitchingGadgets.ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,WitchingGadgets.ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,WitchingGadgets.ID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<DyeRecipe>> DYE_RECIPE=SERIALIZERS.register("dye",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(DyeRecipe::new));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<MachineMenu>> MACHINE_MENU=MENUS.register("machine",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(MachineMenu::new));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<StorageMenu>> STORAGE_MENU=MENUS.register("storage",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(StorageMenu::new));
    public static final Map<String,DeferredItem<? extends Item>> ITEM_MAP=new LinkedHashMap<>();
    public static final Map<String,DeferredBlock<? extends Block>> BLOCK_MAP=new LinkedHashMap<>();
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MachineEntity>> MACHINE=TILES.register("machine",()->BlockEntityType.Builder.of(MachineEntity::new,BLOCK_MAP.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static void init(IEventBus bus){
        for(var element:PortData.read("content")){
            var entry=element.getAsJsonObject();String id=PortData.str(entry,"id"),group=PortData.str(entry,"group");
            if(group.startsWith("Block")){
                var props=BlockBehaviour.Properties.of().strength(group.equals("BlockWoodenDevice")?2.5F:4,group.equals("BlockMetalDevice")?9:6).noOcclusion();
                if(group.equals("BlockMetalDevice")||group.equals("BlockStoneDevice"))props.requiresCorrectToolForDrops();
                if(id.equals("rose_vines"))props.dynamicShape().randomTicks().strength(0).noCollission().sound(net.minecraft.world.level.block.SoundType.VINE);
                if(id.equals("ethereal_wall"))props.dynamicShape();
                if(id.equals("infernal_blast_furnace"))props.lightLevel(state->state.getValue(FurnaceBlock.PART)==22?15:java.util.Set.of(10,12,14,16).contains(state.getValue(FurnaceBlock.PART))?13:0);
                if(id.equals("glimmer_of_light"))props.noCollission().lightLevel(s->15).noLootTable();
                var b=BLOCKS.register(id,()->id.equals("infernal_blast_furnace")?new FurnaceBlock(props):new MachineBlock(id,props));BLOCK_MAP.put(id,b);ITEM_MAP.put(id,ITEMS.registerSimpleBlockItem(id,b));
            }else{
                Item.Properties props=new Item.Properties();Function<Item.Properties,? extends Item> factory=p->new UtilityItem(id,p);
                if(group.equals("ItemCluster")||group.equals("ItemMaterial")||group.equals("BaseGear")){if(Set.of("arcane_abacus","gemcutters_tools","developed_iconograph","guiding_thread").contains(id))props.stacksTo(1);}
                else if(group.equals("ItemMagicFoodstuffs")){props.food(new FoodProperties.Builder().nutrition(id.equals("sweetwart")?2:6).saturationModifier(id.equals("sweetwart")?0.4F:id.equals("nethercake")?1.5F:1F).build());}
                else if(group.equals("ItemCloak")||group.equals("ItemKama")||group.equals("ItemMagicalBaubles")){props.stacksTo(1).rarity(Rarity.RARE);factory=p->new Gear(id,p);}
                else if(group.equals("ItemBag")){props.stacksTo(1).rarity(Rarity.RARE);factory=p->new BagItem(id,p);}
                else if(Set.of("primordial_helm","primordial_cuirass","primordial_greaves","primordial_boots").contains(id)){props=PrimordialArmor.properties(id,props);factory=p->new PrimordialArmor(id,p);}
                else if(id.startsWith("primordial_")&&!id.equals("primordial_gauntlet")){props.stacksTo(1).rarity(Rarity.EPIC);factory=p->new PrimordialItem(id,p);}
                else if(id.equals("infused_gem")){props.stacksTo(1);factory=GemItem::new;}
                else if(id.equals("primordial_gauntlet")){props.stacksTo(1).rarity(Rarity.EPIC).attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder().add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.withDefaultNamespace("base_attack_damage"),8,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.withDefaultNamespace("base_attack_speed"),-2.4,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build());}
                else if(id.equals("bewitched_robe")||id.equals("bewitched_leggings")){factory=p->new RobeItem(id,p);props=RobeItem.properties(id,props);}
                else if(id.equals("thaumium_shears")){props.durability(500);factory=ThaumiumShears::new;}
                else if(id.equals("crystal_capsule")){}
                else props.stacksTo(1).rarity(Rarity.UNCOMMON);
                ITEM_MAP.put(id,ITEMS.registerItem(id,factory,props));
            }
        }
        TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.witchinggadgets")).icon(()->new ItemStack(item("primordial_gauntlet"))).displayItems((p,o)->{
            ITEM_MAP.forEach((n,v)->{if(n.equals("infused_gem")){for(int cut=0;cut<2;cut++)for(var aspect:GemItem.PRIMALS)o.accept(GemItem.create(aspect,cut,0,0));return;}if(ReleaseContent.creative(n))o.accept(v.get());});
            o.accept(tc("native_cinnabar_cluster"));
        }).build());
        ARMOR_MATERIALS.register(bus);BLOCKS.register(bus);ITEMS.register(bus);TILES.register(bus);TABS.register(bus);MENUS.register(bus);SERIALIZERS.register(bus);
    }
    public static void capabilities(RegisterCapabilitiesEvent e){
        e.registerItem(Capabilities.FluidHandler.ITEM,(stack,context)->Capsule.handler(stack),item("crystal_capsule"));
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,MACHINE.get(),(be,side)->be.automation());
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,MACHINE.get(),(be,side)->be.kind.equals("sauna_stove")&&side!=net.minecraft.core.Direction.UP?be.water:null);
    }
    public static Item item(String name){return ITEM_MAP.get(name).get();}
    public static Block block(String name){return BLOCK_MAP.get(name).get();}
    public static ItemStack stack(String name){return new ItemStack(item(name));}
    public static Item tc(String name){return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft",name));}
}
