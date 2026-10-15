package fr.rakambda.fallingtree.common.tree.breaking;

import fr.rakambda.fallingtree.common.FallingTreeCommon;
import fr.rakambda.fallingtree.common.config.enums.*;
import fr.rakambda.fallingtree.common.tree.*;
import fr.rakambda.fallingtree.common.wrapper.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompletedBreakTest {
    @Test void completionOnlyFollowsSuccessfulAllowedRemoval() throws Exception {
        var mod = mock(FallingTreeCommon.class, RETURNS_DEEP_STUBS);
        var player = mock(IPlayer.class, RETURNS_DEEP_STUBS);
        var level = mock(ILevel.class);
        var origin = mock(IBlockPos.class);
        var removed = mock(IBlockPos.class);
        var refused = mock(IBlockPos.class);
        var failed = mock(IBlockPos.class);
        var state = mock(IBlockState.class, RETURNS_DEEP_STUBS);
        var tree = new Tree(level, origin);
        tree.addPart(new TreePart(removed, TreePartType.LOG, 1, state, null));
        tree.addPart(new TreePart(refused, TreePartType.LOG, 2, state, null));
        tree.addPart(new TreePart(failed, TreePartType.LOG, 3, state, null));
        when(mod.getConfiguration().getTrees().getMaxSize()).thenReturn(100);
        when(mod.getConfiguration().getTrees().getBreakOrder()).thenReturn(BreakOrder.CLOSEST_FIRST);
        when(mod.getConfiguration().getTools().getDurabilityMode()).thenReturn(DurabilityMode.NORMAL);
        when(mod.getConfiguration().getTools().getDamageRounding()).thenReturn(DamageRounding.ROUND_DOWN);
        when(level.getBlockState(any())).thenReturn(state);
        when(mod.checkCanBreakBlock(level, removed, state, player)).thenReturn(true);
        when(mod.checkCanBreakBlock(level, failed, state, player)).thenReturn(true);
        when(level.removeBlock(removed, false)).thenReturn(true);
        new InstantaneousTreeBreakingHandler(mod, mock(LeafForceBreaker.class)).breakTree(false, player, tree);
        var completions = mockingDetails(mod).getInvocations().stream()
            .filter(call -> call.getMethod().getName().equals("onBlockBroken")).toList();
        assertEquals(1, completions.size(), "one successful extra log must emit one completion");
        assertSame(removed, completions.getFirst().getArgument(1));
        verify(level, never()).removeBlock(refused, false);
    }
}
