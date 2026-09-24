package dev.loki.lovisual.features.command.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import dev.loki.lovisual.features.command.impl.world.integration.XaeroWaypointCommand;

public class XaeroWaypointCommandTest {
    @Test
    void parsesAbsoluteAndCurrentCoordinates() {
        assertEquals(120, XaeroWaypointCommand.parseCoordinate("120", 7));
        assertEquals(-300, XaeroWaypointCommand.parseCoordinate("-300", 7));
        assertEquals(7, XaeroWaypointCommand.parseCoordinate("~", 7));
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertNull(XaeroWaypointCommand.parseCoordinate(null, 7));
        assertNull(XaeroWaypointCommand.parseCoordinate("", 7));
        assertNull(XaeroWaypointCommand.parseCoordinate("abc", 7));
        assertNull(XaeroWaypointCommand.parseCoordinate("~2", 7));
    }
}
