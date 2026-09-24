package dev.loki.lovisual.render.engine.svg.model;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

public final class SvgStyle {
    @Nullable public Integer fill;
    @Nullable public Integer stroke;
    public float strokeWidth;

    public float fillOpacity;

    public float strokeOpacity;

    public float groupOpacity;

    public int lineCap;

    public int lineJoin;

    public float miterLimit;

    public boolean visible;

    public String fillRule;

    public int currentColor;

    public static SvgStyle root() {

        SvgStyle s = new SvgStyle();
        s.fill = 0xFF000000;
        s.stroke = null;
        s.strokeWidth = 1.0f;
        s.fillOpacity = 1.0f;
        s.strokeOpacity = 1.0f;
        s.groupOpacity = 1.0f;
        s.lineCap = BasicStroke.CAP_BUTT;
        s.lineJoin = BasicStroke.JOIN_MITER;
        s.miterLimit = 4.0f;
        s.visible = true;
        s.fillRule = "nonzero";
        s.currentColor = 0xFF000000;
        return s;
    }

    public SvgStyle copy() {

        SvgStyle out = new SvgStyle();
        out.fill = fill;
        out.stroke = stroke;
        out.strokeWidth = strokeWidth;
        out.fillOpacity = fillOpacity;
        out.strokeOpacity = strokeOpacity;
        out.groupOpacity = groupOpacity;
        out.lineCap = lineCap;
        out.lineJoin = lineJoin;
        out.miterLimit = miterLimit;
        out.visible = visible;
        out.fillRule = fillRule;
        out.currentColor = currentColor;
        return out;
    }
}
