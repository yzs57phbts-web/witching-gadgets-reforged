package witchinggadgets;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(WitchingGadgets.ID)
public final class WitchingGadgets {
    public static final String ID="witchinggadgets";
    public static final Logger LOG=LogUtils.getLogger();
    public WitchingGadgets(IEventBus bus, ModContainer container) {
        Content.init(bus); Effects.init(bus); StatusEffects.init(bus);
        Recipes.init(); Research.init(); Aspects.init(); Events.init();
        bus.addListener(Content::capabilities);
        bus.addListener(AbilityPayload::register);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(()->{
            Gear.registerCurios(); Capsule.register(); Research.bindProxies(); Recipes.optional();TinkererCompat.register();
            LOG.info("Witching Gadgets 1.1.10 adaptation: {} items; Twilight Forest: {}; Witchery heads: deliberately omitted (no usable equivalent in Witchery 0.5.26)",Content.ITEM_MAP.size(),Compat.twilight());
        }));
    }
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ID,path.toLowerCase(java.util.Locale.ROOT));}
}
