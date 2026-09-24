package dev.loki.lovisual.render.engine.svg.parser;

import java.awt.geom.Path2D;

/**
 * Linear SVG path commands for {@link SvgPathParser}: token classification plus
 * move/line/horizontal/vertical segment handlers. Each handler reads numbers via
 * the parser cursor and mutates its current/start point and previous-command state.
 * Curve and arc handlers live in {@link SvgPathCurves}. Package-private helper
 * (impl subfolder, {@code parser} package).
 */
final class SvgPathCommands {

    private SvgPathCommands() {
    }

    static boolean isCommand(char c) {
        return switch (c) {
            case 'M', 'm', 'Z', 'z', 'L', 'l', 'H', 'h', 'V', 'v',
                 'C', 'c', 'S', 's', 'Q', 'q', 'T', 't', 'A', 'a' -> true;
            default -> false;
        };
    }

    static void parseMove(SvgPathParser p, Path2D.Double out, boolean rel) {
        double x = p.readNumber();
        double y = p.readNumber();
        if (Double.isNaN(x) || Double.isNaN(y)) return;
        if (rel) {
            x += p.cx;
            y += p.cy;
        }
        out.moveTo(x, y);
        p.cx = p.sx = x;
        p.cy = p.sy = y;
        p.prevCmd = 'm';

        while (p.hasNumber()) {
            double lx = p.readNumber();
            double ly = p.readNumber();
            if (Double.isNaN(lx) || Double.isNaN(ly)) return;
            if (rel) {
                lx += p.cx;
                ly += p.cy;
            }
            out.lineTo(lx, ly);
            p.cx = lx;
            p.cy = ly;
            p.prevCmd = 'l';
        }
    }

    static void parseLine(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double x = p.readNumber();
            double y = p.readNumber();
            if (Double.isNaN(x) || Double.isNaN(y)) return;
            if (rel) {
                x += p.cx;
                y += p.cy;
            }
            out.lineTo(x, y);
            p.cx = x;
            p.cy = y;
            p.prevCmd = 'l';
        }
    }

    static void parseHorizontal(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double x = p.readNumber();
            if (Double.isNaN(x)) return;
            if (rel) x += p.cx;
            out.lineTo(x, p.cy);
            p.cx = x;
            p.prevCmd = 'h';
        }
    }

    static void parseVertical(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double y = p.readNumber();
            if (Double.isNaN(y)) return;
            if (rel) y += p.cy;
            out.lineTo(p.cx, y);
            p.cy = y;
            p.prevCmd = 'v';
        }
    }
}
