package witchinggadgets;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import thaumcraft.crafting.InfusionCrafting;
import thaumcraft.block.InfusionStabiliser;
import thaumcraft.infusion.*;

/** The original abacus inspects the current altar layout, including an idle altar. */
final class InfusionAnalysis {
    record Warning(String key,BlockPos pos){}
    record Report(int stability,List<Warning> warnings,InfusionCrafting.Plan recipe){}
    static boolean stabilizer(Level level,BlockPos pos){var block=level.getBlockState(pos).getBlock();return block instanceof AbstractSkullBlock||block instanceof InfusionStabiliser s&&s.canStabiliseInfusion(level,pos);}
    static Report inspect(Player player,BlockPos matrix){
        var level=player.level();var pedestals=new ArrayList<BlockPos>();var stabilizers=new ArrayList<BlockPos>();var warnings=new ArrayList<Warning>();
        var components=new ArrayList<ItemStack>();
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
            if(x==0&&z==0)continue;boolean found=false;
            for(int y=-10;y<=5;y++){
                var pos=matrix.offset(x,y,z);if(!level.hasChunkAt(pos))continue;
                if(!found&&level.getBlockEntity(pos) instanceof PedestalBlockEntity){
                    if(y>=0)warnings.add(new Warning("pedestalHeight",pos));
                    else if(Math.abs(x)<=8&&Math.abs(z)<=8){pedestals.add(pos);found=true;}
                    else warnings.add(new Warning("pedestalPos",pos));
                }else if(stabilizer(level,pos))stabilizers.add(pos);
            }
        }
        int symmetry=0;
        for(var pos:pedestals){
            var pedestal=(PedestalBlockEntity)level.getBlockEntity(pos);boolean items=!pedestal.getStack().isEmpty();symmetry+=2+(items?1:0);
            if(items)components.add(pedestal.getStack().copy());
            var other=new BlockPos(2*matrix.getX()-pos.getX(),pos.getY(),2*matrix.getZ()-pos.getZ());
            if(level.hasChunkAt(other)&&level.getBlockEntity(other) instanceof PedestalBlockEntity paired){symmetry-=2;if(!paired.getStack().isEmpty()){if(items)symmetry--;else warnings.add(new Warning("noPartnerItem",pos));}}
            else warnings.add(new Warning("noPartnerPedestal",pos));
        }
        float stabilizerSymmetry=0;
        for(var pos:stabilizers){
            stabilizerSymmetry+=.1F;var other=new BlockPos(2*matrix.getX()-pos.getX(),pos.getY(),2*matrix.getZ()-pos.getZ());
            if(level.hasChunkAt(other)&&stabilizer(level,other))stabilizerSymmetry-=.2F;else warnings.add(new Warning("noPartnerStabilizer",pos));
        }
        InfusionCrafting.Plan recipe=null;
        if(level.getBlockEntity(matrix.below(2)) instanceof PedestalBlockEntity center&&!center.getStack().isEmpty()){
            recipe=InfusionCrafting.findRecipe(components,center.getStack(),player);
            if(recipe==null)recipe=InfusionCrafting.findEnchantment(level,components,center.getStack(),player);
        }
        return new Report(-(int)(symmetry+stabilizerSymmetry),List.copyOf(warnings),recipe);
    }
    static void display(Player player,BlockPos pos){
        var report=inspect(player,pos);var text=AbacusReport.header("wg.abacus.matrix_header",pos);
        text.append(Component.translatable("wg.chat.infusionInfo.stabilityTotal",report.stability()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        if(report.recipe()!=null){
            text.append("\n").append(Component.translatable("wg.chat.infusionInfo.instability",report.recipe().instability()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            // Native plans already include the current enchantment/runic cost multiplier.
            if(report.recipe().type()!=InfusionCrafting.TYPE_RECIPE){
                for(var a:report.recipe().aspects().getAspects())text.append("\n").append(AbacusReport.aspect(a,report.recipe().aspects().getAmount(a)));
            }
        }
        for(var warning:report.warnings())text.append("\n").append(Component.translatable("wg.chat.infusionWarning."+warning.key(),warning.pos().getX(),warning.pos().getY(),warning.pos().getZ()).withStyle(net.minecraft.ChatFormatting.RED));
        player.displayClientMessage(AbacusReport.finish(text),false);
    }
}
