/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;

import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/**
 * Facade for mesh tessellation, gradient and path geometry.
 * Delegates to helpers in geometry/ and mesh/ subpackages; keeps same package for compatibility.
 */
public final class UiMeshGeometry {
    private UiMeshGeometry() {}
    // gradient / color
    public static void computeLinearGradientColors(float w,float h,int s,int e,float a,float o,int[] out){UiGradientGeometry.computeLinearGradientColors(w,h,s,e,a,o,out);}
    public static float clamp01(float v){return UiGradientGeometry.clamp01(v);}
    public static float packLiquidGlassPayload(float d,float s,float b){return UiGradientGeometry.packLiquidGlassPayload(d,s,b);}
    public static float normalizeDegrees(float a){return UiGradientGeometry.normalizeDegrees(a);}
    public static int mixArgb(int s,int e,float t){return UiGradientGeometry.mixArgb(s,e,t);}
    // rect / chamfer
    public static float clampRoundedRadius(float r,double w,double h){return UiRectGeometry.clampRoundedRadius(r,w,h);}
    public static float clampChamfer(double c,double w,double h){return UiRectGeometry.clampChamfer(c,w,h);}
    public static float clampChamferAxis(double c,double l){return UiRectGeometry.clampChamferAxis(c,l);}
    public static void normalizeCornerRadii(double w,double h,float tl,float tr,float br,float bl,float[] out){UiRectGeometry.normalizeCornerRadii(w,h,tl,tr,br,bl,out);}
    public static void normalizeChamfers(double w,double h,float tl,float tr,float br,float bl,float[] out){UiRectGeometry.normalizeChamfers(w,h,tl,tr,br,bl,out);}
    public static int buildNotchedRect(double[] o,double x,double y,double w,double h,double nw,double nd){return UiRectGeometry.buildNotchedRect(o,x,y,w,h,nw,nd);}
    // polygon / quad
    public static void appendColoredQuad(MeshBuilder m,double x,double y,double w,double h,int tl,int tr,int br,int bl){UiPolygonTessellator.appendColoredQuad(m,x,y,w,h,tl,tr,br,bl);}
    public static void appendPolygon(MeshBuilder m,double[] p,int c,int[] idx,int[] v,int argb){UiPolygonTessellator.appendPolygon(m,p,c,idx,v,argb);}
    public static void appendPolygonGradient(MeshBuilder m,double[] p,int c,int[] idx,int[] v,double x,double y,double w,double h,int s,int e,float a,float o){UiPolygonTessellator.appendPolygonGradient(m,p,c,idx,v,x,y,w,h,s,e,a,o);}
    // polyline / line
    public static void appendPolyline(MeshBuilder m,double[] p,int c,double t,boolean cl,int argb,boolean rc){UiPolylineGeometry.appendPolyline(m,p,c,t,cl,argb,rc);}
    public static void appendPolylineGradient(MeshBuilder m,double[] p,int c,double t,boolean cl,int s,int e,boolean rc){UiPolylineGeometry.appendPolylineGradient(m,p,c,t,cl,s,e,rc);}
    public static void appendPolylineLinearGradient(MeshBuilder m,double[] p,int c,double t,boolean cl,double x,double y,double w,double h,int s,int e,float a,float o,boolean rc){UiPolylineGeometry.appendPolylineLinearGradient(m,p,c,t,cl,x,y,w,h,s,e,a,o,rc);}
    // arc / anchor (delegated to gradient helper for circle/arc primitives)
    public static void roundedRectAnchor(double[] o,int off,double x,double y,double w,double h,double r,double tx,double ty){UiGradientGeometry.roundedRectAnchor(o,off,x,y,w,h,r,tx,ty);}
    public static int buildBezier(double[] o,double x1,double y1,double cx1,double cy1,double cx2,double cy2,double x2,double y2,int s){return UiGradientGeometry.buildBezier(o,x1,y1,cx1,cy1,cx2,cy2,x2,y2,s);}

