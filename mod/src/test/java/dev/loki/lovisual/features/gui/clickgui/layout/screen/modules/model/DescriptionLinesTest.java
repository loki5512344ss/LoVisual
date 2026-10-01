package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptionLinesTest {
    /** One unit per character keeps the arithmetic obvious. */
    private static double width(String s) {
        return s.length();
    }

    @Test
    void wrapsOnWordBoundaries() {
        assertEquals(List.of("aaa bbb", "ccc"), DescriptionLines.wrap("aaa bbb ccc", 8, DescriptionLinesTest::width, 5));
    }

    @Test
    void oversizedWordKeepsItsOwnLine() {
        assertEquals(List.of("a", "bbbbbbbbbb", "c"), DescriptionLines.wrap("a bbbbbbbbbb c", 4, DescriptionLinesTest::width, 5));
    }

    @Test
    void overflowIsCutWithEllipsis() {
        List<String> lines = DescriptionLines.wrap("one two three four five six", 7, DescriptionLinesTest::width, 2);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith("..."));
        assertTrue(lines.get(1).length() <= 7);
    }

    @Test
    void blankInputGivesNoLines() {
        assertTrue(DescriptionLines.wrap("  ", 10, DescriptionLinesTest::width, 3).isEmpty());
        assertTrue(DescriptionLines.wrap(null, 10, DescriptionLinesTest::width, 3).isEmpty());
    }
}
