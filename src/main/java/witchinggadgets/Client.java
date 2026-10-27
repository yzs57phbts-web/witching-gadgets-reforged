package witchinggadgets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid=WitchingGadgets.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
@SuppressWarnings({"rawtypes","unchecked"})
public final class Client {
    static final KeyMapping ABILITY=new KeyMapping("key.witchinggadgets.ability",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.witchinggadgets");
    static final KeyMapping SELECT=new KeyMapping("key.witchinggadgets.select",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_N,"key.categories.witchinggadgets");
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(WitchingGadgets.id("item/capsule_shell")));for(String type:java.util.List.of("floor","wall"))for(int s=0;s<6;s++)e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(WitchingGadgets.id("block/rose_"+type+"_"+s)));}
    @SubscribeEvent public static void layers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers e){for(var skin:e.getSkins()){var renderer=(net.minecraft.client.renderer.entity.player.PlayerRenderer)e.getSkin(skin);renderer.addLayer(new ManoplaRendering.Layer(renderer));}}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{ClientEffects.init();net.minecraft.client.renderer.item.ItemProperties.register(Content.item("infused_gem"),WitchingGadgets.id("cut"),(s,l,p,seed)->Data.get(s).getInt("Cut"));});}
    @SubscribeEvent public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e){e.register(ABILITY);e.register(SELECT);}
    @SubscribeEvent public static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(Content.MACHINE_MENU.get(),MachineScreen::new);e.register(Content.STORAGE_MENU.get(),StorageScreen::new);}
    @SubscribeEvent public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e){
        e.register(Client::itemColor,Content.ITEM_MAP.values().stream().map(v->v.get()).toArray(net.minecraft.world.item.Item[]::new));
    }
    static int clothColor(net.minecraft.world.item.ItemStack s){
        if(s.has(net.minecraft.core.component.DataComponents.DYED_COLOR))return s.get(net.minecraft.core.component.DataComponents.DYED_COLOR).rgb();
        String n=Compat.name(s);if(s.getItem() instanceof RobeItem)return 6961280;if(s.getItem() instanceof BagItem)return switch(n){case "voidlinked_bag"->0x484848;case "enderlinked_bag"->0x2d4741;case "hungry_bag"->0x855172;default->0x8a74bd;};if(n.startsWith("spectral_"))return thaumcraft.aspect.Aspect.DARKNESS.getColor();if(n.contains("voluminous"))return thaumcraft.aspect.Aspect.VOID.getColor();if(n.equals("cloak")||n.equals("kama"))return 2041173;return 0xFFFFFF;
    }
    static int itemColor(net.minecraft.world.item.ItemStack s,int layer){
        var item=s.getItem();String n=Compat.name(s);
        if(item instanceof GemItem)return layer==0&&GemItem.aspect(s)!=null?0xFF000000|GemItem.aspect(s).getColor():-1;
        if(item instanceof PrimordialArmor&&layer==0||item instanceof PrimordialItem&&layer==1)return 0xFF000000|GemItem.PRIMALS[Math.floorMod(Data.get(s).getInt("Affinity"),6)].getColor();
        if(layer!=0)return -1;
        if(DyeRecipe.valid(s))return 0xFF000000|clothColor(s);
        return switch(n){case "cloth_of_spacious_folds"->0xFF000000|thaumcraft.aspect.Aspect.VOID.getColor();case "golden_fabric"->0xFF000000|thaumcraft.aspect.Aspect.GREED.getColor();case "bewitched_fleece"->0xFF000000|thaumcraft.aspect.Aspect.MAGIC.getColor();default->-1;};
    }
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Content.MACHINE.get(),MachineRenderer::new);for(var v:Content.ITEM_MAP.values())if(v.get() instanceof Gear)top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(v.get(),CurioRenderer::new);}
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){
        e.registerMobEffect(new net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions(){
            void icon(net.minecraft.world.effect.MobEffectInstance instance,net.minecraft.client.gui.GuiGraphics g,int x,int y,float alpha){g.setColor(1,1,1,alpha);g.blit(WitchingGadgets.id("textures/gui/potioneffects.png"),x,y,StatusEffects.icon(instance)*18,198,18,18,256,256);g.setColor(1,1,1,1);}
            @Override public boolean renderInventoryIcon(net.minecraft.world.effect.MobEffectInstance instance,net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen<?> screen,net.minecraft.client.gui.GuiGraphics g,int x,int y,int z){icon(instance,g,x+6,y+7,1);return true;}
            @Override public boolean renderGuiIcon(net.minecraft.world.effect.MobEffectInstance instance,net.minecraft.client.gui.Gui gui,net.minecraft.client.gui.GuiGraphics g,int x,int y,float z,float alpha){icon(instance,g,x+3,y+3,alpha);return true;}
        },StatusEffects.KNOCKBACK.get(),StatusEffects.DISSOLVE.get(),StatusEffects.CINDER.get());
        var itemRenderer=new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){private LegacyItemRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new LegacyItemRenderer();return renderer;}};
        e.registerItem(itemRenderer,Content.item("crystal_capsule"),Content.item("scan_camera"),Content.item("primordial_gauntlet"),Content.item("spinning_wheel"),Content.item("cutting_table"),Content.item("terrestrial_reformer"),Content.item("label_library"),Content.item("essentia_pump"),Content.item("focus_plains"),Content.item("focus_cold_taiga"),Content.item("focus_desert"),Content.item("focus_jungle"),Content.item("focus_hell"),Content.item("focus_taint"),Content.item("focus_mushroom"));
        for(String kind:java.util.List.of("primordial","advanced_robes")){
            var extension=new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
                private final java.util.Map<String,LegacyModels.Armor> models=new java.util.HashMap<>();
                @Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){int mask=Data.get(stack).getInt("mask");var model=models.computeIfAbsent(slot.name()+mask,k->new LegacyModels.Armor(kind,mask,slot==net.minecraft.world.entity.EquipmentSlot.FEET));((net.minecraft.client.model.HumanoidModel)original).copyPropertiesTo(model);model.prepare(stack,slot);if(kind.equals("advanced_robes")&&slot==net.minecraft.world.entity.EquipmentSlot.CHEST){var helmet=entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);if(!helmet.isEmpty()&&!(helmet.getItem() instanceof thaumcraft.item.gear.Goggles))model.head.visible=false;}return model;}
            };
            for(var v:Content.ITEM_MAP.values())if(kind.equals("primordial")?v.get() instanceof PrimordialArmor:v.get() instanceof RobeItem)e.registerItem(extension,v.get());
        }
    }
    @EventBusSubscriber(modid=WitchingGadgets.ID,value=Dist.CLIENT)
    public static final class Play {
        static boolean jumped,wasGround=true,gloveFocusDown;
        static void migrateSelectionKey(){var mc=Minecraft.getInstance();if(mc.player==null||SELECT.getKey().getValue()!=GLFW.GLFW_KEY_G)return;
            boolean conflict=java.util.Arrays.stream(mc.options.keyMappings).anyMatch(k->k!=SELECT&&k.getKey().equals(SELECT.getKey())&&(k.getName().equals("key.curios.open.desc")||k.getName().equals("key.thaumcraft.misc_toggle")));
            if(conflict)for(int code:new int[]{GLFW.GLFW_KEY_N,GLFW.GLFW_KEY_B,GLFW.GLFW_KEY_Y,GLFW.GLFW_KEY_J}){var key=InputConstants.Type.KEYSYM.getOrCreate(code);if(java.util.Arrays.stream(mc.options.keyMappings).noneMatch(k->k!=SELECT&&k.getKey().equals(key))){SELECT.setKey(key);KeyMapping.resetMapping();mc.options.save();mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("wg.key.select_migrated",SELECT.getTranslatedKeyMessage()),false);System.out.println("WG_PORT12_KEY_MIGRATED: "+key.getName());return;}}
        }
        @SubscribeEvent public static void gloveInput(net.neoforged.neoforge.client.event.ClientTickEvent.Pre e){migrateSelectionKey();var mc=Minecraft.getInstance();if(mc.player!=null&&mc.screen==null&&mc.player.getMainHandItem().is(Content.item("primordial_gauntlet"))&&thaumcraft.client.wand.WandKeyHandler.CHANGE_FOCUS.isDown())while(mc.options.keySwapOffhand.consumeClick()){} }
        @SubscribeEvent public static void fov(net.neoforged.neoforge.client.event.ComputeFovModifierEvent e){if(e.getPlayer().isShiftKeyDown()&&e.getPlayer().isUsingItem()&&e.getPlayer().getUseItem().getItem() instanceof net.minecraft.world.item.BowItem&&Gear.has(e.getPlayer(),"snipers_ring"))e.setNewFovModifier(.25F);}
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
            ClientEffects.tick();var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null)return;
            boolean glove=mc.player.getMainHandItem().is(Content.item("primordial_gauntlet")),focus=thaumcraft.client.wand.WandKeyHandler.CHANGE_FOCUS.isDown();
            if(glove&&focus&&!gloveFocusDown){gloveFocusDown=true;while(mc.options.keySwapOffhand.consumeClick()){}if(mc.player.isShiftKeyDown())net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(3));else mc.setScreen(new GemSelectionScreen(mc.player.getMainHandItem()));return;}if(!focus)gloveFocusDown=false;
            while(ABILITY.consumeClick()){var active=GearAbilities.entries(mc.player);if(active.size()>1){mc.setScreen(new GearAbilityScreen());return;}if(active.size()==1)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(0));}
            while(SELECT.consumeClick()){var held=mc.player.getMainHandItem();if(held.getItem() instanceof PrimordialItem||held.getItem() instanceof PrimordialArmor||held.is(Content.item("primordial_gauntlet")))net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(1));else{long count=GearAbilities.entries(mc.player).stream().filter(entry->entry.stack().getItem() instanceof PrimordialArmor).count();if(count>1){mc.setScreen(new GearAbilityScreen());return;}if(count==1)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(1));}}
            boolean pressed=mc.options.keyJump.isDown(),ground=mc.player.onGround();if(pressed&&!jumped&&!wasGround&&!ground&&!mc.player.getAbilities().flying&&Gear.has(mc.player,"double_jump_shoulders")&&!mc.player.getPersistentData().getBoolean("WGDoubleJump")){mc.player.getPersistentData().putBoolean("WGDoubleJump",true);EquipmentRules.extraJump(mc.player);net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AbilityPayload(2));}if(ground)mc.player.getPersistentData().remove("WGDoubleJump");jumped=pressed;wasGround=ground;
        }
    }
}