    // rect shape builders (kept in facade: rect primitive)
    public static int buildRoundedProgressRect(double[] out,double[] work,double x,double y,double w,double h,double r,double clipX,boolean fromRight){
        int c=buildRoundedRectPolygon(work,x,y,w,h,r);
        if(c<3) return 0;
        return clipPolygonVertical(work,c,out,clipX,fromRight);
    }
    public static int buildRoundedRectPolygon(double[] out,double x,double y,double w,double h,double radius){
        double ww=Math.max(0.0,w); double hh=Math.max(0.0,h);
        if(ww<=0.0||hh<=0.0) return 0;
        double x2=x+ww; double y2=y+hh;
        double r=Math.max(0.0,Math.min(Math.min(ww,hh)*0.5,radius));
        int[] cnt={0};
        if(r<=0.001){add(out,out.length/2,cnt,x,y);add(out,out.length/2,cnt,x2,y);add(out,out.length/2,cnt,x2,y2);add(out,out.length/2,cnt,x,y2);return cnt[0];}
        int seg=Math.max(4,Math.min(10,(int)Math.ceil(r*1.35)));
        add(out,out.length/2,cnt,x+r,y);add(out,out.length/2,cnt,x2-r,y);
        addArc(out,out.length/2,cnt,x2-r,y+r,r,-90.0,0.0,seg);
        add(out,out.length/2,cnt,x2,y2-r);addArc(out,out.length/2,cnt,x2-r,y2-r,r,0.0,90.0,seg);
        add(out,out.length/2,cnt,x+r,y2);addArc(out,out.length/2,cnt,x+r,y2-r,r,90.0,180.0,seg);
        add(out,out.length/2,cnt,x,y+r);addArc(out,out.length/2,cnt,x+r,y+r,r,180.0,270.0,seg);
        return removeClosingDuplicate(out,cnt[0]);
    }
    private static int removeClosingDuplicate(double[] p,int c){
        if(p==null||c<2) return c;
        double fx=p[0],fy=p[1],lx=p[(c-1)*2],ly=p[(c-1)*2+1];
        if(Math.abs(fx-lx)<0.0001&&Math.abs(fy-ly)<0.0001) return c-1;
        return c;
    }
    private static void add(double[] o,int max,int[] cnt,double x,double y){
        if(o==null||cnt==null||cnt.length==0) return;
        int i=cnt[0]; if(i<0||i>=max) return;
        int b=i*2; if(b+1>=o.length) return;
        o[b]=x; o[b+1]=y; cnt[0]=i+1;
    }
    private static void addArc(double[] o,int max,int[] cnt,double cx,double cy,double r,double s,double e,int seg){
        int ss=Math.max(1,seg);
        for(int i=1;i<=ss;i++){double t=i/(double)ss; double a=Math.toRadians(s+(e-s)*t); add(o,max,cnt,cx+Math.cos(a)*r,cy+Math.sin(a)*r);}
    }
    private static int clipPolygonVertical(double[] src,int sc,double[] out,double clipX,boolean keepRight){
        int maxOut=out.length/2; int[] cnt={0};
        if(src==null||out==null||sc<3||maxOut<=0) return 0;
        for(int i=0;i<sc;i++){
            int prev=i==0?sc-1:i-1;
            double sx=src[prev*2],sy=src[prev*2+1],ex=src[i*2],ey=src[i*2+1];
            boolean sIn=keepRight?sx>=clipX-0.0001:sx<=clipX+0.0001;
            boolean eIn=keepRight?ex>=clipX-0.0001:ex<=clipX+0.0001;
            if(sIn&&eIn) add(out,maxOut,cnt,ex,ey);
            else if(sIn) addVerticalIntersection(out,maxOut,cnt,sx,sy,ex,ey,clipX);
            else if(eIn){addVerticalIntersection(out,maxOut,cnt,sx,sy,ex,ey,clipX);add(out,maxOut,cnt,ex,ey);}
        }
        return cnt[0];
    }
    private static void addVerticalIntersection(double[] o,int max,int[] c,double sx,double sy,double ex,double ey,double clipX){
        double dx=ex-sx;
        if(Math.abs(dx)<=0.000001){add(o,max,c,clipX,ey);return;}
        double t=(clipX-sx)/dx; t=Math.max(0.0,Math.min(1.0,t));
        add(o,max,c,clipX,sy+(ey-sy)*t);
    }
}
