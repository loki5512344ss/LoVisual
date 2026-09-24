
package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;

import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

public final class UiPolylineGeometry {
    private UiPolylineGeometry() {}
    public static void appendPolyline(MeshBuilder mesh, double[] points, int pointCount, double thickness, boolean closed, int argb, boolean roundCapsAndJoins) {
        int safeCount=Math.min(pointCount, points.length/2);
        int segments=closed?safeCount:safeCount-1;
        if (segments<=0) return;
        mesh.ensureCapacity(segments*4, segments*6);
        for (int i=0;i<segments;i++) {
            int next=i+1>=safeCount?0:i+1;
            appendThickSegment(mesh, points[i*2], points[i*2+1], points[next*2], points[next*2+1], thickness, argb);
        }
        if (!roundCapsAndJoins) return;
        double radius=thickness*0.5;
        if (closed) { for (int i=0;i<safeCount;i++) appendDisk(mesh, points[i*2], points[i*2+1], radius, 10, argb); return; }
        appendDisk(mesh, points[0], points[1], radius, 10, argb);
        for (int i=1;i<safeCount-1;i++) appendDisk(mesh, points[i*2], points[i*2+1], radius, 10, argb);
        appendDisk(mesh, points[(safeCount-1)*2], points[(safeCount-1)*2+1], radius, 10, argb);
    }
    public static void appendPolylineGradient(MeshBuilder mesh, double[] points, int pointCount, double thickness, boolean closed, int startArgb, int endArgb, boolean roundCapsAndJoins) {
        int safeCount=Math.min(pointCount, points.length/2);
        int segments=closed?safeCount:safeCount-1;
        if (segments<=0) return;
        mesh.ensureCapacity(segments*4, segments*6);
        for (int i=0;i<segments;i++) {
            int next=i+1>=safeCount?0:i+1;
            float t0=segments<=1?0.0f:(float)i/(float)segments;
            float t1=segments<=1?1.0f:(float)(i+1)/(float)segments;
            appendThickSegmentGradient(mesh, points[i*2], points[i*2+1], points[next*2], points[next*2+1], thickness, UiGradientGeometry.mixArgb(startArgb,endArgb,UiGradientGeometry.clamp01(t0)), UiGradientGeometry.mixArgb(startArgb,endArgb,UiGradientGeometry.clamp01(t1)));
        }
        if (!roundCapsAndJoins) return;
        double radius=thickness*0.5;
        for (int i=0;i<safeCount;i++) { float t=safeCount<=1?0.0f:(float)i/(float)(safeCount-1); appendDisk(mesh, points[i*2], points[i*2+1], radius, 10, UiGradientGeometry.mixArgb(startArgb,endArgb,UiGradientGeometry.clamp01(t))); }
    }
    public static void appendPolylineLinearGradient(MeshBuilder mesh, double[] points, int pointCount, double thickness, boolean closed, double x, double y, double width, double height, int startArgb, int endArgb, float angleDeg, float offsetPx, boolean roundCapsAndJoins) {
        int safeCount=Math.min(pointCount, points.length/2);
        int segments=closed?safeCount:safeCount-1;
        if (segments<=0) return;
        mesh.ensureCapacity(segments*4, segments*6);
        for (int i=0;i<segments;i++) {
            int next=i+1>=safeCount?0:i+1;
            int c0=UiGradientGeometry.linearGradientColorAt(points[i*2]-x, points[i*2+1]-y, width, height, startArgb, endArgb, angleDeg, offsetPx);
            int c1=UiGradientGeometry.linearGradientColorAt(points[next*2]-x, points[next*2+1]-y, width, height, startArgb, endArgb, angleDeg, offsetPx);
            appendThickSegmentGradient(mesh, points[i*2], points[i*2+1], points[next*2], points[next*2+1], thickness, c0, c1);
        }
        if (!roundCapsAndJoins) return;
        double radius=thickness*0.5;
        for (int i=0;i<safeCount;i++) { int c=UiGradientGeometry.linearGradientColorAt(points[i*2]-x, points[i*2+1]-y, width, height, startArgb, endArgb, angleDeg, offsetPx); appendDisk(mesh, points[i*2], points[i*2+1], radius, 10, c); }
    }
    static void appendThickSegment(MeshBuilder mesh, double x1, double y1, double x2, double y2, double thickness, int argb) {
        double dx=x2-x1; double dy=y2-y1; double len=Math.sqrt(dx*dx+dy*dy);
        if (len<=0.0001) return;
        double half=thickness*0.5; double nx=-dy/len*half; double ny=dx/len*half;
        int a=(argb>>>24)&0xFF; int r=(argb>>>16)&0xFF; int g=(argb>>>8)&0xFF; int b=argb&0xFF;
        int i1=mesh.vec2(x1-nx,y1-ny).color(r,g,b,a).next();
        int i2=mesh.vec2(x1+nx,y1+ny).color(r,g,b,a).next();
        int i3=mesh.vec2(x2+nx,y2+ny).color(r,g,b,a).next();
        int i4=mesh.vec2(x2-nx,y2-ny).color(r,g,b,a).next();
        mesh.quad(i1,i2,i3,i4);
    }
    static void appendThickSegmentGradient(MeshBuilder mesh, double x1, double y1, double x2, double y2, double thickness, int startArgb, int endArgb) {
        double dx=x2-x1; double dy=y2-y1; double len=Math.sqrt(dx*dx+dy*dy);
        if (len<=0.0001) return;
        double half=thickness*0.5; double nx=-dy/len*half; double ny=dx/len*half;
        int sa=(startArgb>>>24)&0xFF; int sr=(startArgb>>>16)&0xFF; int sg=(startArgb>>>8)&0xFF; int sb=startArgb&0xFF;
        int ea=(endArgb>>>24)&0xFF; int er=(endArgb>>>16)&0xFF; int eg=(endArgb>>>8)&0xFF; int eb=endArgb&0xFF;
        int i1=mesh.vec2(x1-nx,y1-ny).color(sr,sg,sb,sa).next();
        int i2=mesh.vec2(x1+nx,y1+ny).color(sr,sg,sb,sa).next();
        int i3=mesh.vec2(x2+nx,y2+ny).color(er,eg,eb,ea).next();
        int i4=mesh.vec2(x2-nx,y2-ny).color(er,eg,eb,ea).next();
        mesh.quad(i1,i2,i3,i4);
    }
    static void appendDisk(MeshBuilder mesh, double cx, double cy, double radius, int segments, int argb) {
        if (radius<=0.0) return;
        int safeSegments=Math.max(6, Math.min(24, segments));
        mesh.ensureCapacity(safeSegments+1, safeSegments*3);
        int a=(argb>>>24)&0xFF; int r=(argb>>>16)&0xFF; int g=(argb>>>8)&0xFF; int b=argb&0xFF;
        int center=mesh.vec2(cx,cy).color(r,g,b,a).next();
        int first=-1; int prev=-1;
        for (int i=0;i<safeSegments;i++) {
            double angle=Math.PI*2.0*i/safeSegments;
            int vertex=mesh.vec2(cx+Math.cos(angle)*radius, cy+Math.sin(angle)*radius).color(r,g,b,a).next();
            if (first<0) first=vertex; else mesh.triangle(center,prev,vertex);
            prev=vertex;
        }
        if (first>=0 && prev>=0) mesh.triangle(center,prev,first);
    }
}
