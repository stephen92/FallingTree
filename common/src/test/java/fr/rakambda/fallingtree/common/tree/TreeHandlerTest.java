package fr.rakambda.fallingtree.common.tree;
import fr.rakambda.fallingtree.common.FallingTreeCommon;
import fr.rakambda.fallingtree.common.config.enums.DurabilityMode;
import fr.rakambda.fallingtree.common.wrapper.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TreeHandlerTest {
    @Test void beforeBreakChecksToolWithoutScanningTree() {
        var mod = mock(FallingTreeCommon.class, RETURNS_DEEP_STUBS);
        var player = mock(IPlayer.class, RETURNS_DEEP_STUBS);
        when(mod.isPlayerInRightState(player)).thenReturn(true);
        when(mod.getConfiguration().getTools().getDurabilityMode()).thenReturn(DurabilityMode.NORMAL);
        assertFalse(new TreeHandler(mod).shouldCancelEvent(mock(ILevel.class), player, mock(IBlockPos.class), mock(IBlockState.class), null));
        verify(mod, never()).getTreeBuilder();
    }
    @Test void invalidPlayerDoesNotScanOrCancel() {
        var mod = mock(FallingTreeCommon.class);
        assertFalse(new TreeHandler(mod).shouldCancelEvent(mock(ILevel.class), mock(IPlayer.class), mock(IBlockPos.class), mock(IBlockState.class), null));
        verify(mod, never()).getTreeBuilder();
    }
    @Test void preservesLastDurabilityWithoutScanning() {
        var mod = mock(FallingTreeCommon.class, RETURNS_DEEP_STUBS);
        var player = mock(IPlayer.class, RETURNS_DEEP_STUBS);
        when(mod.isPlayerInRightState(player)).thenReturn(true);
        when(player.getMainHandItem().getDurability()).thenReturn(1);
        when(mod.getConfiguration().getTools().getDurabilityMode()).thenReturn(DurabilityMode.SAVE);
        assertTrue(new TreeHandler(mod).shouldCancelEvent(mock(ILevel.class), player, mock(IBlockPos.class), mock(IBlockState.class), null));
        verify(mod, never()).getTreeBuilder();
    }

}
