package fr.rakambda.fallingtree.tests;
import com.mojang.authlib.GameProfile;
import fr.rakambda.fallingtree.neoforge.FallingTree;
import fr.rakambda.fallingtree.common.config.enums.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("fallingtree_test") @PrefixGameTestTemplate(false)
public final class TreeTests {
    private void tree(GameTestHelper h,BlockPos base) {
        for(BlockPos pos:BlockPos.betweenClosed(base.offset(-2,0,-2),base.offset(2,6,2)))h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(base.below(),Blocks.DIRT.defaultBlockState());
        for(int y=0;y<3;y++)h.getLevel().setBlockAndUpdate(base.above(y),Blocks.OAK_LOG.defaultBlockState());
        h.getLevel().setBlockAndUpdate(base.above(3),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(LeavesBlock.PERSISTENT,false));
    }
    @GameTest(template="empty",timeoutTicks=200)
    public void nativeTreeDamageSneakToggleAndProtection(GameTestHelper h) throws Exception {
        var level=h.getLevel(); var config=FallingTree.getMod().getOwnConfiguration();
        config.getTrees().setBreakMode(BreakMode.INSTANTANEOUS); config.getTrees().setLeavesBreaking(false);
        config.getTrees().setMinimumLeavesAroundRequired(1); config.getTrees().setSearchAreaRadius(-1);
        config.setSneakMode(SneakMode.SNEAK_DISABLE); config.getTools().setDamageMultiplicand(1);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TreeTester"));
        player.setGameMode(GameType.SURVIVAL);
        var axe=new ItemStack(Items.DIAMOND_AXE); player.setItemInHand(InteractionHand.MAIN_HAND,axe);
        var base=h.absolutePos(new BlockPos(2,2,2)); tree(h,base);
        TestMod.COMPLETED.clear();
        player.gameMode.destroyBlock(base);
        h.assertTrue(TestMod.COMPLETED.size()==2 && new java.util.HashSet<>(TestMod.COMPLETED).size()==2,
            "each successfully felled extra log emits exactly one completion");
        h.assertTrue(level.getBlockState(base).isAir() && level.getBlockState(base.above()).isAir() && level.getBlockState(base.above(2)).isAir(),"native block break removes entire tree");
        // Preserve the shared fork's accounting: two additional logs, minus its vanilla
        // credit, plus the ordinary hit-block use. This port does not rebalance tool cost.
        h.assertTrue(axe.getDamageValue()==2,"existing three-log tree durability is preserved, actual="+axe.getDamageValue());
        int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(5)).stream().filter(e->e.getItem().is(Items.OAK_LOG)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(drops==3,"three logs produce exactly three drops, actual="+drops);
        base=base.offset(8,0,0); tree(h,base); player.setShiftKeyDown(true); player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        player.gameMode.destroyBlock(base);
        h.assertTrue(level.getBlockState(base).isAir() && level.getBlockState(base.above()).is(Blocks.OAK_LOG),"sneaking performs ordinary single break");
        player.setShiftKeyDown(false); player.setPose(net.minecraft.world.entity.Pose.STANDING); base=base.offset(8,0,0); tree(h,base);
        level.getServer().getCommands().getDispatcher().execute("fallingtree toggle",player.createCommandSourceStack());
        player.gameMode.destroyBlock(base);
        h.assertTrue(level.getBlockState(base.above()).is(Blocks.OAK_LOG),"toggle bypasses tree breaking");
        level.getServer().getCommands().getDispatcher().execute("fallingtree toggle",player.createCommandSourceStack());
        base=base.offset(8,0,0); tree(h,base); TestMod.PROTECTED.add(base);
        try { player.gameMode.destroyBlock(base); h.assertTrue(level.getBlockState(base).is(Blocks.OAK_LOG) && level.getBlockState(base.above()).is(Blocks.OAK_LOG),"canceled origin leaves entire tree"); }
        finally { TestMod.PROTECTED.clear(); }
        base=base.offset(8,0,0); tree(h,base); TestMod.PROTECTED.add(base.above());
        try { player.gameMode.destroyBlock(base); h.assertTrue(level.getBlockState(base.above()).is(Blocks.OAK_LOG),"additional log protection event respected"); }
        finally { TestMod.PROTECTED.clear(); }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public void fallingModesAndShiftDownUseNativeEntities(GameTestHelper h) {
        var level=h.getLevel();var config=FallingTree.getMod().getOwnConfiguration();
        config.getTrees().setLeavesBreaking(false);config.getTrees().setMinimumLeavesAroundRequired(1);config.getTrees().setSearchAreaRadius(-1);config.setSneakMode(SneakMode.SNEAK_DISABLE);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FallTester"));player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_AXE));
        var base=h.absolutePos(new BlockPos(2,2,2));
        for(var mode:new BreakMode[]{BreakMode.FALL_ITEM,BreakMode.FALL_BLOCK,BreakMode.FALL_ALL_BLOCK}) {
            config.getTrees().setBreakMode(mode);config.getTrees().setLeavesBreaking(mode==BreakMode.FALL_ALL_BLOCK);tree(h,base);TestMod.COMPLETED.clear();player.gameMode.destroyBlock(base);
            h.assertTrue(TestMod.COMPLETED.contains(base.above()) && TestMod.COMPLETED.contains(base.above(2))
                && !TestMod.COMPLETED.contains(base), mode+" notifies extra logs without duplicating origin");
            h.assertTrue(level.getBlockState(base).isAir()&&level.getBlockState(base.above()).isAir()&&level.getBlockState(base.above(2)).isAir(),mode+" removes logs");
            var entities=level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,new AABB(base).inflate(5));
            var logEntities=entities.stream().filter(e->e.getBlockState().is(Blocks.OAK_LOG)).toList();
            h.assertTrue(logEntities.size()==2,mode+" creates native entities for extra logs, actual="+logEntities.size());
            h.assertTrue(logEntities.stream().allMatch(e->e.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getBoolean("CancelDrop")== (mode==BreakMode.FALL_ITEM)),mode+" entity item-drop behavior preserved");
            if(mode==BreakMode.FALL_ALL_BLOCK)h.assertTrue(entities.stream().anyMatch(e->e.getBlockState().is(Blocks.OAK_LEAVES))&&level.getBlockState(base.above(3)).isAir(),"FALL_ALL_BLOCK creates native leaf entity");
            entities.forEach(net.minecraft.world.entity.Entity::discard);
        }
        config.getTrees().setBreakMode(BreakMode.SHIFT_DOWN);tree(h,base);player.gameMode.destroyBlock(base);
        h.assertTrue(level.getBlockState(base).is(Blocks.OAK_LOG)&&level.getBlockState(base.above()).is(Blocks.OAK_LOG)&&level.getBlockState(base.above(2)).isAir(),"shift-down removes top log and retains origin");
        config.getTrees().setBreakMode(BreakMode.INSTANTANEOUS);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public void nativeConfigurationPacketRoundTripsAllModes(GameTestHelper h) {
        for(var mode:BreakMode.values()) {
            var packet=new fr.rakambda.fallingtree.common.network.ConfigurationPacket(true,2.75,true,mode);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var codec=fr.rakambda.fallingtree.neoforge.network.FallingTreeConfigPacket.CODEC;
                codec.encode(buffer,new fr.rakambda.fallingtree.neoforge.network.FallingTreeConfigPacket(packet));
                var decoded=codec.decode(buffer).getPacket();
                h.assertTrue(decoded.equals(packet)&&buffer.readableBytes()==0,"native configuration packet round trip "+mode);
            } finally {buffer.release();}
        }
        h.succeed();
    }
}
