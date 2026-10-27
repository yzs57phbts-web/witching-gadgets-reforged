package witchinggadgets;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Close-up regression gallery, explicitly enabled only in the disposable audit world. */
public final class VisualAuditClient {
    static int ticks;
    record View(String file,double x,double y,double z,float yaw,float pitch,String held){}
    static final List<View> VIEWS=List.of(
        new View("01-cutting-empty-top",.5,66,1.8,180,60,""),
        new View("02-cutting-phials",3.5,64.1,2.5,180,25,""),
        new View("03-cutting-essence",6.5,64.1,2.5,180,25,""),
        new View("04-terraformer",9.5,63.7,2.9,180,12,""),
        new View("05-essentia-pump",12.5,63.6,2.5,180,15,""),
        new View("06-label-library",15.5,64.1,2.5,180,35,""),
        new View("07-stone-extruder",18.5,64.1,2.5,180,25,""),
        new View("08-sauna",21.5,64.1,2.5,180,40,""),
        new View("09-focus-types",10,66,-.5,180,35,""),
        new View("10-held-ethereal-wall",.5,65,5,180,0,"ethereal_wall"),
        new View("11-held-terraformer",.5,65,5,180,0,"terrestrial_reformer"),
        new View("12-held-camera",.5,65,5,180,0,"scan_camera"),
        new View("13-held-gauntlet",.5,65,5,180,0,"primordial_gauntlet"),
        new View("14-held-table",.5,65,5,180,0,"cutting_table")
    );
    public static void tick(){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null||mc.level==null||server==null||mc.getOverlay()!=null)return;
        if(!server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("wg-visual-audit"))throw new IllegalStateException("Visual audit restricted to wg-visual-audit");
        if(ticks++==0){mc.setScreen(null);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.setCameraType(CameraType.FIRST_PERSON);validate();server.execute(()->{
            var l=server.overworld();l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(BlockPos pos:BlockPos.betweenClosed(-5,63,-9,32,71,8))l.setBlockAndUpdate(pos,pos.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
            String[] names={"cutting_table","cutting_table","cutting_table","terrestrial_reformer","essentia_pump","label_library","stone_extruder","sauna_stove","ethereal_wall"};
            for(int i=0;i<names.length;i++)l.setBlockAndUpdate(new BlockPos(i*3,64,0),Content.block(names[i]).defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH));
            for(int col=1;col<=2;col++){var m=(MachineEntity)l.getBlockEntity(new BlockPos(col*3,64,0));m.items.setStackInSlot(0,new ItemStack(Items.DIAMOND));for(int slot=1;slot<=3;slot++){var s=new ItemStack(Content.tc(col==1?"essentia_phial":"crystallized_essence"));thaumcraft.aspect.AspectKeys.ASPECTS.set(s,new thaumcraft.aspect.AspectList().add(thaumcraft.aspect.Aspect.ORDER,8));m.items.setStackInSlot(slot,s);}m.sync();}
            String[] focus={"plains","cold_taiga","desert","jungle","hell","taint","mushroom"};for(int i=0;i<focus.length;i++)l.setBlockAndUpdate(new BlockPos(i*3,64,-5),Content.block("focus_"+focus[i]).defaultBlockState());l.setBlockAndUpdate(new BlockPos(9,65,0),Content.block("focus_plains").defaultBlockState());
            var root=new BlockPos(28,64,0);for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){var pos=root.offset(x,y,z);l.setBlockAndUpdate(pos,Content.block("infernal_blast_furnace").defaultBlockState());var m=(MachineEntity)l.getBlockEntity(pos);m.master=root;m.position=y*9+(z+1)*3+x+1;m.original=Multiblock.expectedBrick(m.position).defaultBlockState();m.sync();}
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();for(var slot:EquipmentSlot.values())if(slot.isArmor())p.setItemSlot(slot,ItemStack.EMPTY);top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).ifPresent(inv->{for(var handler:inv.getCurios().values())for(int s=0;s<handler.getStacks().getSlots();s++)handler.getStacks().setStackInSlot(s,ItemStack.EMPTY);});
        });}
        if(ticks<40)return;int phase=(ticks-40)/30,offset=(ticks-40)%30;
        if(phase<VIEWS.size()){
            var view=VIEWS.get(phase);mc.options.hideGui=view.held.isEmpty();if(offset==0)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,view.held.isEmpty()?ItemStack.EMPTY:Content.stack(view.held));p.connection.teleport(view.x,view.y,view.z,view.yaw,view.pitch);});
            if(offset==23)shot(view.file);
        }else if(phase==VIEWS.size()){
            if(offset==0)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,Content.stack(switch(slot){case HEAD->"primordial_helm";case CHEST->"primordial_cuirass";case LEGS->"primordial_greaves";default->"primordial_boots";}));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.connection.teleport(6,64,5,0,0);});
            mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);if(offset==23)shot("15-primordial-front");
        }else if(phase==VIEWS.size()+1){
            if(offset==0)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.CHEST,Content.stack("bewitched_robe"));p.setItemSlot(EquipmentSlot.LEGS,Content.stack("bewitched_leggings"));p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);});if(offset==23)shot("16-bewitched-robes");
        }else if(phase==VIEWS.size()+2){if(offset==0)mc.setScreen(new ClientSmoke.Preview());if(offset==23)shot("17-release-items-ru");}
        else if(phase==VIEWS.size()+3){if(offset==0){mc.setScreen(null);mc.options.setCameraType(CameraType.FIRST_PERSON);server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(31,66.5,5,145,25));}if(offset==23)shot("18-infernal-furnace");}
        else if(phase==VIEWS.size()+4){if(offset==0){mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();for(var slot:EquipmentSlot.values())if(slot.isArmor())p.setItemSlot(slot,ItemStack.EMPTY);top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).ifPresent(inv->{for(String slot:List.of("hands","shoulders"))inv.getStacksHandler(slot).ifPresent(h->h.getStacks().setStackInSlot(0,Content.stack(slot.equals("hands")?"wolven_vambraces":"double_jump_shoulders")));});p.connection.teleport(6,64,5,0,0);});}if(offset==23)shot("19-shoulders-vambraces");}
        else{Port3Audit.tick(phase-VIEWS.size()-5,offset);}
    }
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"wg-audit-"+name+".png",mc.getMainRenderTarget(),m->System.out.println("WG_AUDIT_SCREENSHOT: "+name));}
    static void validate(){
        var obj=WgObjModel.get(WitchingGadgets.id("models/terraformer.obj"));for(String name:List.of("main_01","crystals_02","focus_03","focus_crystal_04"))if(obj.quadCount(name)==0)throw new IllegalStateException(name);
        if(LegacyModels.machine("cutting_table").getAsJsonArray("cubes").size()!=13)throw new IllegalStateException("Conditional table geometry leaked into static model");
        var mc=Minecraft.getInstance();for(var e:Content.ITEM_MAP.entrySet()){var s=new ItemStack(e.getValue().get());var model=mc.getItemRenderer().getModel(s,null,null,0);if(model==mc.getModelManager().getMissingModel()||model.getParticleIcon().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing "+e.getKey());if(s.getItem() instanceof BlockItem&&model.getTransforms().getTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).scale.length()>1.4F)throw new IllegalStateException("Oversized held block "+e.getKey());}
        for(String name:List.of("snow_generator","obsidian_plate","obsidian_decoration","magic_tile_lock","sarcophagus","wall_mirror","void_walkway"))if(ReleaseContent.creative(name))throw new IllegalStateException("Prototype in gallery "+name);
    }
}

