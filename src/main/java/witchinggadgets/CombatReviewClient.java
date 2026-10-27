package witchinggadgets;

import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import static witchinggadgets.GameplayAuditClient.*;

/** Opt-in review in the disposable audit save; native hands, use packets and shield fallback. */
final class CombatReviewClient {
    static int shots;
    static void shot(String name){Screenshot.grab(mc().gameDirectory,"wg-port13-"+String.format("%02d",++shots)+"-"+name+".png",mc().getMainRenderTarget(),c->System.out.println("WG_PORT13_SCREENSHOT: "+name));}
    static void plan(){
        add("prepare disposable survival combat review",p->{p.closeContainer();EquipmentTests.clear(p);p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.setHealth(20);p.getFoodData().setFoodLevel(20);var level=p.serverLevel();level.setDayTime(1000);level.setWeatherParameters(100000,0,false,false);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);for(var pos:BlockPos.betweenClosed(-6,63,-6,6,69,6))level.setBlockAndUpdate(pos,pos.getY()==63?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());for(var e:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(30),e->e!=p))e.discard();p.connection.teleport(.5,64,.5,0,18);give(p,ItemStack.EMPTY);},()->{mc().setScreen(null);mc().options.pauseOnLostFocus=false;mc().options.hideGui=false;EquipmentAuditClient.release();},p->{});
        for(var arm:HumanoidArm.values()){
            add("prepare "+arm+" first person gauntlet",p->{p.setMainArm(arm);give(p,Content.stack("primordial_gauntlet"));},()->{mc().player.setMainArm(arm);mc().options.setCameraType(CameraType.FIRST_PERSON);},p->check(Math.abs(p.getAttributeValue(Attributes.ATTACK_SPEED)-1.6)<.001,"server gauntlet speed"));
            add("show "+arm+" gauntlet without green wisp",p->{},()->{check(Math.abs(mc().player.getAttributeValue(Attributes.ATTACK_SPEED)-1.6)<.001,"client gauntlet speed");shot("gauntlet-"+arm.name().toLowerCase()+"-first-person");},p->{});
            add("prepare third person "+arm+" gauntlet",p->{},()->mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT),p->{});
            add("show third person "+arm+" gauntlet",p->{},()->shot("gauntlet-"+arm.name().toLowerCase()+"-third-person"),p->{});
        }
        for(String kind:new String[]{"primordial_claymore","primordial_greataxe","primordial_warhammer"}){
            add("hold right click with "+kind,p->{p.setMainArm(HumanoidArm.RIGHT);var weapon=Content.stack(kind);Data.integer(weapon,"Affinity",1);give(p,weapon);},()->{mc().player.setMainArm(HumanoidArm.RIGHT);mc().options.setCameraType(CameraType.FIRST_PERSON);mc().options.keyUse.setDown(true);mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);},p->check(!p.isUsingItem()&&!p.isBlocking(),"native weapon use still blocks: "+kind));
            add("show unblocked "+kind,p->{},()->{check(!mc().player.isUsingItem()&&!mc().player.isBlocking(),"client still blocking: "+kind);shot(kind+"-right-click-no-block");EquipmentAuditClient.release();},p->{});
        }
        add("use ordinary shield with primordial weapon",p->{p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));p.inventoryMenu.sendAllDataToRemote();},()->{mc().options.keyUse.setDown(true);mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);mc().gameMode.useItem(mc().player,InteractionHand.OFF_HAND);},p->check(p.isBlocking()&&p.getUseItem().is(Items.SHIELD),"offhand shield failed through native use packets"));
        add("show normal offhand shield",p->{},()->{check(mc().player.isBlocking(),"client offhand shield absent");shot("ordinary-offhand-shield");EquipmentAuditClient.release();},p->{});
        add("finish combat review",p->System.out.println("WG_PORT13_NETWORK_OK: native use packets do not block with all three weapons; ordinary offhand shield works; gauntlet speed 1.6 synchronized"),()->{System.out.println("WG_PORT13_CLIENT_OK: "+shots+" screenshots");mc().stop();},p->{});
    }
}
