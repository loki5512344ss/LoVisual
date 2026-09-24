package dev.loki.lovisual.render.engine.svg.parser;

import java.awt.geom.Path2D;

/**
 * Parses an SVG path {@code d} attribute into a {@link Path2D.Double}. This class
 * owns the scan cursor and the current/start/last-control-point state and drives the
 * command dispatch loop; the individual command handlers live in
 * {@link SvgPathCommands} and {@link SvgPathCurves}, and arc-to-Bezier conversion in
 * {@link SvgArcGeometry}.
 */
public final class SvgPathParser {

    private final String data;
    private int index;
    private char command;

    double cx;
    double cy;
    double sx;
    double sy;
    double lastCubicX;
    double lastCubicY;
    double lastQuadX;
    double lastQuadY;
    char prevCmd;

    private SvgPathParser(String data) {
        this.data = data;
    }

    public static Path2D.Double parse(String data) {

        return new SvgPathParser(data).parseInternal();
    }

    private Path2D.Double parseInternal() {
        Path2D.Double out = new Path2D.Double(Path2D.WIND_NON_ZERO);

        while (true) {
            skipDelimiters();
            if (eof()) break;

            char c = data.charAt(index);
            if (SvgPathCommands.isCommand(c)) {
                command = c;
                index++;
            } else if (command == 0) {
                break;
            }

            boolean rel = Character.isLowerCase(command);
            switch (Character.toLowerCase(command)) {
                case 'm' -> SvgPathCommands.parseMove(this, out, rel);
                case 'z' -> {
                    out.closePath();
                    cx = sx;
                    cy = sy;
                    prevCmd = 'z';
                }
                case 'l' -> SvgPathCommands.parseLine(this, out, rel);
                case 'h' -> SvgPathCommands.parseHorizontal(this, out, rel);
                case 'v' -> SvgPathCommands.parseVertical(this, out, rel);
                case 'c' -> SvgPathCurves.parseCubic(this, out, rel);
                case 's' -> SvgPathCurves.parseSmoothCubic(this, out, rel);
                case 'q' -> SvgPathCurves.parseQuadratic(this, out, rel);
                case 't' -> SvgPathCurves.parseSmoothQuadratic(this, out, rel);
                case 'a' -> SvgPathCurves.parseArc(this, out, rel);
                default -> {
                    return out;
                }
            }
        }

        return out;
    }

    boolean hasNumber() {
        skipDelimiters();
        if (eof()) return false;
        char c = data.charAt(index);
        return c == '+' || c == '-' || c == '.' || Character.isDigit(c);
    }

    double readNumber() {
        skipDelimiters();
        if (eof()) return Double.NaN;

        int start = index;
        if (data.charAt(index) == '+' || data.charAt(index) == '-') index++;
        boolean hasDot = false;
        while (!eof()) {
            char c = data.charAt(index);
            if (Character.isDigit(c)) {
                index++;
                continue;
            }
            if (c == '.' && !hasDot) {
                hasDot = true;
                index++;
                continue;
            }
            break;
        }
        if (!eof() && (data.charAt(index) == 'e' || data.charAt(index) == 'E')) {
            int e = index + 1;
            if (e < data.length() && (data.charAt(e) == '+' || data.charAt(e) == '-')) e++;
            boolean hasExpDigits = false;
            while (e < data.length() && Character.isDigit(data.charAt(e))) {
                e++;
                hasExpDigits = true;
            }
            if (hasExpDigits) index = e;
        }
        if (start == index) return Double.NaN;
        try {
            return Double.parseDouble(data.substring(start, index));
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private void skipDelimiters() {
        while (!eof()) {
            char c = data.charAt(index);
            if (Character.isWhitespace(c) || c == ',') index++;
            else break;
        }
    }

    private boolean eof() {
        return index >= data.length();
    }
}
