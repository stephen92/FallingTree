package fr.rakambda.fallingtree.neoforge.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Noncancelable completion of an extra tree block; BreakEvent remains the permission check. */
public class FallingTreeBlockBrokenEvent extends BlockEvent {
    private final Player player;
    public FallingTreeBlockBrokenEvent(Level level, BlockPos pos, BlockState state, Player player) {
        super(level, pos, state);
        this.player = player;
    }
    public Player getPlayer() { return player; }
}
