package witchinggadgets;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Visual proof for the additional user reports; invoked only by EffectsAuditClient. */
final class Port4ReviewClient {
    static final BlockPos FURNACE=new BlockPos(8,64,0),ROSE=new BlockPos(0,64,-4);
    static int picks;
    static void tick(int now){var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(now==0){mc.setScreen(null);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),-2.5,64,.5,Set.of(),90,15);var l=p.serverLevel();
            for(BlockPos pos:BlockPos.betweenClosed(-1,64,-6,2,68,-3))l.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(ROSE.north(),Blocks.STONE_BRICKS.defaultBlockState());var m=EffectsAuditClient.place(l,ROSE,"rose_vines");m.progress=5;m.sync();m=EffectsAuditClient.place(l,ROSE.above(),"rose_vines");m.progress=5;m.sync();l.setBlockAndUpdate(ROSE.above().north(),Blocks.STONE_BRICKS.defaultBlockState());
            for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){var at=FURNACE.offset(x,y,z);BlockStateDummy.put(l,at,x,y,z);}
            if(!Multiblock.tryForm(l,FURNACE,p,new ItemStack(Content.tc("wand"))))throw new IllegalStateException("real furnace formation failed");var furnace=(MachineEntity)l.getBlockEntity(FURNACE);furnace.items.setStackInSlot(0,new ItemStack(Content.tc("native_iron_cluster"),8));furnace.sync();for(var research:thaumcraft.research.ResearchCategories.allResearch())thaumcraft.research.ResearchManager.complete(p,research.key(),false);
        });}
        if(now==25)EffectsAuditClient.shot("09-pump-small-connectors");
        if(now==40)server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(.5,64,-1.5,180,18));
        if(now>=55&&now<100){long start=System.nanoTime();for(int i=0;i<50;i++){mc.player.pick(6,0,false);picks++;}if(System.nanoTime()-start>500_000_000L)throw new IllegalStateException("Looking at mature vines still stalls the client");}
        if(now==80)EffectsAuditClient.shot("10-looking-at-mature-vines");
        if(now==110)server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(11,65.5,4.5,145,20));
        if(now==130)EffectsAuditClient.shot("11-furnace-active-original-textures");
        if(now==140)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();((MachineEntity)p.serverLevel().getBlockEntity(FURNACE)).use(p,InteractionHand.MAIN_HAND);});
        if(now==160){if(mc.screen instanceof MachineScreen)throw new IllegalStateException("Removed furnace GUI reopened");EffectsAuditClient.shot("12-furnace-without-gui");}
        if(now==175){mc.player.closeContainer();mc.setScreen(new thaumcraft.client.research.ResearchPageScreen(thaumcraft.research.ResearchCategories.getResearch("TERRAFORMFOCUS_MUSHROOM"),0));}
        if(now==195){String page=net.minecraft.client.resources.language.I18n.get("witchinggadgets_research_page.TERRAFORMFOCUS_MUSHROOM.1");if(!page.contains("Limus")||!page.contains("§a"))throw new IllegalStateException("Mushroom translation loses its aspect");EffectsAuditClient.shot("13-mushroom-full-russian-limus-and-slime");}
        if(now==210)mc.setScreen(new thaumcraft.client.research.ResearchPageScreen(thaumcraft.research.ResearchCategories.getResearch("TERRAFORMFOCUS_COLDTAIGA"),0));
        if(now==230)EffectsAuditClient.shot("14-taiga-full-russian-gelum");
        if(now==240)mc.setScreen(new thaumcraft.client.research.ResearchPageScreen(thaumcraft.research.ResearchCategories.getResearch("TERRAFORMFOCUS_HELL"),0));
        if(now==260)EffectsAuditClient.shot("15-nether-full-russian-tenebrae");
        if(now==270){mc.setScreen(null);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.addEffect(new MobEffectInstance(StatusEffects.KNOCKBACK,1000,1));p.addEffect(new MobEffectInstance(StatusEffects.DISSOLVE,1000,2));p.addEffect(new MobEffectInstance(StatusEffects.CINDER,1000,1));});}
        if(now==280)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(now==295)EffectsAuditClient.shot("16-three-original-status-icons");
        if(now==305){mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.removeAllEffects();top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).ifPresent(inv->inv.getStacksHandler("back").ifPresent(h->h.getStacks().setStackInSlot(0,Content.stack("mantle_of_the_raven"))));p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(6,68,7,180,22);});}
        if(now==330){if(CurioRenderer.glide(Content.stack("mantle_of_the_raven"),mc.player,1)<.9F)throw new IllegalStateException("raven cape does not lift in flight");EffectsAuditClient.shot("17-raven-cape-horizontal-flight");}
        if(now==345)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=false;p.onUpdateAbilities();p.connection.teleport(6,64,7,180,15);});
        if(now==375){if(CurioRenderer.glide(Content.stack("mantle_of_the_raven"),mc.player,1)>.1F)throw new IllegalStateException("raven cape stays horizontal after landing");EffectsAuditClient.shot("18-raven-cape-landed");}
        if(now==390){System.out.println("WG_PORT4_REVIEW_OK: "+picks+" mature-vine picks without stalls; real furnace formation without GUI; colored Russian Limus/Gelum/Tenebrae pages and slime recipes; original status icons; pump connectors; raven cape lifts and settles");mc.stop();}
    }
    static final class BlockStateDummy {
        static void put(net.minecraft.server.level.ServerLevel l,BlockPos pos,int x,int y,int z){var state=y<2?Multiblock.expectedBrick(y*9+(z+1)*3+x+1).defaultBlockState():x==0&&z==0?Blocks.LAVA.defaultBlockState():Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,x<0?Direction.EAST:x>0?Direction.WEST:z<0?Direction.SOUTH:Direction.NORTH);l.setBlockAndUpdate(pos,state);}
    }
}
