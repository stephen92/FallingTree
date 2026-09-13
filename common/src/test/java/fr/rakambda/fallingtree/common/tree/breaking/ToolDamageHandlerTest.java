package fr.rakambda.fallingtree.common.tree.breaking;

import fr.rakambda.fallingtree.common.config.enums.*;
import fr.rakambda.fallingtree.common.wrapper.IItemStack;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolDamageHandlerTest {
    @Test void repeatedSizeRejectionsDoNotSpamNormalConsole() {
        var events = new ArrayList<LogEvent>();
        var logger = (Logger) LogManager.getLogger(ToolDamageHandler.class);
        var oldLevel = logger.getLevel();
        var oldAdditive = logger.isAdditive();
        var appender = new AbstractAppender("test-size-rejections", null, null, true, Property.EMPTY_ARRAY) {
            public void append(LogEvent event) { events.add(event.toImmutable()); }
        };
        appender.start(); logger.addAppender(appender); logger.setLevel(Level.ALL); logger.setAdditive(false);
        try {
            for (int i = 0; i < 30; i++) {
                assertThrows(BreakTreeTooBigException.class, () -> handler(101, 0, MaxSizeAction.ABORT));
                assertThrows(BreakTreeTooSmallException.class, () -> handler(1, 2, MaxSizeAction.ABORT));
            }
            assertEquals(60, events.size(), "Keep diagnostics available at debug level");
            assertTrue(events.stream().noneMatch(e -> e.getLevel().isMoreSpecificThan(Level.INFO)),
                "Ordinary size rejections must not flood the INFO console");
        } finally {
            logger.removeAppender(appender); logger.setLevel(oldLevel); logger.setAdditive(oldAdditive); appender.stop();
        }
    }
    @Test void exactLimitAndCutModeStillWork() throws Exception {
        assertEquals(100, handler(100, 0, MaxSizeAction.ABORT).getMaxBreakCount());
        assertEquals(100, handler(101, 0, MaxSizeAction.CUT).getMaxBreakCount());
    }
    private ToolDamageHandler handler(int count, int min, MaxSizeAction action) throws Exception {
        return new ToolDamageHandler(mock(IItemStack.class), 1, DurabilityMode.NORMAL, count,
            min, 100, action, DamageRounding.ROUND_DOWN);
    }
}
