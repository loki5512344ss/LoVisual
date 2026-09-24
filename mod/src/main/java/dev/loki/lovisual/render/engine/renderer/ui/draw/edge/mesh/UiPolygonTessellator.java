
package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;

import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

public final class UiPolygonTessellator {
    private UiPolygonTessellator() {}
    public static void appendColoredQuad(MeshBuilder mesh, double x, double y, double width, double height, int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        mesh.ensureQuadCapacity();
        int tlA=(cTopLeft>>>24)&0xFF; int tlR=(cTopLeft>>>16)&0xFF; int tlG=(cTopLeft>>>8)&0xFF; int tlB=cTopLeft&0xFF;
        int trA=(cTopRight>>>24)&0xFF; int trR=(cTopRight>>>16)&0xFF; int trG=(cTopRight>>>8)&0xFF; int trB=cTopRight&0xFF;
        int brA=(cBottomRight>>>24)&0xFF; int brR=(cBottomRight>>>16)&0xFF; int brG=(cBottomRight>>>8)&0xFF; int brB=cBottomRight&0xFF;
        int blA=(cBottomLeft>>>24)&0xFF; int blR=(cBottomLeft>>>16)&0xFF; int blG=(cBottomLeft>>>8)&0xFF; int blB=cBottomLeft&0xFF;
        int i1=mesh.vec2(x,y).color(tlR,tlG,tlB,tlA).next();
        int i2=mesh.vec2(x,y+height).color(blR,blG,blB,blA).next();
        int i3=mesh.vec2(x+width,y+height).color(brR,brG,brB,brA).next();
        int i4=mesh.vec2(x+width,y).color(trR,trG,trB,trA).next();
        mesh.quad(i1,i2,i3,i4);
    }
    static void emitUiPolygonTriangle(MeshBuilder mesh, int a, int b, int c, boolean reverseForUiCull) {
        if (reverseForUiCull) mesh.triangle(a,c,b); else mesh.triangle(a,b,c);
    }
    public static void appendPolygon(MeshBuilder mesh, double[] points, int pointCount, int[] indices, int[] vertexIds, int argb) {
        int safeCount=Math.min(pointCount, Math.min(points.length/2, Math.min(indices.length, vertexIds.length)));
        int triangles=safeCount-2;
        if (triangles<=0) return;
        mesh.ensureCapacity(safeCount, triangles*3);
        int a=(argb>>>24)&0xFF; int r=(argb>>>16)&0xFF; int g=(argb>>>8)&0xFF; int b=argb&0xFF;
        for (int i=0;i<safeCount;i++) { indices[i]=i; vertexIds[i]=mesh.vec2(points[i*2],points[i*2+1]).color(r,g,b,a).next(); }
        double area=polygonArea(points,safeCount);
        boolean reverseForUiCull=area>=0.0;
        if (isConvex(points,safeCount)) { for (int i=2;i<safeCount;i++) emitUiPolygonTriangle(mesh, vertexIds[0], vertexIds[i-1], vertexIds[i], reverseForUiCull); return; }
        int remaining=safeCount;
        double winding=area>=0.0?1.0:-1.0;
        int guard=safeCount*safeCount;
        while (remaining>2 && guard-->0) {
            boolean clipped=false;
            for (int i=0;i<remaining;i++) {
                int prev=i==0?remaining-1:i-1;
                int next=i+1==remaining?0:i+1;
                if (!isEar(points,indices,remaining,prev,i,next,winding)) continue;
                emitUiPolygonTriangle(mesh, vertexIds[indices[prev]], vertexIds[indices[i]], vertexIds[indices[next]], reverseForUiCull);
                for (int j=i;j<remaining-1;j++) indices[j]=indices[j+1];
                remaining--; clipped=true; break;
            }
            if (!clipped) { for (int i=2;i<remaining;i++) emitUiPolygonTriangle(mesh, vertexIds[indices[0]], vertexIds[indices[i-1]], vertexIds[indices[i]], reverseForUiCull); return; }
        }
    }
    public static void appendPolygonGradient(MeshBuilder mesh, double[] points, int pointCount, int[] indices, int[] vertexIds, double x, double y, double width, double height, int startArgb, int endArgb, float angleDeg, float offsetPx) {
        int safeCount=Math.min(pointCount, Math.min(points.length/2, Math.min(indices.length, vertexIds.length)));
        int triangles=safeCount-2;
        if (triangles<=0) return;
        mesh.ensureCapacity(safeCount, triangles*3);
        for (int i=0;i<safeCount;i++) {
            indices[i]=i;
            int argb=UiGradientGeometry.linearGradientColorAt(points[i*2]-x, points[i*2+1]-y, width, height, startArgb, endArgb, angleDeg, offsetPx);
            int a=(argb>>>24)&0xFF; int r=(argb>>>16)&0xFF; int g=(argb>>>8)&0xFF; int b=argb&0xFF;
            vertexIds[i]=mesh.vec2(points[i*2], points[i*2+1]).color(r,g,b,a).next();
        }
        double area=polygonArea(points,safeCount);
        boolean reverseForUiCull=area>=0.0;
        if (isConvex(points,safeCount)) { for (int i=2;i<safeCount;i++) emitUiPolygonTriangle(mesh, vertexIds[0], vertexIds[i-1], vertexIds[i], reverseForUiCull); return; }
        int remaining=safeCount;
        double winding=area>=0.0?1.0:-1.0;
        int guard=safeCount*safeCount;
        while (remaining>2 && guard-->0) {
            boolean clipped=false;
            for (int i=0;i<remaining;i++) {
                int prev=i==0?remaining-1:i-1;
                int next=i+1==remaining?0:i+1;
                if (!isEar(points,indices,remaining,prev,i,next,winding)) continue;
                emitUiPolygonTriangle(mesh, vertexIds[indices[prev]], vertexIds[indices[i]], vertexIds[indices[next]], reverseForUiCull);
                for (int j=i;j<remaining-1;j++) indices[j]=indices[j+1];
                remaining--; clipped=true; break;
            }
            if (!clipped) { for (int i=2;i<remaining;i++) emitUiPolygonTriangle(mesh, vertexIds[indices[0]], vertexIds[indices[i-1]], vertexIds[indices[i]], reverseForUiCull); return; }
        }
    }
    static boolean isConvex(double[] points, int count) {
        double sign=0.0;
        for (int i=0;i<count;i++) {
            int i0=i; int i1=(i+1)%count; int i2=(i+2)%count;
            double cross=cross(points,i0,i1,i2);
            if (Math.abs(cross)<=0.0001) continue;
            if (sign==0.0) sign=Math.signum(cross); else if (Math.signum(cross)!=sign) return false;
        }
        return true;
    }
    static boolean isEar(double[] points, int[] vertexIds, int remaining, int prev, int i, int next, double winding) {
        int p=vertexIds[prev]; int c=vertexIds[i]; int n=vertexIds[next];
        if (crossByVertex(points,p,c,n)*winding<=0.0001) return false;
        double ax=points[p*2]; double ay=points[p*2+1]; double bx=points[c*2]; double by=points[c*2+1]; double cx=points[n*2]; double cy=points[n*2+1];
        for (int j=0;j<remaining;j++) { if (j==prev||j==i||j==next) continue; int v=vertexIds[j]; if (pointInTriangle(points[v*2], points[v*2+1], ax,ay,bx,by,cx,cy,winding)) return false; }
        return true;
    }
    static double cross(double[] points, int i0, int i1, int i2) {
        double ax=points[i0*2]; double ay=points[i0*2+1]; double bx=points[i1*2]; double by=points[i1*2+1]; double cx=points[i2*2]; double cy=points[i2*2+1];
        return (bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
    }
    static double crossByVertex(double[] points, int i0, int i1, int i2) { return cross(points,i0,i1,i2); }
    static double polygonArea(double[] points, int count) {
        double area=0.0;
        for (int i=0;i<count;i++) { int j=i+1==count?0:i+1; area+=points[i*2]*points[j*2+1]-points[j*2]*points[i*2+1]; }
        return area*0.5;
    }
    static boolean pointInTriangle(double px,double py,double ax,double ay,double bx,double by,double cx,double cy,double winding) {
        double c1=(bx-ax)*(py-ay)-(by-ay)*(px-ax);
        double c2=(cx-bx)*(py-by)-(cy-by)*(px-bx);
        double c3=(ax-cx)*(py-cy)-(ay-cy)*(px-cx);
        return c1*winding>=0.0 && c2*winding>=0.0 && c3*winding>=0.0;
    }
}
