package dev.loki.lovisual.features.module.modules.visuals.scene.visual;

import com.mojang.blaze3d.textures.GpuTextureView;

final class ReimaginedDepthBindings {
    private final GpuTextureView main;
    private final GpuTextureView translucent;
    private final GpuTextureView itemEntity;
    private final GpuTextureView particles;
    private final GpuTextureView weather;
    private final GpuTextureView clouds;
    ReimaginedDepthBindings(GpuTextureView main, GpuTextureView translucent, GpuTextureView itemEntity, GpuTextureView particles, GpuTextureView weather, GpuTextureView clouds) {
        this.main = main; this.translucent = translucent; this.itemEntity = itemEntity; this.particles = particles; this.weather = weather; this.clouds = clouds;
    }
    boolean hasAnyDepth() { return primary() != null; }
    boolean hasMain() { return main != null; }
    boolean hasTranslucent() { return translucent != null; }
    boolean hasItemEntity() { return itemEntity != null; }
    boolean hasParticles() { return particles != null; }
    boolean hasWeather() { return weather != null; }
    boolean hasClouds() { return clouds != null; }
    GpuTextureView primary() {
        if (main != null) return main;
        if (translucent != null) return translucent;
        if (itemEntity != null) return itemEntity;
        if (particles != null) return particles;
        if (weather != null) return weather;
        return clouds;
    }
    GpuTextureView mainOr(GpuTextureView p) { return main != null ? main : firstOr(p); }
    GpuTextureView translucentOr(GpuTextureView p) { return translucent != null ? translucent : firstOr(p); }
    GpuTextureView itemEntityOr(GpuTextureView p) { return itemEntity != null ? itemEntity : firstOr(p); }
    GpuTextureView particlesOr(GpuTextureView p) { return particles != null ? particles : firstOr(p); }
    GpuTextureView weatherOr(GpuTextureView p) { return weather != null ? weather : firstOr(p); }
    GpuTextureView cloudsOr(GpuTextureView p) { return clouds != null ? clouds : firstOr(p); }
    private GpuTextureView firstOr(GpuTextureView placeholder) { GpuTextureView p = primary(); return p != null ? p : placeholder; }
}
