package witchinggadgets;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid=WitchingGadgets.ID,value=Dist.CLIENT)
public final class ClientWorldSmoke {
    static int ticks;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(Boolean.getBoolean("wg.ordinaryAudit")){GameplayAuditClient.tick();return;}
        if(Boolean.getBoolean("wg.pumpAudit")){PumpReviewClient.tick();return;}
        if(Boolean.getBoolean("wg.gameplayAudit")){GameplayAuditClient.tick();return;}
        if(Boolean.getBoolean("wg.furnaceAudit")){Port5AuditClient.tick();return;}
        if(Boolean.getBoolean("wg.effectsAudit")){EffectsAuditClient.tick();return;}
        if(Boolean.getBoolean("wg.visualAudit")){VisualAuditClient.tick();return;}
        if(!Boolean.getBoolean("wg.worldSmoke"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;var server=mc.getSingleplayerServer();
        if(!server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("wg-validation"))throw new IllegalStateException("Visual smoke test is restricted to wg-validation");
        if(ticks++==0){mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.setScreen(null);server.execute(()->{
            var l=server.overworld();l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);
            for(BlockPos p:BlockPos.betweenClosed(-10,63,-4,24,72,10))l.setBlockAndUpdate(p,p.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
            String[] names={"spinning_wheel","cutting_table","sauna_stove","terrestrial_reformer","essentia_pump","ageing_stone","label_library","ethereal_wall"};for(int i=0;i<names.length;i++){var p=new BlockPos(-5+i*3,64,0);l.setBlockAndUpdate(p,Content.block(names[i]).defaultBlockState().setValue(MachineBlock.FACING,Direction.SOUTH));if(l.getBlockEntity(p) instanceof MachineEntity m){if(m.kind.equals("spinning_wheel"))for(int slot=0;slot<4;slot++)m.items.setStackInSlot(slot,new ItemStack(Items.STRING,8));if(m.kind.equals("sauna_stove"))m.water.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,2000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);}}
            l.setBlockAndUpdate(new BlockPos(4,65,0),Content.block("focus_plains").defaultBlockState());var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(6,68,14,180,17);
            for(var entity:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-10,63,-4,24,73,10)))entity.discard();
        });}
        if(ticks==90)shot("wg-world.png");
        if(ticks==110){mc.options.hideGui=false;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.connection.teleport(-2,65,3,180,20);var m=(MachineEntity)server.overworld().getBlockEntity(new BlockPos(-2,64,0));m.use(p,net.minecraft.world.InteractionHand.MAIN_HAND);});}
        if(ticks==145){if(!(mc.screen instanceof MachineScreen))throw new IllegalStateException("Cutting table screen failed");shot("wg-cutting-table.png");}
        if(ticks==155){mc.options.hideGui=true;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,Content.stack("primordial_helm"));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,Content.stack("primordial_cuirass"));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,Content.stack("primordial_greaves"));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,Content.stack("primordial_boots"));top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).ifPresent(inv->inv.getStacksHandler("back").ifPresent(slots->slots.getStacks().setStackInSlot(0,Content.stack("mantle_of_the_raven"))));p.connection.teleport(0,65,7,180,8);});mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        if(ticks==200)shot("wg-primordial-raven.png");
        if(ticks==205)mc.setScreen(new ClientSmoke.Preview());
        if(ticks==215)shot("wg-items.png");
        if(ticks>=220){System.out.println("WG_WORLD_SMOKE: machine renderers, inventory screen, primordial armor and raven cloak rendered in dedicated validation save");mc.stop();}
    }
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->System.out.println("WG_SCREENSHOT: "+name));}
}
