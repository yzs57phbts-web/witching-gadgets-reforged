package witchinggadgets;
import java.util.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.SimpleTier;
import net.minecraft.world.item.crafting.Ingredient;
public final class PrimordialItem extends SwordItem {
    public static final Tier TIER=new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,1500,8,6,25,()->Ingredient.of(Content.tc("void_metal_ingot")));
    public final String kind;
    public PrimordialItem(String kind,Properties p){super(TIER,p.attributes(SwordItem.createAttributes(TIER,kind.equals("primordial_claymore")?4:6,kind.equals("primordial_claymore")?-2.4F:-3F)));this.kind=kind;}
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack){var aspect=GemItem.PRIMALS[Math.floorMod(Data.get(stack).getInt("Affinity"),6)];return super.getName(stack).copy().append(net.minecraft.network.chat.Component.literal(" — ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)).append(net.minecraft.network.chat.Component.translatable("tc.aspect."+aspect.getTag()).withStyle(style->style.withColor(aspect.getColor())));}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<net.minecraft.network.chat.Component> text,TooltipFlag f){super.appendHoverText(s,c,text,f);text.add(net.minecraft.network.chat.Component.translatable("wg.tooltip.affinity",net.minecraft.network.chat.Component.translatable("tc.aspect."+GemItem.PRIMALS[Math.floorMod(Data.get(s).getInt("Affinity"),6)].getTag())));text.add(net.minecraft.network.chat.Component.translatable("wg.tooltip.affinity_controls",net.minecraft.network.chat.Component.keybind("key.witchinggadgets.ability"),net.minecraft.network.chat.Component.keybind("key.witchinggadgets.select")));}
    @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(!l.isClientSide()&&e.tickCount%40==0&&s.isDamaged())s.setDamageValue(s.getDamageValue()-1);}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var s=p.getItemInHand(hand);if(p.isShiftKeyDown()){if(!l.isClientSide())Data.integer(s,"Affinity",(Data.get(s).getInt("Affinity")+1)%6);return InteractionResultHolder.sidedSuccess(s,l.isClientSide());}return InteractionResultHolder.pass(s);}
    @Override public float getDestroySpeed(ItemStack s,BlockState state){return kind.equals("primordial_warhammer")&&state.is(BlockTags.MINEABLE_WITH_PICKAXE)||kind.equals("primordial_greataxe")&&(state.is(BlockTags.MINEABLE_WITH_AXE)||state.is(BlockTags.LEAVES))?8:super.getDestroySpeed(s,state);}
    @Override public boolean isCorrectToolForDrops(ItemStack s,BlockState state){return kind.equals("primordial_warhammer")&&state.is(BlockTags.MINEABLE_WITH_PICKAXE)||kind.equals("primordial_greataxe")&&state.is(BlockTags.MINEABLE_WITH_AXE);}
    private boolean areaMaterial(BlockState state){return kind.equals("primordial_warhammer")?state.is(BlockTags.MINEABLE_WITH_PICKAXE)||state.is(BlockTags.MINEABLE_WITH_SHOVEL)||state.is(BlockTags.ICE)||state.is(BlockTags.SNOW)||state.is(BlockTags.ANVIL)||state.getBlock() instanceof net.minecraft.world.level.block.HalfTransparentBlock:state.is(BlockTags.MINEABLE_WITH_AXE)||state.is(BlockTags.LEAVES)||state.getBlock() instanceof net.minecraft.world.level.block.BushBlock||state.getBlock() instanceof net.minecraft.world.level.block.VineBlock||state.getBlock() instanceof net.minecraft.world.level.block.CactusBlock;}
    @Override public boolean mineBlock(ItemStack s,Level l,BlockState state,BlockPos pos,LivingEntity e){
        boolean secondary=e.getPersistentData().getBoolean("WGAreaMining");
        if(!l.isClientSide()&&e instanceof net.minecraft.server.level.ServerPlayer p&&(!p.isShiftKeyDown()||kind.equals("primordial_greataxe"))&&(kind.equals("primordial_warhammer")||kind.equals("primordial_greataxe"))&&!p.getPersistentData().getBoolean("WGAreaMining")){
            var hit=p.pick(6,1,false);var axis=p.getPersistentData().contains("WGMiningPos")&&p.getPersistentData().getLong("WGMiningPos")==pos.asLong()?net.minecraft.core.Direction.from3DDataValue(p.getPersistentData().getInt("WGMiningFace")).getAxis():hit instanceof net.minecraft.world.phys.BlockHitResult b?b.getDirection().getAxis():net.minecraft.core.Direction.Axis.Y;p.getPersistentData().putBoolean("WGAreaMining",true);
            try{for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){if(a==0&&b==0)continue;BlockPos next=axis==net.minecraft.core.Direction.Axis.Y?pos.offset(a,0,b):axis==net.minecraft.core.Direction.Axis.X?pos.offset(0,a,b):pos.offset(a,b,0);var block=l.getBlockState(next);boolean suitable=areaMaterial(block);if(l.hasChunkAt(next)&&suitable&&block.getDestroySpeed(l,next)>=0)p.gameMode.destroyBlock(next);}}finally{p.getPersistentData().remove("WGAreaMining");}
        }if(!kind.equals("primordial_claymore")){if(!l.isClientSide()&&!secondary&&state.getDestroySpeed(l,pos)!=0)s.hurtAndBreak(1,e,EquipmentSlot.MAINHAND);return true;}return super.mineBlock(s,l,state,pos,e);
    }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack repair){return repair.is(Content.tc("void_metal_ingot"));}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity victim,LivingEntity attacker){if(!kind.equals("primordial_claymore"))stack.hurtAndBreak(1,attacker,EquipmentSlot.MAINHAND);return super.hurtEnemy(stack,victim,attacker);}
}
