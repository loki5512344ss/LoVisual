package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext;

import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.LuaTables;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api.PoseApi;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime.LuaSandbox;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.addBillboardVertices;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.applyOpacity;

/**
 * Lua-глобал {@code particleManager}: {@code particleManager:addParticle{ x,y,z, vx,vy,vz, lifetime, size, color,
 * alpha, gravity, drag, tick = function(p) ... end }}. Координаты x,y,z заданы в системе текущей позы руки
 * (после M:), частица живёт в мире. Колбэк tick получает таблицу p и может менять x,y,z,vx,vy,vz,size,alpha,color.
 */
public final class HandParticles {

    private static final Logger LOG = LoggerFactory.getLogger(HandParticles.class);
    private static final int MAX = 256;

    private static final class P {
        float x, y, z, px, py, pz, vx, vy, vz, size, alpha, gravity, drag;
        int age, life, color;
        LuaValue tick;
    }

    private final List<P> list = new ArrayList<>();
    private final LuaSandbox sandbox;
    private final PoseApi pose;

    public HandParticles(LuaSandbox sandbox, PoseApi pose) {
        this.sandbox = sandbox;
        this.pose = pose;
    }

    public void clear() {
        list.clear();
    }

    public LuaTable table() {
        LuaTable t = new LuaTable();
        LuaTables.put(t, "addParticle", a -> {
            if (list.size() < MAX && a.arg(2).istable()) spawn(a.arg(2));
            return LuaValue.NIL;
        });
        return t;
    }

    private void spawn(LuaValue o) {
        Matrix4f m = pose.matrix();
        Minecraft mc = Minecraft.getInstance();
        if (m == null || mc.gameRenderer == null || mc.gameRenderer.mainCamera() == null) return;
        Vector3f local = m.transformPosition(num(o, "x", 0), num(o, "y", 0), num(o, "z", 0), new Vector3f());
        Vector3f world = new Quaternionf(RenderState.cameraRotation).transform(local);
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        P p = new P();
        p.x = p.px = (float) cam.x + world.x;
        p.y = p.py = (float) cam.y + world.y;
        p.z = p.pz = (float) cam.z + world.z;
        Vector3f v = new Quaternionf(RenderState.cameraRotation).transform(new Vector3f(num(o, "vx", 0), num(o, "vy", 0), num(o, "vz", 0)));
        p.vx = v.x;
        p.vy = v.y;
        p.vz = v.z;
        p.life = Math.max(1, Math.min(200, (int) num(o, "lifetime", 20)));
        p.size = num(o, "size", 0.05f);
        p.alpha = num(o, "alpha", 1f);
        p.gravity = num(o, "gravity", 0f);
        p.drag = num(o, "drag", 0.98f);
        LuaValue c = o.get("color");
        p.color = c.isnil() ? 0xFFFFFFFF : (int) (long) c.todouble();
        LuaValue tick = o.get("tick");
        p.tick = tick.isfunction() ? tick : null;
        list.add(p);
    }

    private static float num(LuaValue o, String key, float def) {
        LuaValue v = o.get(key);
        return v.isnumber() ? (float) v.todouble() : def;
    }

    public void tick() {
        for (int i = list.size() - 1; i >= 0; i--) {
            P p = list.get(i);
            p.px = p.x;
            p.py = p.y;
            p.pz = p.z;
            p.vy -= p.gravity;
            p.vx *= p.drag;
            p.vy *= p.drag;
            p.vz *= p.drag;
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            if (p.tick != null) callTick(p);
            if (++p.age >= p.life) list.remove(i);
        }
    }

    private void callTick(P p) {
        LuaTable t = new LuaTable();
        t.set("x", p.x); t.set("y", p.y); t.set("z", p.z);
        t.set("vx", p.vx); t.set("vy", p.vy); t.set("vz", p.vz);
        t.set("size", p.size); t.set("alpha", p.alpha); t.set("color", (double) (p.color & 0xFFFFFFFFL));
        t.set("age", p.age); t.set("life", p.life);
        sandbox.beginRun();
        try {
            p.tick.call(t);
            p.x = num(t, "x", p.x); p.y = num(t, "y", p.y); p.z = num(t, "z", p.z);
            p.vx = num(t, "vx", p.vx); p.vy = num(t, "vy", p.vy); p.vz = num(t, "vz", p.vz);
            p.size = num(t, "size", p.size); p.alpha = num(t, "alpha", p.alpha);
            p.color = (int) (long) t.get("color").todouble();
        } catch (LuaError e) {
            LOG.warn("HoldMyItems particle tick failed: {}", e.getMessage());
            p.tick = null;
        }
    }

    public void render(Renderer3D renderer, float partialTicks) {
        if (renderer == null || list.isEmpty()) return;
        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                TextureStorage.BLOOM, Renderer3D.DepthMode.NONE);
        if (mesh == null) return;
        for (P p : list) {
            float fade = 1.0f - (p.age + partialTicks) / p.life;
            int color = applyOpacity(p.color, p.alpha * Math.max(0f, fade));
            Vector3f right = new Vector3f(1, 0, 0).rotate(RenderState.cameraRotation).mul(p.size);
            Vector3f up = new Vector3f(0, 1, 0).rotate(RenderState.cameraRotation).mul(p.size);
            addBillboardVertices(mesh, p.px + (p.x - p.px) * partialTicks, p.py + (p.y - p.py) * partialTicks,
                    p.pz + (p.z - p.pz) * partialTicks, right, up, color, color, color, color);
        }
    }
}
