package dev.loki.lovisual.render.engine.svg.backend;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import dev.loki.lovisual.render.engine.svg.model.SvgStyle;
import dev.loki.lovisual.render.engine.svg.model.SvgStyleUtil;
import dev.loki.lovisual.render.engine.svg.parser.SvgPathParser;
import dev.loki.lovisual.util.logging.DebugLog;

/**
 * Парсинг SVG → SvgDoc (viewBox + shapes). Вынесено из гиганта SvgMeshBackend.
 */
final class SvgParser {
    private SvgParser() {}

    static @Nullable SvgMeshBackend.SvgDoc parseSvg(InputStream in, String cacheKey) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            safeSet(f, "http://apache.org/xml/features/disallow-doctype-decl", true);
            safeSet(f, "http://xml.org/sax/features/external-general-entities", false);
            safeSet(f, "http://xml.org/sax/features/external-parameter-entities", false);
            safeSet(f, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(in);
            Element root = doc.getDocumentElement();
            if (root == null) return null;
            double[] vb = parseViewBox(root);
            SvgStyle rootStyle = SvgStyle.root();
            ArrayList<SvgMeshBackend.SvgShape> shapes = new ArrayList<>();
            walk(root, new AffineTransform(), rootStyle, shapes);
            return new SvgMeshBackend.SvgDoc(cacheKey, vb[0], vb[1], vb[2], vb[3], shapes);
        } catch (Exception e) {
            DebugLog.warn("[LoVisual][SVG] Parse failed: %s", e.getMessage());
            return null;
        }
    }

    static void safeSet(DocumentBuilderFactory f, String name, boolean value) {
        try { f.setFeature(name, value); } catch (Exception ignored) {}
    }

    static double[] parseViewBox(Element root) {
        String viewBox = root.getAttribute("viewBox");
        if (viewBox != null && !viewBox.isBlank()) {
            double[] vals = SvgStyleUtil.parseNumberList(viewBox);
            if (vals.length == 4 && vals[2] > 0.0 && vals[3] > 0.0) return vals;
        }
        double w = SvgStyleUtil.parseLength(root.getAttribute("width"), 24.0);
        double h = SvgStyleUtil.parseLength(root.getAttribute("height"), 24.0);
        if (w <= 0.0) w = 24.0;
        if (h <= 0.0) h = 24.0;
        return new double[]{0.0, 0.0, w, h};
    }

    static void walk(Element node, AffineTransform parentTransform, SvgStyle parentStyle, List<SvgMeshBackend.SvgShape> out) {
        String tag = node.getTagName();
        if (tag == null || tag.isBlank()) return;
        tag = tag.toLowerCase(Locale.ROOT);
        SvgStyle style = parentStyle.copy();
        SvgStyleUtil.applyElementStyle(style, node);
        if (!style.visible) return;
        AffineTransform transform = new AffineTransform(parentTransform);
        transform.concatenate(SvgStyleUtil.parseTransform(node.getAttribute("transform")));
        switch (tag) {
            case "svg", "g", "a", "symbol" -> {
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element childElement) walk(childElement, transform, style, out);
                }
            }
            case "defs" -> {}
            default -> {
                Path2D.Double path = buildPath(node, tag);
                if (path == null) return;
                int winding = "evenodd".equalsIgnoreCase(style.fillRule) ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO;
                path.setWindingRule(winding);
                out.add(new SvgMeshBackend.SvgShape(path, transform, style.copy(), winding));
            }
        }
    }

    static @Nullable Path2D.Double buildPath(Element element, String tag) {
        return switch (tag) {
            case "path" -> {
                String d = element.getAttribute("d");
                if (d == null || d.isBlank()) yield null;
                yield SvgPathParser.parse(d);
            }
            case "rect" -> buildRect(element);
            case "circle" -> buildCircle(element);
            case "ellipse" -> buildEllipse(element);
            case "line" -> buildLine(element);
            case "polyline" -> buildPoly(element, false);
            case "polygon" -> buildPoly(element, true);
            default -> null;
        };
    }

    static @Nullable Path2D.Double buildRect(Element e) {
        double x = SvgStyleUtil.parseLength(e.getAttribute("x"), 0.0);
        double y = SvgStyleUtil.parseLength(e.getAttribute("y"), 0.0);
        double w = SvgStyleUtil.parseLength(e.getAttribute("width"), 0.0);
        double h = SvgStyleUtil.parseLength(e.getAttribute("height"), 0.0);
        if (w <= 0.0 || h <= 0.0) return null;
        double rx = SvgStyleUtil.parseLength(e.getAttribute("rx"), -1.0);
        double ry = SvgStyleUtil.parseLength(e.getAttribute("ry"), -1.0);
        if (rx < 0.0 && ry >= 0.0) rx = ry;
        if (ry < 0.0 && rx >= 0.0) ry = rx;
        if (rx < 0.0) rx = 0.0;
        if (ry < 0.0) ry = 0.0;
        rx = Math.min(rx, w * 0.5);
        ry = Math.min(ry, h * 0.5);
        Path2D.Double out = new Path2D.Double();
        if (rx > 0.0 || ry > 0.0) out.append(new RoundRectangle2D.Double(x, y, w, h, rx * 2.0, ry * 2.0), false);
        else { out.moveTo(x, y); out.lineTo(x + w, y); out.lineTo(x + w, y + h); out.lineTo(x, y + h); out.closePath(); }
        return out;
    }

    static @Nullable Path2D.Double buildCircle(Element e) {
        double cx = SvgStyleUtil.parseLength(e.getAttribute("cx"), 0.0);
        double cy = SvgStyleUtil.parseLength(e.getAttribute("cy"), 0.0);
        double r = SvgStyleUtil.parseLength(e.getAttribute("r"), 0.0);
        if (r <= 0.0) return null;
        Path2D.Double out = new Path2D.Double();
        out.append(new Ellipse2D.Double(cx - r, cy - r, 2.0 * r, 2.0 * r), false);
        return out;
    }

    static @Nullable Path2D.Double buildEllipse(Element e) {
        double cx = SvgStyleUtil.parseLength(e.getAttribute("cx"), 0.0);
        double cy = SvgStyleUtil.parseLength(e.getAttribute("cy"), 0.0);
        double rx = SvgStyleUtil.parseLength(e.getAttribute("rx"), 0.0);
        double ry = SvgStyleUtil.parseLength(e.getAttribute("ry"), 0.0);
        if (rx <= 0.0 || ry <= 0.0) return null;
        Path2D.Double out = new Path2D.Double();
        out.append(new Ellipse2D.Double(cx - rx, cy - ry, 2.0 * rx, 2.0 * ry), false);
        return out;
    }

    static Path2D.Double buildLine(Element e) {
        double x1 = SvgStyleUtil.parseLength(e.getAttribute("x1"), 0.0);
        double y1 = SvgStyleUtil.parseLength(e.getAttribute("y1"), 0.0);
        double x2 = SvgStyleUtil.parseLength(e.getAttribute("x2"), 0.0);
        double y2 = SvgStyleUtil.parseLength(e.getAttribute("y2"), 0.0);
        Path2D.Double out = new Path2D.Double();
        out.moveTo(x1, y1); out.lineTo(x2, y2);
        return out;
    }

    static @Nullable Path2D.Double buildPoly(Element e, boolean closed) {
        String points = e.getAttribute("points");
        if (points == null || points.isBlank()) return null;
        double[] vals = SvgStyleUtil.parseNumberList(points);
        if (vals.length < 4) return null;
        Path2D.Double out = new Path2D.Double();
        out.moveTo(vals[0], vals[1]);
        for (int i = 2; i + 1 < vals.length; i += 2) out.lineTo(vals[i], vals[i + 1]);
        if (closed) out.closePath();
        return out;
    }
}
