package fr.rakambda.fallingtree.tests;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.EventPriority;
@Mod("fallingtree_test") public final class TestMod {
    static final Set<BlockPos> PROTECTED=new HashSet<>();
    public TestMod() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW,false,BlockEvent.BreakEvent.class,e->{if(PROTECTED.contains(e.getPos()))e.setCanceled(true);});
    }
}
