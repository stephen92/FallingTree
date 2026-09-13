package fr.rakambda.fallingtree.common.tree.builder;

import fr.rakambda.fallingtree.common.FallingTreeCommon;
import fr.rakambda.fallingtree.common.config.IConfiguration;
import fr.rakambda.fallingtree.common.config.real.TreeConfiguration;
import fr.rakambda.fallingtree.common.tree.TreePartType;
import fr.rakambda.fallingtree.common.tree.builder.position.BasicPositionFetcher;
import fr.rakambda.fallingtree.common.wrapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TreeBuilderTest {
    final FallingTreeCommon<?> mod = mock(FallingTreeCommon.class);
    final TreeConfiguration config = spy(new TreeConfiguration());
    final ILevel level = mock(ILevel.class);
    final IPlayer player = mock(IPlayer.class);
    final IBlock log = mock(IBlock.class), air = mock(IBlock.class);
    final IBlockState logState = mock(IBlockState.class), airState = mock(IBlockState.class);
    final Set<Pos> logs = new HashSet<>();
    final Pos origin = new Pos(0, 0, 0);
    int reads;
    @BeforeEach void setup() throws Exception {
        var instance = BasicPositionFetcher.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true); instance.set(null, null);
        var rootConfig = mock(IConfiguration.class);
        when(mod.getConfiguration()).thenReturn(rootConfig);
        when(rootConfig.getTrees()).thenReturn(config);
        config.setMinimumLeavesAroundRequired(0);
        when(logState.getBlock()).thenReturn(log);
        when(airState.getBlock()).thenReturn(air);
        when(mod.isLogBlock(log)).thenReturn(true);
        when(mod.getTreePart(log)).thenReturn(TreePartType.LOG);
        when(mod.getTreePart(air)).thenReturn(TreePartType.OTHER);
        when(level.getBlockState(any())).thenAnswer(call -> {
            reads++;
            return logs.contains(call.getArgument(0)) ? logState : airState;
        });
        logs.add(origin);
    }
    @Test void unrestrictedAdjacencyDoesNotReadSixExtraNeighborsPerCandidate() throws Exception {
        var tree = new TreeBuilder(mod).getTree(player, level, origin, logState, null).orElseThrow();
        assertEquals(1, tree.getSize());
        assertEquals(28, reads, "Only the position fetcher's parent and 27 neighborhood reads are needed");
    }
    @Test void connectedLogsStopAtExistingScanLimit() {
        for (int x = 0; x < 600; x++) logs.add(new Pos(x, 0, 0));
        assertThrows(TreeTooBigException.class, () -> new TreeBuilder(mod).getTree(player, level, origin, logState, null));
    }
    @Test void finiteRadiusBoundsConnectedLogs() throws Exception {
        for (int x = 0; x < 600; x++) logs.add(new Pos(x, 0, 0));
        config.setSearchAreaRadius(8);
        var tree = new TreeBuilder(mod).getTree(player, level, origin, logState, null).orElseThrow();
        assertEquals(9, tree.getSize());
        assertTrue(tree.getParts().stream().allMatch(part -> part.blockPos().getX() <= 8));
    }
    @Test void denseConnectedTreeContainsEachPositionOnce() throws Exception {
        for (int x = -3; x <= 3; x++) for (int y = 0; y < 4; y++)
            for (int z = -3; z <= 3; z++) logs.add(new Pos(x, y, z));
        var tree = new TreeBuilder(mod).getTree(player, level, origin, logState, null).orElseThrow();
        assertEquals(logs.size(), tree.getSize());
        assertEquals(logs.size(), tree.getParts().size());
    }

    @Test void configuredAdjacencyStillRejectsDisallowedNeighbors() throws Exception {
        doReturn(Set.of(log)).when(config).getAllowedAdjacentBlockBlocks(mod);
        doReturn(Set.of(log)).when(config).getAllAllowedAdjacentBlockBlocks(mod);
        when(mod.translate(anyString())).thenReturn(mock(IComponent.class, RETURNS_SELF));
        assertTrue(new TreeBuilder(mod).getTree(player, level, origin, logState, null).isEmpty());
        assertTrue(reads > 0);
    }
    @Test void configuredAdjacencyAllowsMatchingNeighbors() throws Exception {
        doReturn(Set.of(log, air)).when(config).getAllowedAdjacentBlockBlocks(mod);
        doReturn(Set.of(log, air)).when(config).getAllAllowedAdjacentBlockBlocks(mod);
        assertEquals(1, new TreeBuilder(mod).getTree(player, level, origin, logState, null).orElseThrow().getSize());
        assertTrue(reads > 28);
    }
    record Pos(int x, int y, int z) implements IBlockPos {
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public Object getRaw() { return this; }
        public IBlockPos immutable() { return this; }
        public IBlockPos offset(int dx, int dy, int dz) { return new Pos(x+dx,y+dy,z+dz); }
        public IBlockPos relative(DirectionCompat d) {
            return switch(d) {
                case UP -> offset(0,1,0); case DOWN -> offset(0,-1,0);
                case EAST -> offset(1,0,0); case WEST -> offset(-1,0,0);
                case NORTH -> offset(0,0,-1); case SOUTH -> offset(0,0,1);
            };
        }
        public Stream<IBlockPos> betweenClosedStream(IBlockPos a, IBlockPos b) {
            var result = new ArrayList<IBlockPos>();
            for (int xx = Math.min(a.getX(), b.getX()); xx <= Math.max(a.getX(), b.getX()); xx++)
                for (int yy = Math.min(a.getY(), b.getY()); yy <= Math.max(a.getY(), b.getY()); yy++)
                    for (int zz = Math.min(a.getZ(), b.getZ()); zz <= Math.max(a.getZ(), b.getZ()); zz++)
                        result.add(new Pos(xx,yy,zz));
            return result.stream();
        }
    }
}
